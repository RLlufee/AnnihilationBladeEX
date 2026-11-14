package qwq.qingyi.annihilationblade.annihilation_blade.item;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import qwq.qingyi.annihilationblade.annihilation_blade.AnnihilationBladeDefinitions;

import java.util.function.Consumer;

public class ItemAnnihilationBlade extends ItemSlashBlade {
    public ItemAnnihilationBlade(Item.Properties properties) {
        super(ToolMaterial.NETHERITE, 100, -2.4F, properties.fireResistant().stacksTo(1).component(DataComponents.UNBREAKABLE, Unit.INSTANCE));
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        return AnnihilationBladeDefinitions.createStack();
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull ServerLevel level, @NotNull Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (entity.tickCount % 20 == 0) {
            AnnihilationBladeDefinitions.ensureStats(stack, level);
        }
    }

    @Override
    public boolean releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entityLiving, int timeLeft) {
        AnnihilationBladeDefinitions.refreshHeldStats(stack, entityLiving);
        return super.releaseUsing(stack, level, entityLiving, timeLeft);
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
        tooltip.accept(Component.empty());
        tooltip.accept(Component.translatable("item.annihilationbladeex.passive").withStyle(ChatFormatting.GOLD));
        tooltip.accept(Component.translatable("item.annihilationbladeex.desc.line1").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.accept(Component.translatable("item.annihilationbladeex.desc.line2").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.accept(Component.translatable("item.annihilationbladeex.desc.line3").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.accept(Component.translatable("item.annihilationbladeex.desc.line4").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.accept(Component.empty());
        tooltip.accept(Component.translatable("item.annihilationbladeex.infinite_damage").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
