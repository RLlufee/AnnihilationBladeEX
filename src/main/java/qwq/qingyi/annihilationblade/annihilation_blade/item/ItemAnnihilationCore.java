package qwq.qingyi.annihilationblade.annihilation_blade.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import qwq.qingyi.annihilationblade.annihilation_blade.AnnihilationBladeDefinitions;

import java.util.function.Consumer;

public class ItemAnnihilationCore extends Item {
    public ItemAnnihilationCore(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            ItemStack godSword = AnnihilationBladeDefinitions.createStack(level);
            if (!godSword.isEmpty() && player.getInventory().add(godSword)) {
                itemStack.shrink(1);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
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
        tooltip.accept(Component.translatable("item.annihilationbladeex.annihilation_core.tip"));
    }
}
