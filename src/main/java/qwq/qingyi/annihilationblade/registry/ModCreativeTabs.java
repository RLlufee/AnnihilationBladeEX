package qwq.qingyi.annihilationblade.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.annihilation_blade.AnnihilationBladeDefinitions;
import qwq.qingyi.annihilationblade.blood_prison.BloodPrisonDefinitions;
import qwq.qingyi.annihilationblade.infinity_stellaris.InfinityStellarisDefinitions;
import qwq.qingyi.annihilationblade.loli_blade.LoliBladeDefinitions;
import qwq.qingyi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;

/**
 * 模组创造模式物品栏标签页注册。
 */
public final class ModCreativeTabs {
    public static final CreativeModeTab SLASHBLADE_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            AnnihilationBladeex.prefix("slashblade_tab"),
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("item.annihilationbladeex.tab_title"))
                    .icon(ModCreativeTabs::createIcon)
                    .displayItems((parameters, output) -> {
                        output.accept(AnnihilationBladeDefinitions.createFromLookup(parameters.holders()));
                        output.accept(NightfallDragonDefinitions.createFromLookup(parameters.holders()));
                        output.accept(BloodPrisonDefinitions.createStack(parameters.holders()));
                        output.accept(InfinityStellarisDefinitions.createFromLookup(parameters.holders()));
                        output.accept(LoliBladeDefinitions.createFromLookup(parameters.holders()));
                        output.accept(new ItemStack(ModItems.ANNIHILATION_FRAGMENT));
                        output.accept(new ItemStack(ModItems.ANNIHILATION_CORE));
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }

    private static ItemStack createIcon() {
        return AnnihilationBladeDefinitions.createStack();
    }

    public static void register() {
        // 触发静态初始化完成注册
    }
}
