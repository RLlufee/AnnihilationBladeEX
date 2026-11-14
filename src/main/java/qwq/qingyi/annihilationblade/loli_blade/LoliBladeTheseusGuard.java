package qwq.qingyi.annihilationblade.loli_blade;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import qwq.qingyi.annihilationblade.config.ModConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 服务端守卫（同步覆盖整合单人内置服务端与独立专用服务端）：每 tick 校验受保护持有者存在状态并在脱离世界时重建恢复。
 */
public final class LoliBladeTheseusGuard {
    /** 连续缺席 tick 阈值，用于避开维度切换瞬间的短暂 removed 状态。 */
    private static final int RECONSTRUCT_DELAY = 3;
    private static final Map<UUID, Integer> ABSENT_TICKS = new HashMap<>();

    private LoliBladeTheseusGuard() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(LoliBladeTheseusGuard::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        if (!ModConfig.COMMON.loliBlade.ultimateTheseus.getValue() || server == null) {
            return;
        }

        for (UUID id : LoliBladeProtectionManager.getProtectedPlayers()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                ABSENT_TICKS.remove(id);
                continue;
            }

            if (player.isRemoved()) {
                int absent = ABSENT_TICKS.merge(id, 1, Integer::sum);
                if (absent >= RECONSTRUCT_DELAY) {
                    reconstruct(player);
                    ABSENT_TICKS.remove(id);
                }
            } else {
                ABSENT_TICKS.remove(id);
            }
        }
    }

    /** 以原 UUID 解除移除状态标记、复位生命值与死亡计时，并重新断言无敌与飞行能力。 */
    private static void reconstruct(ServerPlayer player) {
        ((LoliBladeEntityAccess) player).annihilationblade$unsetRemoved();
        player.setHealth(player.getMaxHealth());
        player.deathTime = 0;
        player.invulnerableTime = 0;
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().invulnerable = true;
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }
}
