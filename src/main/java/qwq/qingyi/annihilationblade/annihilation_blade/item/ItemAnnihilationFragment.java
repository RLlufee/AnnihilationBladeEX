package qwq.qingyi.annihilationblade.annihilation_blade.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ItemAnnihilationFragment extends Item {
    public ItemAnnihilationFragment(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            @NotNull Item.TooltipContext context,
            @NotNull TooltipDisplay display,
            @NotNull Consumer<Component> tooltip,
            @NotNull TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.annihilationbladeex.annihilation_fragment.tip"));
    }
}
