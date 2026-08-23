package QWQ.QingYi.annihilationbladeex.nightfall_dragon.logic;

import QWQ.QingYi.annihilationbladeex.config.ModConfig;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.NightfallDragonDefinitions;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.entity.ScaleGuardSwordEntity;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.item.NightfallDragonItemSupport;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class ScaleGuardLogic {
   public static int getSwordCount() {
      return ModConfig.COMMON.nightfallDragon.scaleGuardSwordCount.getValue();
   }

   private ScaleGuardLogic() {
   }

   public static void prepareCast(Player player) {
      if (player instanceof ServerPlayer serverPlayer && canUseAwakenedArt(serverPlayer)) {
         ServerLevel level = serverPlayer.serverLevel();
         Vec3 center = serverPlayer.position().add(0.0D, 1.1D, 0.0D);
         level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 0.55F);
         level.playSound(null, center.x, center.y, center.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.9F, 0.7F);
         level.sendParticles(ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, 32, 1.6D, 0.45D, 1.6D, 0.02D);
      }
   }

   public static void unleash(Player player) {
      if (!(player instanceof ServerPlayer serverPlayer) || !canUseAwakenedArt(serverPlayer)) {
         return;
      }

      ServerLevel level = serverPlayer.serverLevel();
      Vec3 center = serverPlayer.position().add(0.0D, 1.1D, 0.0D);
      int swordCount = getSwordCount();
      for (int i = 0; i < swordCount; i++) {
         ScaleGuardSwordEntity sword = ScaleGuardSwordEntity.create(level, serverPlayer, i, swordCount);
         if (sword != null) {
            level.addFreshEntity(sword);
         }
      }

      level.playSound(null, center.x, center.y, center.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.8F, 0.55F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.3F, 0.65F);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, 48, 1.9D, 0.55D, 1.9D, 0.035D);
   }

   private static boolean canUseAwakenedArt(ServerPlayer player) {
      ItemStack stack = player.getMainHandItem();
      return NightfallDragonItemSupport.isNightfallDragon(stack) && NightfallDragonDefinitions.FORM_AWAKENED.equals(NightfallDragonDefinitions.getForm(stack));
   }
}
