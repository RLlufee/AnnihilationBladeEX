package QWQ.QingYi.annihilationbladeex.nightfall_dragon.specialeffect;

import QWQ.QingYi.annihilationbladeex.common.SpecialEffectSupport;
import QWQ.QingYi.annihilationbladeex.registry.ModSpecialEffects;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mods.flammpfeil.slashblade.RegistryEvents;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.event.SlashBladeEvent.DoSlashEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "annihilationbladeex")
public class OuterGodScar extends SpecialEffect {
   private static final int SWORD_COUNT = 5;
   private static final double RANGE = 18.0;
   private static final Map<UUID, Long> COMPANION_SWORDS = new HashMap<>();

   public OuterGodScar() {
      super(0, false, false);
   }

   @SubscribeEvent
   public static void onDoingSlash(DoSlashEvent event) {
      if (!(event.getUser() instanceof ServerPlayer player)) {
         return;
      }

      ISlashBladeState state = event.getSlashBladeState();
      if (!state.hasSpecialEffect(ModSpecialEffects.OUTER_GOD_SCAR.getId())) {
         return;
      }

      ServerLevel level = player.serverLevel();
      Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.58, 0.0);
      Vec3 direction = player.getLookAngle().normalize();
      spawnRiftScar(level, center, direction);
      List<LivingEntity> targets = SpecialEffectSupport.radialTargets(level, player, center, RANGE);
      for (int i = 0; i < SWORD_COUNT; i++) {
         LivingEntity target = targets.isEmpty() ? null : targets.get(i % targets.size());
         spawnCompanionSword(level, player, center, direction, target, i, state.getColorCode());
      }

      level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 0.45F);
   }

   private static void spawnCompanionSword(ServerLevel level, ServerPlayer player, Vec3 center, Vec3 direction, LivingEntity target, int index, int color) {
      double angle = (Math.PI * 2.0) * index / SWORD_COUNT + player.tickCount * 0.22;
      Vec3 orbit = center.add(Math.cos(angle) * 1.85, 0.25 + Math.sin(angle * 1.7) * 0.55, Math.sin(angle) * 1.85);
      Vec3 aim = target != null ? SpecialEffectSupport.centerOf(target).subtract(orbit) : direction;
      if (aim.lengthSqr() < 1.0E-6) {
         aim = direction;
      }

      EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(RegistryEvents.SummonedSword, level);
      sword.setOwner(player);
      sword.setShooter(player);
      sword.setColor(color);
      sword.setDamage(6.0);
      sword.setPierce((byte)1);
      sword.setDelay(6 + index % 4);
      sword.setRoll(index * 52.0F);
      sword.setPos(orbit.x, orbit.y, orbit.z);
      Vec3 forward = aim.normalize();
      sword.moveTo(orbit.x, orbit.y, orbit.z, yawToFace(forward), pitchToFace(forward));
      sword.shoot(forward.x, forward.y, forward.z, 1.95F, 0.0F);
      level.addFreshEntity(sword);
      COMPANION_SWORDS.put(sword.getUUID(), level.getGameTime() + 200L);
      level.sendParticles(ParticleTypes.REVERSE_PORTAL, orbit.x, orbit.y, orbit.z, 5, 0.12, 0.12, 0.12, 0.04);
   }

   public static boolean isCompanionSword(Entity entity, long gameTime) {
      purgeExpiredCompanionSwords(gameTime);
      return entity instanceof EntityAbstractSummonedSword sword
         && COMPANION_SWORDS.getOrDefault(sword.getUUID(), Long.MIN_VALUE) > gameTime;
   }

   private static void purgeExpiredCompanionSwords(long gameTime) {
      if (gameTime % 200 != 0) {
         return;
      }

      COMPANION_SWORDS.entrySet().removeIf(entry -> entry.getValue() <= gameTime);
   }

   private static void spawnRiftScar(ServerLevel level, Vec3 center, Vec3 direction) {
      Vec3 right = SpecialEffectSupport.rightOf(direction);
      Vec3 start = center.add(direction.scale(1.2)).subtract(right.scale(2.2));
      Vec3 end = center.add(direction.scale(5.8)).add(right.scale(2.2));
      for (int i = 0; i <= 28; i += 2) {
         Vec3 pos = start.lerp(end, i / 28.0);
         level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 1, 0.025, 0.025, 0.025, 0.03);
         if (i % 8 == 0) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
         }
      }
   }

   private static float yawToFace(Vec3 direction) {
      return (float)(Mth.atan2(direction.x, direction.z) * 180.0F / (float)Math.PI);
   }

   private static float pitchToFace(Vec3 direction) {
      double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
      return (float)(-Mth.atan2(direction.y, horizontal) * 180.0F / (float)Math.PI);
   }
}
