package qwq.qingyi.annihilationblade.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.annihilation_blade.item.ItemAnnihilationBlade;
import qwq.qingyi.annihilationblade.annihilation_blade.item.ItemAnnihilationCore;
import qwq.qingyi.annihilationblade.annihilation_blade.item.ItemAnnihilationFragment;
import qwq.qingyi.annihilationblade.nightfall_dragon.item.ItemNightfallDragon;

import java.util.function.Function;

/**
 * 模组物品注册表：MC 26.1.2 必须在构建 Item 时先通过 Properties.setId 绑定 ResourceKey。
 */
public final class ModItems {
    public static final ItemAnnihilationBlade ANNIHILATION_BLADE =
            register("annihilation_blade", props -> new ItemAnnihilationBlade(props));
    public static final ItemAnnihilationFragment ANNIHILATION_FRAGMENT =
            register("annihilation_fragment", ItemAnnihilationFragment::new);
    public static final ItemAnnihilationCore ANNIHILATION_CORE =
            register("annihilation_core", ItemAnnihilationCore::new);
    public static final ItemNightfallDragon NIGHTFALL_DRAGON =
            register("nightfall_dragon", ItemNightfallDragon::new);

    private ModItems() {
    }

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory) {
        Identifier id = AnnihilationBladeex.prefix(name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        T item = factory.apply(new Item.Properties().setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void register() {
        // 触发静态初始化完成注册
    }
}
