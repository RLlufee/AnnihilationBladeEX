package qwq.qingyi.annihilationblade.loli_blade.logic;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import qwq.qingyi.annihilationblade.config.ModConfig;
import qwq.qingyi.annihilationblade.loli_blade.LoliBladeDefinitions;
import qwq.qingyi.annihilationblade.loli_blade.LoliBladeProtectionManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 萝莉之刃持有者防御、伤势免疫、飞行赋予及虚空回拉逻辑。
 */
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

        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (LoliBladeDefinitions.isOwnedBy(stack, player)) {
                return true;
            }
        }

        return false;
    }

    public static boolean shouldProtect(Player player) {
        return ModConfig.COMMON.loliBlade.defenseEnabled.getValue() && hasOwnedLoliBlade(player);
    }

    public static boolean shouldCancelIncomingDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!entity.level().isClientSide() && entity instanceof Player player && shouldProtect(player)) {
            if (ModConfig.COMMON.loliBlade.reflectDamage.getValue()) {
                reflectDamage(player, source, amount);
            }
            return true;
        }
        return false;
    }

    public static boolean shouldCancelLivingHurt(LivingEntity entity) {
        return !entity.level().isClientSide() && entity instanceof Player player && shouldProtect(player);
    }

    public static boolean shouldCancelDeath(LivingEntity entity) {
        if (!entity.level().isClientSide() && entity instanceof Player player && shouldProtect(player)) {
            player.setHealth(player.getMaxHealth());
            player.invulnerableTime = 0;
            player.deathTime = 0;
            return true;
        }
        return false;
    }

    public static void onPlayerTick(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        if (!shouldProtect(player)) {
            LoliBladeProtectionManager.unmark(player.getUUID());
            revokeAbilities(player);
            return;
        }

        // 维护受保护名册，供字节码层 Mixin 与服务端守卫在高频路径上快速判定
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

        if (config.voidProtection.getValue() && player.getY() < player.level().getMinY()) {
            double safeY = player.level().getMaxY() - 2.0D;
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
        if (attacker == null
                || attacker == defender
                || (attacker instanceof Player player && hasOwnedLoliBlade(player))
                || !attacker.isAlive()
                || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }

        UUID attackerId = attacker.getUUID();
        if (!ACTIVE_REFLECTIONS.add(attackerId)) {
            return;
        }

        try {
            attacker.invulnerableTime = 0;
            attacker.hurtServer(level, level.damageSources().thorns(defender), amount);
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
        List<Holder<MobEffect>> harmful = new ArrayList<>();
        player.getActiveEffects().forEach(effect -> {
            if (!effect.getEffect().value().isBeneficial()) {
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
