package qwq.qingyi.annihilationblade.common;

import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 拔刀剑统一目标判定工具：沿用拔刀剑原生可攻击实体过滤逻辑。
 */
public final class SlashBladeTargeting {
    private SlashBladeTargeting() {
    }

    public static boolean canAttack(Player attacker, LivingEntity target) {
        if (!(attacker.level() instanceof ServerLevel level)) {
            return false;
        }
        return attacker != target && target.isAlive() && TargetSelector.test.test(level, attacker, target);
    }
}
