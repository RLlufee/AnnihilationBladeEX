package qwq.qingyi.annihilationblade.common;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;

import javax.annotation.Nullable;

/**
 * 从数据包名刀注册表读取对应刀剑定义并实例化物品栈。
 */
public final class NamedBladeStacks {
    private NamedBladeStacks() {
    }

    public static ItemStack get(@Nullable Level level, String bladeName) {
        return get(level, bladeName, null);
    }

    public static ItemStack get(@Nullable Level level, String bladeName, @Nullable Item bladeItem) {
        if (level == null || bladeName == null || bladeName.isEmpty()) {
            return ItemStack.EMPTY;
        }

        try {
            Registry<SlashBladeDefinition> registry = SlashBlade.getSlashBladeDefinitionRegistry(level);
            SlashBladeDefinition definition = registry.getValue(Identifier.fromNamespaceAndPath(AnnihilationBladeex.MODID, bladeName));
            if (definition == null) {
                return ItemStack.EMPTY;
            }

            ItemStack stack = bladeItem == null ? definition.getBlade() : definition.getBlade(bladeItem);
            return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        } catch (RuntimeException exception) {
            AnnihilationBladeex.LOGGER.warn("读取数据包名刀定义失败: {}", bladeName, exception);
            return ItemStack.EMPTY;
        }
    }
}
