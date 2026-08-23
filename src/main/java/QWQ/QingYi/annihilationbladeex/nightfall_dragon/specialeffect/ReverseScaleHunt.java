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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "annihilationbladeex")
public class ReverseScaleHunt extends SpecialEffect {
   private static final int SWORD_COUNT = 12;
   private static final double RANGE = 32.0;
   private static final int GOLD = 0xF4D66A;
   private static final int PURPLE_FLAME = 0xDA43FF;
   private static final Map<UUID, Long> REVERSE_SCALE_SWORDS = new HashMap<>();

   public ReverseScaleHunt() {
      super(0, false, false);
   }

   @SubscribeEvent
   public static void onDoingSlash(DoSlashEvent event) {
      if (!(event.getUser() instanceof ServerPlayer player)) {
         return;
      }

      ISlashBladeState state = event.getSlashBladeState();
      if (!state.hasSpecialEffect(ModSpecialEffects.REVERSE_SCALE_HUNT.getId())) {
         return;
      }

      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2, false, true, true));
      ServerLevel level = player.serverLevel();
      Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.66, 0.0);
      Vec3 direction = player.getLookAngle().normalize();
      List<LivingEntity> targets = SpecialEffectSupport.radialTargets(level, player, center, RANGE);
      for (int i = 0; i < SWORD_COUNT; i++) {
         LivingEntity target = targets.isEmpty() ? null : targets.get(i % targets.size());
         spawnReverseScale(level, player, center, direction, target, i);
      }

      level.playSound(null, center.x, center.y, center.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.85F, 1.35F);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 0.75F);
   }

   private static void spawnReverseScale(ServerLevel level, ServerPlayer player, Vec3 center, Vec3 direction, LivingEntity target, int index) {
      double angle = (Math.PI * 2.0) * index / SWORD_COUNT + player.tickCount * 0.36;
      double radius = 2.4 + index % 3 * 0.32;
      Vec3 orbit = center.add(Math.cos(angle) * radius, 0.45 + Math.sin(angle * 2.0) * 0.75, Math.sin(angle) * radius);
      Vec3 aim = target != null ? SpecialEffectSupport.centerOf(target).subtract(orbit) : direction;
      if (aim.lengthSqr() < 1.0E-6) {
         aim = direction;
      }

      Vec3 forward = aim.normalize();
      EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(RegistryEvents.SummonedSword, level);
      sword.setOwner(player);
      sword.setShooter(player);
      sword.setColor(index % 2 == 0 ? GOLD : PURPLE_FLAME);
      sword.setDamage(14.0);
      sword.setPierce((byte)4);
      sword.setDelay(3 + index % 3);
      sword.setRoll(index * 31.0F);
      sword.setPos(orbit.x, orbit.y, orbit.z);
      sword.moveTo(orbit.x, orbit.y, orbit.z, yawToFace(forward), pitchToFace(forward));
      sword.shoot(forward.x, forward.y, forward.z, 3.15F, 0.0F);
      level.addFreshEntity(sword);
      REVERSE_SCALE_SWORDS.put(sword.getUUID(), level.getGameTime() + 200L);
      level.sendParticles(index % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.DRAGON_BREATH, orbit.x, orbit.y, orbit.z, 5, 0.15, 0.15, 0.15, 0.03);
   }

   public static boolean isReverseScaleSword(Entity entity, long gameTime) {
      purgeExpiredReverseScaleSwords(gameTime);
      return entity instanceof EntityAbstractSummonedSword sword
         && REVERSE_SCALE_SWORDS.getOrDefault(sword.getUUID(), Long.MIN_VALUE) > gameTime;
   }

   private static void purgeExpiredReverseScaleSwords(long gameTime) {
      if (gameTime % 200 != 0) {
         return;
      }

      REVERSE_SCALE_SWORDS.entrySet().removeIf(entry -> entry.getValue() <= gameTime);
   }

   private static float yawToFace(Vec3 direction) {
      return (float)(Mth.atan2(direction.x, direction.z) * 180.0F / (float)Math.PI);
   }

   private static float pitchToFace(Vec3 direction) {
      double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
      return (float)(-Mth.atan2(direction.y, horizontal) * 180.0F / (float)Math.PI);
   }
}
