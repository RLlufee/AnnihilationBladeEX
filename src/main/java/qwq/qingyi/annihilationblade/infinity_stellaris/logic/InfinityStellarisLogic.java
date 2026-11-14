package qwq.qingyi.annihilationblade.infinity_stellaris.logic;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import qwq.qingyi.annihilationblade.infinity_stellaris.InfinityStellarisDefinitions;
import qwq.qingyi.annihilationblade.infinity_stellaris.item.InfinityStellarisItemSupport;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 无尽星空持有态防御、常驻星尘粒子、飞行与虚空保护逻辑。
 */
public final class InfinityStellarisLogic {
    private static final int BLADE_REFRESH_INTERVAL = 20;
    private static final ParticleOptions DRAGON_BREATH_PARTICLE = PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F);
    private static final Set<UUID> PLAYERS_WITH_FLIGHT = new HashSet<>();

    private InfinityStellarisLogic() {
    }

    /** 对应 HIGHEST 优先级 LivingIncomingDamageEvent（免伤 + 恢复持刀者状态）。 */
    public static boolean shouldCancelIncomingDamage(LivingEntity target) {
        if (!target.level().isClientSide() && target instanceof Player player && InfinityStellarisItemSupport.hasInfinityStellarisInInventory(player)) {
            restoreFinalWeaponBearer(player);
            return true;
        }
        return false;
    }

    /** 对应 HIGHEST 优先级 LivingDeathEvent。 */
    public static boolean shouldCancelDeath(LivingEntity target) {
        if (!target.level().isClientSide() && target instanceof Player player && InfinityStellarisItemSupport.hasInfinityStellarisInInventory(player)) {
            restoreFinalWeaponBearer(player);
            return true;
        }
        return false;
    }

    public static void onPlayerTick(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        UUID id = player.getUUID();
        boolean holding = InfinityStellarisItemSupport.isHoldingInfinityStellaris(player);
        boolean inInventory = InfinityStellarisItemSupport.hasInfinityStellarisInInventory(player);

        if (holding) {
            if (player.tickCount % BLADE_REFRESH_INTERVAL == 0) {
                refreshHeldStats(player);
            }

            // 顶尖常驻视觉特效
            if (player.level() instanceof ServerLevel serverLevel && player.tickCount % 2 == 0) {
                double px = player.getX();
                double py = player.getY() + player.getBbHeight() / 2.0;
                double pz = player.getZ();
                serverLevel.sendParticles(DRAGON_BREATH_PARTICLE, px, py, pz, 3, 0.5, 0.5, 0.5, 0.02);
                serverLevel.sendParticles(ParticleTypes.ENCHANT, px, py, pz, 5, 0.8, 0.8, 0.8, 0.1);
                serverLevel.sendParticles(ParticleTypes.END_ROD, px, player.getY(), pz, 1, 0.6, 0.1, 0.6, 0.01);
            }
        }

        if (inInventory) {
            if (player.getHealth() < player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }

            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0F);
            player.fallDistance = 0.0;
            if (!player.isCreative() && !player.isSpectator() && !player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                PLAYERS_WITH_FLIGHT.add(id);
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
        } else if (!player.isCreative() && !player.isSpectator() && PLAYERS_WITH_FLIGHT.remove(id)) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    public static void onPlayerLogout(Player player) {
        clearPlayer(player);
    }

    public static void onPlayerChangedDimension(Player player) {
        clearPlayer(player);
    }

    private static void refreshHeldStats(Player player) {
        if (InfinityStellarisItemSupport.isInfinityStellaris(player.getMainHandItem())) {
            InfinityStellarisDefinitions.ensureStats(player.getMainHandItem(), player.level());
        }

        if (InfinityStellarisItemSupport.isInfinityStellaris(player.getOffhandItem())) {
            InfinityStellarisDefinitions.ensureStats(player.getOffhandItem(), player.level());
        }
    }

    private static void clearPlayer(Player player) {
        UUID id = player.getUUID();
        if (!player.isCreative() && !player.isSpectator() && PLAYERS_WITH_FLIGHT.remove(id)) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }

        EntropyDissolutionLogic.clearPlayer(id);
        CosmicStringCutLogic.clearPlayer(id);
        CurvatureRuptureLogic.clearPlayer(player);
    }

    private static void restoreFinalWeaponBearer(Player player) {
        player.invulnerableTime = Math.max(player.invulnerableTime, 20);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        player.deathTime = 0;
    }
}
