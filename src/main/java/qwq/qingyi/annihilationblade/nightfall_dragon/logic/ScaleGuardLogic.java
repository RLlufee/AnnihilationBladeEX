package qwq.qingyi.annihilationblade.nightfall_dragon.logic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import qwq.qingyi.annihilationblade.config.ModConfig;
import qwq.qingyi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;
import qwq.qingyi.annihilationblade.nightfall_dragon.entity.ScaleGuardSwordEntity;
import qwq.qingyi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;

/**
 * 护身逆鳞解放逻辑（觉醒形态）：召唤环绕自身的高速幻影剑阵并在收束后向下俯冲爆破。
 */
public final class ScaleGuardLogic {
    private ScaleGuardLogic() {
    }

    private static int getSwordCount() {
        return ModConfig.COMMON.nightfallDragon.scaleGuardSwordCount.getValue();
    }

    public static void prepareCast(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !canUseAwakenedArt(serverPlayer)) {
            return;
        }

        ServerLevel level = (ServerLevel) serverPlayer.level();
        Vec3 center = serverPlayer.position().add(0.0D, 1.1D, 0.0D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 0.55F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.9F, 0.7F);
        level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F), center.x, center.y, center.z, 32, 1.6D, 0.45D, 1.6D, 0.02D);
    }

    public static void unleash(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !canUseAwakenedArt(serverPlayer)) {
            return;
        }

        ServerLevel level = (ServerLevel) serverPlayer.level();
        int swordCount = getSwordCount();
        for (int i = 0; i < swordCount; i++) {
            ScaleGuardSwordEntity controller = ScaleGuardSwordEntity.create(level, serverPlayer, i, swordCount);
            if (controller != null) {
                level.addFreshEntity(controller);
            }
        }

        Vec3 center = serverPlayer.position().add(0.0D, 1.1D, 0.0D);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.8F, 0.55F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.3F, 0.65F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, 48, 1.9D, 0.55D, 1.9D, 0.035D);
    }

    private static boolean canUseAwakenedArt(Player player) {
        ItemStack stack = player.getMainHandItem();
        return NightfallDragonItemSupport.isNightfallDragon(stack)
                && NightfallDragonDefinitions.FORM_AWAKENED.equals(NightfallDragonDefinitions.getForm(stack));
    }
}
