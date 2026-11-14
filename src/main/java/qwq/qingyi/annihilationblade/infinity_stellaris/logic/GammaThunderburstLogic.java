package qwq.qingyi.annihilationblade.infinity_stellaris.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import qwq.qingyi.annihilationblade.common.ServerTickScheduler;
import qwq.qingyi.annihilationblade.infinity_stellaris.entity.GammaThunderboltEntity;
import qwq.qingyi.annihilationblade.infinity_stellaris.item.InfinityStellarisItemSupport;
import qwq.qingyi.annihilationblade.registry.ModEntities;

/**
 * 伽马雷暴逻辑：攻击或挥刀时在玩家周身或指定方形区域内分轮降下伽马雷霆。
 */
public final class GammaThunderburstLogic {
    private static final int ROUNDS = 3;
    private static final int BOLTS_PER_ROUND = 12;
    private static final int RADIUS = 128;

    private GammaThunderburstLogic() {
    }

    public static void onHurt(LivingEntity target, DamageSource damageSource) {
        if (target.level().isClientSide() || EntropyDissolutionLogic.isInternalExecution(target)) {
            return;
        }

        Entity source = damageSource.getEntity();
        Entity directSource = damageSource.getDirectEntity();
        if (source instanceof Player player
                && !(directSource instanceof GammaThunderboltEntity)
                && EntropyDissolutionLogic.isInfinityDamage(player, damageSource, directSource)) {
            trigger(player);
        }
    }

    public static void trigger(Player player) {
        if (!(player.level() instanceof ServerLevel level) || !InfinityStellarisItemSupport.isHoldingInfinityStellaris(player)) {
            return;
        }

        for (int round = 0; round < ROUNDS; round++) {
            ServerTickScheduler.schedule(round, () -> spawnRound(level, player));
        }
    }

    private static void spawnRound(ServerLevel level, Player player) {
        if (!player.isAlive()) {
            return;
        }

        RandomSource random = player.getRandom();
        Vec3 origin = player.position();
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.8F, 1.4F + random.nextFloat() * 0.4F);
        spawnRandomBoltsAround(level, origin, RADIUS, BOLTS_PER_ROUND, random);
    }

    public static void spawnRandomBoltsAround(ServerLevel level, Vec3 origin, int radius, int count, RandomSource random) {
        for (int i = 0; i < count; i++) {
            int x = (int) Math.floor(origin.x) + random.nextInt(radius * 2 + 1) - radius;
            int z = (int) Math.floor(origin.z) + random.nextInt(radius * 2 + 1) - radius;
            spawnSurfaceBolt(level, x, z, origin.y);
        }
    }

    public static void spawnRandomBoltsInSquare(ServerLevel level, Vec3 center, double halfSize, int count, RandomSource random) {
        int bound = Math.max(1, (int) Math.round(halfSize));
        for (int i = 0; i < count; i++) {
            int x = (int) Math.floor(center.x) + random.nextInt(bound * 2 + 1) - bound;
            int z = (int) Math.floor(center.z) + random.nextInt(bound * 2 + 1) - bound;
            spawnSurfaceBolt(level, x, z, center.y);
        }
    }

    public static void spawnBolt(ServerLevel level, Vec3 position) {
        spawnBolt(level, position, -1);
    }

    public static void spawnBolt(ServerLevel level, Vec3 position, int fixedColor) {
        GammaThunderboltEntity bolt = ModEntities.GAMMA_THUNDERBOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.setFixedColor(fixedColor);
            bolt.snapTo(position.x, position.y, position.z);
            level.addFreshEntity(bolt);
        }
    }

    private static void spawnSurfaceBolt(ServerLevel level, int x, int z, double fallbackY) {
        int minY = level.getMinY();
        int maxY = level.getMaxY();
        int fallbackBlockY = Math.max(minY + 1, Math.min(maxY - 2, (int) Math.floor(fallbackY)));
        BlockPos loadCheck = new BlockPos(x, fallbackBlockY, z);
        if (!level.isLoaded(loadCheck)) {
            return;
        }

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= minY) {
            y = fallbackBlockY;
        }

        y = Math.max(minY + 1, Math.min(maxY - 2, y));
        spawnBolt(level, new Vec3(x + 0.5, y, z + 0.5));
    }
}
