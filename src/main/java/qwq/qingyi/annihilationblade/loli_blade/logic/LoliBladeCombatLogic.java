package qwq.qingyi.annihilationblade.loli_blade.logic;

import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import qwq.qingyi.annihilationblade.common.SlashBladeStateSupport;
import qwq.qingyi.annihilationblade.common.SlashBladeTargeting;
import qwq.qingyi.annihilationblade.common.SpecialEffectSupport;
import qwq.qingyi.annihilationblade.config.ModConfig;
import qwq.qingyi.annihilationblade.loli_blade.LoliBladeDefinitions;
import qwq.qingyi.annihilationblade.loli_blade.LoliBladeEntityAccess;
import qwq.qingyi.annihilationblade.loli_blade.LoliBladeLivingEntityAccess;
import qwq.qingyi.annihilationblade.registry.ModSounds;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 萝莉之刃服务端目标结算逻辑：只处理世界内的实体状态与清除流程。
 */
public final class LoliBladeCombatLogic {
    private static final float EXECUTION_DAMAGE = 1.0E9F;
    private static final double CONE_HALF_ANGLE_COSINE = 0.8660254037844386D;
    private static final Set<UUID> ACTIVE_EXECUTIONS = new HashSet<>();
    private static final Set<UUID> ACTIVE_ACTIONS = new HashSet<>();
    private static final Map<UUID, Long> FACING_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> AREA_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> LAST_SOUND_TICK = new HashMap<>();

    private LoliBladeCombatLogic() {
    }

    /** 准备施法：确保手持萝莉之刃属性与附魔就绪。 */
    public static void prepareCast(Player player) {
        ItemStack blade = player.getMainHandItem();
        if (!blade.isEmpty()) {
            LoliBladeDefinitions.ensureStats(blade, player.level());
        }
    }

    public static void clearPlayerState(UUID id) {
        FACING_COOLDOWNS.remove(id);
        AREA_COOLDOWNS.remove(id);
        LAST_SOUND_TICK.remove(id);
        ACTIVE_ACTIONS.remove(id);
    }

    /** 拦截直接普攻伤害前置：由持有者挥击触发高额结算并取消原版流程。 */
    public static boolean handleLivingAttack(LivingEntity entity, DamageSource source) {
        if (entity.level().isClientSide()
                || "thorns".equals(source.getMsgId())
                || !(source.getEntity() instanceof Player attacker)
                || source.getDirectEntity() != attacker
                || !LoliBladeDefinitions.isLoliBlade(attacker.getMainHandItem())) {
            return false;
        }

        if (!isActiveAttacker(attacker)) {
            return true;
        }

        if (!SlashBladeTargeting.canAttack(attacker, entity) || isProtectedOwner(entity)) {
            return false;
        }

        UUID targetId = entity.getUUID();
        if (ACTIVE_EXECUTIONS.add(targetId)) {
            try {
                execute(entity, attacker);
            } finally {
                ACTIVE_EXECUTIONS.remove(targetId);
            }
        }
        return true;
    }

    public static InteractionResult handleEntityAttack(Player attacker, Entity target) {
        if (!LoliBladeDefinitions.isLoliBlade(attacker.getMainHandItem())) {
            return InteractionResult.PASS;
        }

        if (attacker.level().isClientSide() || !isActiveAttacker(attacker)) {
            return InteractionResult.FAIL;
        }

        if (target instanceof LivingEntity living) {
            if (!isProtectedOwner(living)) {
                execute(living, attacker);
            }
        } else if (ModConfig.COMMON.loliBlade.removeEntity.getValue()) {
            removeEntity(target);
        }

        return InteractionResult.FAIL;
    }

    /** 监听伤害落地阶段：主手持有萝莉之刃且对合法目标造成有效伤害时播放命中音效。 */
    public static void handleLivingDamage(LivingEntity target, DamageSource source, float damageTaken) {
        if (target.level().isClientSide() || damageTaken <= 0.0F) {
            return;
        }

        Entity directSource = source.getDirectEntity();
        Entity trueSource = source.getEntity();
        Player attacker = null;
        if (trueSource instanceof Player player) {
            attacker = player;
        } else if (directSource instanceof Player player) {
            attacker = player;
        }

        if (attacker == null || !isLoliBladeAttack(source, attacker)) {
            return;
        }

        if (target == attacker || !SlashBladeTargeting.canAttack(attacker, target) || isProtectedOwner(target)) {
            return;
        }

        playLoliAttackSound(attacker, target);
    }

