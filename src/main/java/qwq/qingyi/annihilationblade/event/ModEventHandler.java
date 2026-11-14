package qwq.qingyi.annihilationblade.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import qwq.qingyi.annihilationblade.annihilation_blade.AnnihilationBladeDefinitions;
import qwq.qingyi.annihilationblade.annihilation_blade.logic.TerminusLogic;
import qwq.qingyi.annihilationblade.annihilation_blade.logic.WorldRiftChain;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.AbyssalDecree;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.CausalityCollapse;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.Dankong;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.PhantomJudgement;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.StarlessJudgement;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.TerminusEcho;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.VoidDominion;
import qwq.qingyi.annihilationblade.annihilation_blade.visual.AnnihilationVisuals;
import qwq.qingyi.annihilationblade.common.SlashBladeTargeting;
import qwq.qingyi.annihilationblade.common.SpecialEffectSupport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 湮灭之刃核心事件逻辑处理器（服务端与公共逻辑，客户端 Tooltip 见客户端事件类）。
 */
public final class ModEventHandler {
    private static final int INVENTORY_SCAN_INTERVAL = 10;
    private static final int BLADE_REFRESH_INTERVAL = 20;
    private static final Set<UUID> PLAYERS_WITH_FLIGHT = new HashSet<>();
    private static final Map<UUID, Boolean> HAS_BLADE_CACHE = new HashMap<>();
    private static final Map<UUID, Integer> LAST_BLADE_SCAN_TICK = new HashMap<>();

    private ModEventHandler() {
    }

    private static boolean isGodBlade(ItemStack stack) {
        return SpecialEffectSupport.isAnnihilationBlade(stack);
    }

