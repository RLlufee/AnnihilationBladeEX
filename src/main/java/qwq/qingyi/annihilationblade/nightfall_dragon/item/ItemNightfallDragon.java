package qwq.qingyi.annihilationblade.nightfall_dragon.item;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import qwq.qingyi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;

public class ItemNightfallDragon extends ItemSlashBlade {
    public ItemNightfallDragon(Item.Properties properties) {
        super(ToolMaterial.NETHERITE, 100, -2.4F, properties.fireResistant().stacksTo(1).component(DataComponents.UNBREAKABLE, Unit.INSTANCE));
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        return NightfallDragonDefinitions.createStack();
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull ServerLevel level, @NotNull Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (entity.tickCount % 20 == 0) {
            NightfallDragonDefinitions.ensureStats(stack, level);
        }
    }

    @Override
    public boolean releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entityLiving, int timeLeft) {
        NightfallDragonDefinitions.ensureStats(stack, entityLiving.level());
        return super.releaseUsing(stack, level, entityLiving, timeLeft);
    }
}