    private static boolean isLoliBladeAttack(DamageSource source, Player attacker) {
        if (!LoliBladeDefinitions.isLoliBlade(attacker.getMainHandItem())) {
            return false;
        }
        Entity direct = source.getDirectEntity();
        if (direct == attacker || direct == null) {
            return true;
        }
        if (direct instanceof AbstractArrow || direct instanceof ThrowableItemProjectile) {
            return false;
        }
        return direct.getType().toString().contains("slashblade")
                || direct instanceof EntityAbstractSummonedSword;
    }

    public static void handleAction(Player attacker, Action action) {
        if (attacker.level().isClientSide() || !isActiveAttacker(attacker) || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }

        UUID id = attacker.getUUID();
        if (!ACTIVE_ACTIONS.add(id)) {
            return;
        }

        try {
            ModConfig.LoliBlade config = ModConfig.COMMON.loliBlade;
            Map<UUID, Long> cooldowns = action == Action.FACING ? FACING_COOLDOWNS : AREA_COOLDOWNS;
            int cooldown = action == Action.FACING ? config.facingCooldownTicks.getValue() : config.areaCooldownTicks.getValue();
            if (!SpecialEffectSupport.tryStartCooldown(cooldowns, attacker, level.getGameTime(), cooldown)) {
                return;
            }

            double range = action == Action.FACING ? config.attackRange.getValue() : config.slashArtRange.getValue();
            List<Entity> targets = action == Action.FACING
                    ? facingTargets(level, attacker, range, config.maxTargets.getValue())
                    : areaTargets(level, attacker, range, config.maxTargets.getValue());
            for (Entity target : targets) {
                executeEntity(target, attacker);
            }
        } finally {
            ACTIVE_ACTIONS.remove(id);
        }
    }

    public static void handleFacingSpecialEffect(Player attacker) {
        handleAction(attacker, Action.FACING);
    }

    public static void handleAreaSlashArt(Player attacker) {
        handleAction(attacker, Action.AREA);
    }

    public static void execute(LivingEntity target, Player attacker) {
        if (!(target.level() instanceof ServerLevel level)
                || !SlashBladeTargeting.canAttack(attacker, target)
                || !isActiveAttacker(attacker)
                || isProtectedOwner(target)) {
            return;
        }

        ModConfig.LoliBlade config = ModConfig.COMMON.loliBlade;
        if (target instanceof Player player && config.clearInventory.getValue()) {
            clearPlayerInventory(player);
        }

        target.invulnerableTime = 0;
        target.setAbsorptionAmount(0.0F);
        DamageSource source = level.damageSources().playerAttack(attacker);
        ItemStack blade = attacker.getMainHandItem();
        int killCountBefore = getKillCount(blade);

        // 第一击走原版伤害链，保留战利品、经验、统计和死亡事件
        target.hurtServer(level, source, EXECUTION_DAMAGE);
        playLoliAttackSound(attacker, target);

        if (config.ultimateObliterate.getValue()) {
            // 不依赖原版伤害链结果，使用字节码同步归零与更新抑制完成清除
            obliterate(target, source);
        } else {
            if (target.isAlive() && config.forceDeath.getValue()) {
                target.invulnerableTime = 0;
                target.setHealth(0.0F);
                target.die(source);
            }
            if (target.isAlive() && config.removeEntity.getValue()) {
                target.remove(Entity.RemovalReason.KILLED);
            }
        }

        boolean defeated = !target.isAlive() || target.isRemoved();
        if (defeated) {
            ensureKillCountIncremented(blade, killCountBefore);
        }

        if (target instanceof ServerPlayer player && config.kickPlayer.getValue()) {
            player.connection.disconnect(Component.translatable("message.annihilationbladeex.loli_blade.kick"));
        }
    }

    private static void clearPlayerInventory(Player player) {
        player.getInventory().clearContent();
        player.getEnderChestInventory().clearContent();
    }

    private static void removeEntity(Entity target) {
        if (!target.isRemoved()) {
            target.remove(Entity.RemovalReason.KILLED);
        }
    }

    /**
     * 物理清除路径：绕过常规防御帧与拦截标记，确保目标进入死亡并停止后续 tick 更新。
     */
    private static void obliterate(LivingEntity target, DamageSource source) {
        if (!target.isAlive() && target.getHealth() <= 0.0F) {
            return;
        }

        target.invulnerableTime = 0;
        target.setAbsorptionAmount(0.0F);
        ((LoliBladeLivingEntityAccess) target).annihilationblade$forceZeroHealth();
        target.setHealth(0.0F);
        target.die(source);
        target.setHealth(-1.0F);
        pokeHealthField(target);
        if (!(target instanceof Player)) {
            ((LoliBladeEntityAccess) target).annihilationblade$setUpdateSuppressed(true);
            if (!target.isRemoved()) {
                target.remove(Entity.RemovalReason.KILLED);
            }
        }
    }

