package QWQ.QingYi.annihilationblade.loli_blade.logic;

import QWQ.QingYi.annihilationblade.config.ModConfig;
import QWQ.QingYi.annihilationblade.loli_blade.LoliBladeDefinitions;
import QWQ.QingYi.annihilationblade.loli_blade.LoliBladeProtectionManager;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** 萝莉刀持有者的服务器防御、飞行和虚空保护。 */
public final class LoliBladeDefenseLogic {
   private static final Set<UUID> GRANTED_FLIGHT = new HashSet<>();
   private static final Set<UUID> GRANTED_INVULNERABILITY = new HashSet<>();
   private static final Set<UUID> ACTIVE_REFLECTIONS = new HashSet<>();

   private LoliBladeDefenseLogic() {
   }

   public static boolean hasOwnedLoliBlade(Player player) {
      if (player == null) {
         return false;
      }

      if (LoliBladeDefinitions.isOwnedBy(player.getMainHandItem(), player)
         || LoliBladeDefinitions.isOwnedBy(player.getOffhandItem(), player)) {
         return true;
      }

      for (var stack : player.getInventory().items) {
         if (LoliBladeDefinitions.isOwnedBy(stack, player)) {
            return true;
         }
      }

      return false;
   }

   public static boolean shouldProtect(Player player) {
      return ModConfig.COMMON.loliBlade.defenseEnabled.getValue() && hasOwnedLoliBlade(player);
   }

   public static void onLivingAttack(LivingAttackEvent event) {
      if (!event.getEntity().level().isClientSide
         && event.getEntity() instanceof Player player
         && shouldProtect(player)) {
         event.setCanceled(true);
         if (ModConfig.COMMON.loliBlade.reflectDamage.getValue()) {
            reflectDamage(player, event.getSource(), event.getAmount());
         }
      }
   }

   public static void onLivingHurt(LivingHurtEvent event) {
      if (!event.getEntity().level().isClientSide
         && event.getEntity() instanceof Player player
         && shouldProtect(player)) {
         event.setCanceled(true);
      }
   }

   public static void onLivingDeath(LivingDeathEvent event) {
      if (!event.getEntity().level().isClientSide
         && event.getEntity() instanceof Player player
         && shouldProtect(player)) {
         event.setCanceled(true);
      player.setHealth(player.getMaxHealth());
      player.invulnerableTime = 0;
      player.deathTime = 0;
      }
   }

   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase != Phase.END || event.player.level().isClientSide) {
         return;
      }

      Player player = event.player;
      if (!shouldProtect(player)) {
         LoliBladeProtectionManager.unmark(player.getUUID());
         revokeAbilities(player);
         return;
      }

      // 维护受保护名册，供方法级 Mixin 与忒修斯守卫在高频路径上快速判定。
      LoliBladeProtectionManager.markProtected(player.getUUID());

      ModConfig.LoliBlade config = ModConfig.COMMON.loliBlade;
      if (config.restoreHealth.getValue() && player.getHealth() < player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }

      player.getFoodData().setFoodLevel(20);
      player.getFoodData().setSaturation(20.0F);
      player.clearFire();
      player.resetFallDistance();
      removeHarmfulEffects(player);

      if (!player.isCreative() && !player.isSpectator()) {
         if (config.grantFlight.getValue() && !player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            GRANTED_FLIGHT.add(player.getUUID());
            player.onUpdateAbilities();
         }

         if (!player.getAbilities().invulnerable) {
            player.getAbilities().invulnerable = true;
            GRANTED_INVULNERABILITY.add(player.getUUID());
            player.onUpdateAbilities();
         }
      }

      if (config.voidProtection.getValue() && player.getY() < player.level().getMinBuildHeight()) {
         double safeY = player.level().getMaxBuildHeight() - 2.0D;
         player.teleportTo(player.getX(), safeY, player.getZ());
         player.setDeltaMovement(0.0D, 0.0D, 0.0D);
         player.resetFallDistance();
      }
   }

   public static void clearPlayerState(Player player) {
      UUID id = player.getUUID();
      LoliBladeProtectionManager.unmark(id);
      if (!player.isCreative() && !player.isSpectator()) {
         if (GRANTED_FLIGHT.remove(id)) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
         }
         if (GRANTED_INVULNERABILITY.remove(id)) {
            player.getAbilities().invulnerable = false;
         }
         player.onUpdateAbilities();
      } else {
         GRANTED_FLIGHT.remove(id);
         GRANTED_INVULNERABILITY.remove(id);
      }
   }

   private static void reflectDamage(Player defender, DamageSource source, float amount) {
      if (amount <= 0.0F) {
         return;
      }

      LivingEntity attacker = findAttacker(source);
      if (attacker == null || attacker == defender
         || (attacker instanceof Player player && hasOwnedLoliBlade(player))
         || !attacker.isAlive()) {
         return;
      }

      UUID attackerId = attacker.getUUID();
      if (!ACTIVE_REFLECTIONS.add(attackerId)) {
         return;
      }

      try {
         attacker.invulnerableTime = 0;
         attacker.hurt(defender.level().damageSources().thorns(defender), amount);
      } finally {
         ACTIVE_REFLECTIONS.remove(attackerId);
      }
   }

   private static LivingEntity findAttacker(DamageSource source) {
      Entity sourceEntity = source.getEntity();
      if (sourceEntity instanceof LivingEntity living) {
         return living;
      }

      Entity directEntity = source.getDirectEntity();
      if (directEntity instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity living) {
         return living;
      }

      return null;
   }

   private static void removeHarmfulEffects(Player player) {
      List<MobEffect> harmful = new ArrayList<>();
      player.getActiveEffects().forEach(effect -> {
         if (!effect.getEffect().isBeneficial()) {
            harmful.add(effect.getEffect());
         }
      });
      harmful.forEach(player::removeEffect);
   }

   private static void revokeAbilities(Player player) {
      if (!player.isCreative() && !player.isSpectator()) {
         boolean changed = false;
         if (GRANTED_FLIGHT.remove(player.getUUID())) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            changed = true;
         }
         if (GRANTED_INVULNERABILITY.remove(player.getUUID())) {
            player.getAbilities().invulnerable = false;
            changed = true;
         }
         if (changed) {
            player.onUpdateAbilities();
         }
      }
   }
}
