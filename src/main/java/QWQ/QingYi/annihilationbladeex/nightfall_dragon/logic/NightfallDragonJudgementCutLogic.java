package QWQ.QingYi.annihilationbladeex.nightfall_dragon.logic;

import QWQ.QingYi.annihilationbladeex.common.ServerTickScheduler;
import QWQ.QingYi.annihilationbladeex.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationbladeex.common.SpecialEffectSupport;
import QWQ.QingYi.annihilationbladeex.config.ModConfig;
import QWQ.QingYi.annihilationbladeex.infinity_stellaris.logic.GammaThunderburstLogic;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.NightfallDragonDefinitions;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.item.NightfallDragonItemSupport;
import java.util.ArrayList;
import java.util.List;
import mods.flammpfeil.slashblade.RegistryEvents;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.entity.EntityJudgementCut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class NightfallDragonJudgementCutLogic {
   private static final int RANDOM_POSITION_ATTEMPTS = 16;
   private static final int TOTAL_RANDOM_POSITION_ATTEMPTS = 320;

   private static double getRadius() {
      return ModConfig.COMMON.nightfallDragon.judgementCutRange.getValue();
   }

   private static int getTotalCuts() {
      return ModConfig.COMMON.nightfallDragon.judgementCutTotalCuts.getValue();
   }

   private static int getIntervalTicks() {
      return ModConfig.COMMON.nightfallDragon.judgementCutIntervalTicks.getValue();
   }

   private static double getDamage() {
      return ModConfig.COMMON.nightfallDragon.judgementCutDamage.getValue();
   }

   private static float getScale() {
      return ModConfig.COMMON.nightfallDragon.judgementCutScale.getValue().floatValue();
   }

   private NightfallDragonJudgementCutLogic() {
   }

   public static void prepareCast(Player player) {
      if (player instanceof ServerPlayer serverPlayer && canUseSealedArt(serverPlayer)) {
         ServerLevel level = serverPlayer.serverLevel();
         Vec3 center = serverPlayer.position().add(0.0, serverPlayer.getBbHeight() * 0.5, 0.0);
         level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 18, 1.2, 0.45, 1.2, 0.08);
         level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 0.75F);
      }
   }

   public static void unleash(Player player) {
      if (!(player instanceof ServerPlayer serverPlayer) || !canUseSealedArt(serverPlayer)) {
         return;
      }

      ServerLevel level = serverPlayer.serverLevel();
      List<CutAnchor> anchors = collectAnchors(level, serverPlayer);
      Vec3 center = serverPlayer.position().add(0.0, serverPlayer.getBbHeight() * 0.5, 0.0);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, 1.3F, 0.55F);

      for (int i = 0; i < anchors.size(); i++) {
         int index = i;
         ServerTickScheduler.schedule(index * getIntervalTicks(), () -> spawnScheduledCut(serverPlayer, anchors.get(index), index));
      }
   }

   private static boolean canUseSealedArt(ServerPlayer player) {
      ItemStack stack = player.getMainHandItem();
      return NightfallDragonItemSupport.isNightfallDragon(stack) && NightfallDragonDefinitions.FORM_SEALED.equals(NightfallDragonDefinitions.getForm(stack));
   }

   private static List<CutAnchor> collectAnchors(ServerLevel level, ServerPlayer player) {
      int totalCuts = getTotalCuts();
      double radius = getRadius();
      List<CutAnchor> anchors = new ArrayList<>(totalCuts);
      Vec3 center = player.position();

      List<LivingEntity> targets = SpecialEffectSupport.radialTargets(level, player, center, radius);
      for (LivingEntity target : targets) {
         anchors.add(CutAnchor.target(target.getId(), target.position()));
         if (anchors.size() >= totalCuts) {
            return anchors;
         }
      }

      RandomSource random = player.getRandom();
      int attempts = 0;
      while (anchors.size() < totalCuts && attempts++ < TOTAL_RANDOM_POSITION_ATTEMPTS) {
         Vec3 position = randomPosition(level, player, random);
         if (position != null) {
            anchors.add(CutAnchor.fixed(position));
         }
      }

      Vec3 fallback = findNearbyFloor(level, player.getX(), player.getY(), player.getZ());
      while (anchors.size() < totalCuts && fallback != null) {
         anchors.add(CutAnchor.fixed(fallback));
      }

      return anchors;
   }

   private static Vec3 randomPosition(ServerLevel level, ServerPlayer player, RandomSource random) {
      double radius = getRadius();
      for (int attempt = 0; attempt < RANDOM_POSITION_ATTEMPTS; attempt++) {
         double distance = radius * Math.sqrt(random.nextDouble());
         double angle = random.nextDouble() * Math.PI * 2.0;
         double x = player.getX() + Math.cos(angle) * distance;
         double z = player.getZ() + Math.sin(angle) * distance;
         Vec3 position = findNearbyFloor(level, x, player.getY(), z);
         if (position != null) {
            return position;
         }
      }

      return null;
   }

   private static Vec3 findNearbyFloor(ServerLevel level, double x, double baseY, double z) {
      BlockPos origin = BlockPos.containing(x, baseY, z);
      for (int dy = 0; dy >= -10; dy--) {
         BlockPos floor = origin.offset(0, dy, 0);
         if (hasClearCutSpace(level, floor)) {
            return new Vec3(x, floor.getY() + 1.0, z);
         }
      }

      for (int dy = 1; dy <= 8; dy++) {
         BlockPos floor = origin.offset(0, dy, 0);
         if (hasClearCutSpace(level, floor)) {
            return new Vec3(x, floor.getY() + 1.0, z);
         }
      }

      return null;
   }

   private static boolean hasClearCutSpace(ServerLevel level, BlockPos floor) {
      BlockState floorState = level.getBlockState(floor);
      if (!floorState.getFluidState().isEmpty() || !floorState.isFaceSturdy(level, floor, Direction.UP)) {
         return false;
      }

      for (int height = 1; height <= 3; height++) {
         BlockPos space = floor.above(height);
         BlockState state = level.getBlockState(space);
         if (!state.getFluidState().isEmpty() || !state.getCollisionShape(level, space).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private static void spawnScheduledCut(ServerPlayer player, CutAnchor anchor, int index) {
      if (!player.isAlive() || !canUseSealedArt(player)) {
         return;
      }

      ServerLevel level = player.serverLevel();
      Vec3 position = resolveAnchor(level, player, anchor);
      if (position == null) {
         return;
      }

      spawnJudgementCut(level, player, position, index);
   }

   private static Vec3 resolveAnchor(ServerLevel level, ServerPlayer player, CutAnchor anchor) {
      if (anchor.targetId >= 0 && level.getEntity(anchor.targetId) instanceof LivingEntity target && SlashBladeTargeting.canAttack(player, target)) {
         return target.position();
      }

      return anchor.position;
   }

   private static void spawnJudgementCut(ServerLevel level, ServerPlayer player, Vec3 position, int index) {
      EntityJudgementCut cut = new EntityJudgementCut(RegistryEvents.JudgementCut, level);
      cut.setOwner(player);
      cut.setShooter(player);
      cut.setColor(NightfallDragonDefinitions.SEALED_SUMMONED_SWORD_COLOR);
      cut.setDamage(getDamage());
      cut.setLifetime(20);
      cut.setRank(getScale());
      cut.setIsCritical(index % 3 == 0);
      cut.setNoGravity(true);
      cut.setPos(position.x, position.y + 0.15, position.z);

      BladeStateAccess.of(player.getMainHandItem()).ifPresent(state -> cut.setColor(state.getColorCode()));

      level.addFreshEntity(cut);

      GammaThunderburstLogic.spawnBolt(level, position, 0xB026FF);

      double pScale = Math.max(0.5, getScale());
      int pCount1 = (int) Math.round(5 * pScale);
      int pCount2 = (int) Math.round(8 * pScale);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, position.x, position.y + 0.2, position.z, pCount1, 0.45 * pScale, 0.08 * pScale, 0.45 * pScale, 0.05);
      level.sendParticles(ParticleTypes.ENCHANT, position.x, position.y + 0.25, position.z, pCount2, 0.55 * pScale, 0.1 * pScale, 0.55 * pScale, 0.04);
      level.playSound(null, position.x, position.y, position.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.45F, 0.85F + index % 4 * 0.08F);
   }

   private record CutAnchor(int targetId, Vec3 position) {
      private static CutAnchor target(int targetId, Vec3 position) {
         return new CutAnchor(targetId, position);
      }

      private static CutAnchor fixed(Vec3 position) {
         return new CutAnchor(-1, position);
      }
   }
}
