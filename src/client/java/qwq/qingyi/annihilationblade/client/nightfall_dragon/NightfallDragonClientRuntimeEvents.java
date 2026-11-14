package qwq.qingyi.annihilationblade.client.nightfall_dragon;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import qwq.qingyi.annihilationblade.nightfall_dragon.entity.NightfallDragonScreenShakeEntity;

/**
 * 永夜魔龙屏幕震动角度采样逻辑，由 {@code CameraMixin} 在相机对齐阶段调用。
 */
public final class NightfallDragonClientRuntimeEvents {
    public record ShakeOffset(float pitchDelta, float yawDelta, float rollDelta) {
    }

    private NightfallDragonClientRuntimeEvents() {
    }

    public static ShakeOffset sampleCameraShake(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.isPaused()) {
            return null;
        }

        float age = player.tickCount + partialTick;
        float shakeAmplitude = 0.0F;
        for (NightfallDragonScreenShakeEntity shake : player.level().getEntitiesOfClass(
                NightfallDragonScreenShakeEntity.class,
                player.getBoundingBox().inflate(24.0D)
        )) {
            if (shake.distanceTo(player) < shake.getRadius()) {
                shakeAmplitude += shake.getShakeAmount(player, partialTick);
            }
        }

        if (shakeAmplitude <= 0.0F) {
            return null;
        }

        shakeAmplitude = Math.min(shakeAmplitude, 1.0F);
        return new ShakeOffset(
                (float) (shakeAmplitude * Math.cos(age * 3.0F + 2.0F) * 25.0F),
                (float) (shakeAmplitude * Math.cos(age * 5.0F + 1.0F) * 25.0F),
                (float) (shakeAmplitude * Math.cos(age * 4.0F) * 25.0F)
        );
    }
}