    private static boolean hasBladeInInventory(Player player) {
        if (!isGodBlade(player.getMainHandItem()) && !isGodBlade(player.getOffhandItem())) {
            UUID id = player.getUUID();
            Integer lastScan = LAST_BLADE_SCAN_TICK.get(id);
            if (lastScan != null && player.tickCount - lastScan < INVENTORY_SCAN_INTERVAL) {
                return HAS_BLADE_CACHE.getOrDefault(id, false);
            }

            for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
                if (isGodBlade(stack)) {
                    cacheBladeState(player, true);
                    return true;
                }
            }

            cacheBladeState(player, false);
            return false;
        } else {
            cacheBladeState(player, true);
            return true;
        }
    }

    private static void cacheBladeState(Player player, boolean hasBlade) {
        UUID id = player.getUUID();
        HAS_BLADE_CACHE.put(id, hasBlade);
        LAST_BLADE_SCAN_TICK.put(id, player.tickCount);
    }

    private static void refreshBladeGrowthData(Player player) {
        refreshBladeGrowthData(player.getMainHandItem(), player);
        refreshBladeGrowthData(player.getOffhandItem(), player);

        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            refreshBladeGrowthData(stack, player);
        }
    }

    private static void refreshBladeGrowthData(ItemStack stack, Player player) {
        if (isGodBlade(stack)) {
            AnnihilationBladeDefinitions.ensureStats(stack, player.level());
        }
    }

    /** 对应 1.21.1 LOWEST 优先级 LivingDamageEvent.Pre。 */
    public static float onHurt(LivingEntity target, DamageSource damageSource, float amount) {
        if (WorldRiftChain.isExecutingChain()) {
            return amount;
        }

        Entity source = damageSource.getEntity();
        Entity directSource = damageSource.getDirectEntity();
        float resultAmount = amount;
        if (source instanceof Player player) {
            boolean shouldKill = isGodBlade(player.getMainHandItem());
            if (directSource != null && directSource.getType().toString().contains("slashblade")) {
                shouldKill = shouldKill || hasBladeInInventory(player);
            }

            if (shouldKill && SlashBladeTargeting.canAttack(player, target)) {
                if (target.level() instanceof ServerLevel level) {
                    WorldRiftChain.scheduleFromDamage(level, player, target);
                }

                if (!TerminusLogic.isMarkedForDeath(target)) {
                    resultAmount = 10000.0F;
                    TerminusLogic.markForDeath(target);
                    if (target.level() instanceof ServerLevel level) {
                        AnnihilationVisuals.spawnExecutionBurst(level, target, player.getRandom());
                    }

                    if (player.distanceTo(target) < 6.0F) {
                        target.level().playSound(
                                null,
                                target.getX(),
                                target.getY(),
                                target.getZ(),
                                SoundEvents.TRIDENT_THUNDER.value(),
                                SoundSource.PLAYERS,
                                0.5F,
                                2.0F
                        );
                    }
                }
            }
        }
        return resultAmount;
    }

    /** 持有湮灭之刃误伤不可攻击目标时拦截伤害（HIGHEST）。 */
    public static boolean shouldCancelGodBladeAttack(LivingEntity target, DamageSource damageSource) {
        return damageSource.getEntity() instanceof Player player
                && isGodBlade(player.getMainHandItem())
                && !SlashBladeTargeting.canAttack(player, target);
    }

    /** 背包持有湮灭之刃时免疫一切受击伤害（HIGHEST）。 */
    public static boolean shouldCancelLivingAttack(LivingEntity target) {
        return !target.level().isClientSide() && target instanceof Player player && hasBladeInInventory(player);
    }

    /** 背包持有湮灭之刃时拦截死亡事件并回满生命（HIGHEST）。 */
    public static boolean shouldCancelLivingDeath(LivingEntity target) {
        if (!target.level().isClientSide() && target instanceof Player player && hasBladeInInventory(player)) {
            player.setHealth(player.getMaxHealth());
            return true;
        }
        return false;
    }

    public static void onPlayerTick(Player player) {
        if (!player.level().isClientSide()) {
            UUID key = player.getUUID();
            boolean hasBlade = hasBladeInInventory(player);
            if (hasBlade && player.tickCount % BLADE_REFRESH_INTERVAL == 0) {
                refreshBladeGrowthData(player);
            }

            if (hasBlade) {
                if (player.getHealth() < player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }

                player.getFoodData().setFoodLevel(20);
                player.getFoodData().setSaturation(20.0F);
                if (!player.isCreative() && !player.isSpectator() && !player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    PLAYERS_WITH_FLIGHT.add(key);
                    player.onUpdateAbilities();
                }

                if (player.getY() < -64.0) {
                    player.teleportTo(player.getX(), 320.0, player.getZ());
                    player.setDeltaMovement(0.0, 0.0, 0.0);
                    if (!player.getAbilities().flying) {
                        player.getAbilities().flying = true;
                        player.onUpdateAbilities();
                    }
                }
            } else if (!player.isCreative() && !player.isSpectator() && player.getAbilities().mayfly && PLAYERS_WITH_FLIGHT.contains(key)) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                PLAYERS_WITH_FLIGHT.remove(key);
                player.onUpdateAbilities();
            }
        }
    }

    public static void onPlayerLogout(Player player) {
        clearPlayerState(player);
        Dankong.clearBlinkMode(player.getUUID());
    }

    public static void onPlayerChangedDimension(Player player) {
        clearPlayerState(player);
    }

    private static void clearPlayerState(Player player) {
        UUID id = player.getUUID();
        PLAYERS_WITH_FLIGHT.remove(id);
        HAS_BLADE_CACHE.remove(id);
        LAST_BLADE_SCAN_TICK.remove(id);
        Dankong.clearPlayer(id);
        TerminusEcho.clearPlayer(id);
        AbyssalDecree.clearPlayer(id);
        PhantomJudgement.clearPlayer(player.level(), id);
        CausalityCollapse.clearPlayer(id);
        StarlessJudgement.clearPlayer(id);
        VoidDominion.clearPlayer(id);
    }
}
