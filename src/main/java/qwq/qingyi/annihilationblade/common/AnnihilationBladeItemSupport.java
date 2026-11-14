package qwq.qingyi.annihilationblade.common;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import qwq.qingyi.annihilationblade.annihilation_blade.AnnihilationBladeDefinitions;
import qwq.qingyi.annihilationblade.registry.ModItems;

/**
 * 湮灭之刃手持与物品识别工具。
 */
public final class AnnihilationBladeItemSupport {
    private AnnihilationBladeItemSupport() {
    }

    public static boolean isHoldingAnnihilationBlade(Player player) {
        return isAnnihilationBlade(player.getMainHandItem()) || isAnnihilationBlade(player.getOffhandItem());
    }

    public static boolean isAnnihilationBlade(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.is(ModItems.ANNIHILATION_BLADE)
                || AnnihilationBladeDefinitions.DESCRIPTION_ID.equals(stack.getItem().getDescriptionId())
                || SlashBladeStateSupport.state(stack)
                        .map(state -> AnnihilationBladeDefinitions.DESCRIPTION_ID.equals(state.getTranslationKey()))
                        .orElse(false);
    }
}