    /** 兜底写入私有 health 字段，防止覆写 setHealth 的目标绕过。 */
    private static void pokeHealthField(LivingEntity entity) {
        try {
            Field field = LivingEntity.class.getDeclaredField("health");
            field.setAccessible(true);
            field.set(entity, 0.0F);
        } catch (Throwable ignored) {
        }
    }

    private static boolean isActiveAttacker(Player player) {
        return ModConfig.COMMON.loliBlade.attackEnabled.getValue()
                && LoliBladeDefinitions.isOwnedBy(player.getMainHandItem(), player);
    }

    private static boolean isProtectedOwner(LivingEntity target) {
        return target instanceof Player player && LoliBladeDefenseLogic.hasOwnedLoliBlade(player);
    }

    private static List<Entity> areaTargets(ServerLevel level, Player attacker, double range, int maxTargets) {
        Vec3 center = attacker.position();
        AABB area = new AABB(center, center).inflate(range);
        List<Entity> targets = level.getEntitiesOfClass(
                Entity.class,
                area,
                target -> isAreaTarget(attacker, target)
                        && target.getBoundingBox().getCenter().distanceToSqr(center) <= range * range
        );
        targets.sort(Comparator.comparingDouble(target -> target.position().distanceToSqr(center)));
        return limit(targets, maxTargets);
    }

    private static List<Entity> facingTargets(ServerLevel level, Player attacker, double range, int maxTargets) {
        Vec3 start = attacker.getEyePosition();
        Vec3 direction = attacker.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(range));
        AABB area = new AABB(start, end).inflate(2.5D, 2.5D, 2.5D);
        List<Entity> targets = level.getEntitiesOfClass(
                Entity.class,
                area,
                target -> {
                    if (!isAreaTarget(attacker, target)) {
                        return false;
                    }

                    Vec3 delta = target.getBoundingBox().getCenter().subtract(start);
                    double projection = delta.dot(direction);
                    return projection >= 0.0D && projection <= range
                            && (projection <= 1.0D || delta.normalize().dot(direction) >= CONE_HALF_ANGLE_COSINE);
                }
        );
        targets.sort(Comparator.comparingDouble(target -> target.position().subtract(attacker.position()).lengthSqr()));
        return limit(targets, maxTargets);
    }

    private static boolean isAreaTarget(Player attacker, Entity target) {
        if (target == attacker || !target.isAlive()) {
            return false;
        }

        if (target instanceof LivingEntity living) {
            return SlashBladeTargeting.canAttack(attacker, living) && !isProtectedOwner(living);
        }

        return ModConfig.COMMON.loliBlade.removeEntity.getValue();
    }

    private static List<Entity> limit(List<Entity> targets, int maxTargets) {
        int limit = Math.max(1, maxTargets);
        return targets.size() <= limit ? targets : new ArrayList<>(targets.subList(0, limit));
    }

    private static void executeEntity(Entity target, Player attacker) {
        if (target instanceof LivingEntity living) {
            execute(living, attacker);
        } else if (ModConfig.COMMON.loliBlade.removeEntity.getValue()) {
            removeEntity(target);
        }
    }

    public static void playLoliAttackSound(Player attacker, LivingEntity target) {
        if (attacker.level() instanceof ServerLevel level) {
            long gameTime = level.getGameTime();
            Long lastTick = LAST_SOUND_TICK.get(attacker.getUUID());
            if (lastTick == null || lastTick != gameTime) {
                LAST_SOUND_TICK.put(attacker.getUUID(), gameTime);
                level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), ModSounds.LOLI_SUCCESS, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    public enum Action {
        FACING,
        AREA
    }

    private static int getKillCount(ItemStack blade) {
        return SlashBladeStateSupport.isSlashBlade(blade) ? SlashBladeStateSupport.killCount(blade) : -1;
    }

    private static void ensureKillCountIncremented(ItemStack blade, int killCountBefore) {
        if (killCountBefore >= 0) {
            SlashBladeStateSupport.state(blade).ifPresent(state -> {
                if (state.getKillCount() == killCountBefore) {
                    state.setKillCount(killCountBefore + 1);
                }
            });
        }
    }
}
