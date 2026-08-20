package QWQ.QingYi.annihilationblade.common;

import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class SlashBladeTargeting {
   private SlashBladeTargeting() {
   }

   public static boolean canAttack(Player attacker, LivingEntity target) {
      return attacker != target && target.isAlive() && TargetSelector.test.test(attacker, target);
   }
}
