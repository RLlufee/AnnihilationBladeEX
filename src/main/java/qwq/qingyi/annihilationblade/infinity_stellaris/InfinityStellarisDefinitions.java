package qwq.qingyi.annihilationblade.infinity_stellaris;

import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.init.SBItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.common.NamedBladeStacks;
import qwq.qingyi.annihilationblade.common.SlashBladeStateSupport;

import javax.annotation.Nullable;
import java.util.List;

public final class InfinityStellarisDefinitions {
    public static final String NAME = "infinity_stellaris";
    public static final Identifier ID = AnnihilationBladeex.prefix(NAME);
    public static final String DESCRIPTION_ID = "item.annihilationbladeex.infinity_stellaris";
    private static final Identifier COMBO_ROOT_ID = Identifier.fromNamespaceAndPath("slashblade", "standby");
    private static final Identifier BLADE_MODEL = AnnihilationBladeex.prefix("model/infinity_stellaris.obj");
    private static final Identifier BLADE_TEXTURE = AnnihilationBladeex.prefix("model/infinity_stellaris.png");
    private static final Identifier VACUUM_DECAY_COLLAPSE = AnnihilationBladeex.prefix("vacuum_decay_collapse");
    private static final int SUMMONED_SWORD_COLOR = 4248287;
    private static final List<Identifier> SPECIAL_EFFECTS = List.of(
            AnnihilationBladeex.prefix("entropy_dissolution"),
            AnnihilationBladeex.prefix("curvature_rupture"),
            AnnihilationBladeex.prefix("gamma_thunderburst"),
            AnnihilationBladeex.prefix("cosmic_string_cut")
    );

    private InfinityStellarisDefinitions() {
    }

    private static Item slashbladeItem() {
        return SBItems.SLASHBLADE;
    }

    public static ItemStack createStack() {
        return createStack(null);
    }

    public static ItemStack createStack(@Nullable Level level) {
        ItemStack datapackStack = NamedBladeStacks.get(level, NAME, slashbladeItem());
        if (!datapackStack.isEmpty()) {
            ensureStats(datapackStack, level);
            return datapackStack;
        }

        ItemStack stack = new ItemStack(slashbladeItem());
        applyStats(stack, level);
        return stack;
    }

    public static void ensureStats(ItemStack stack) {
        applyRuntimeStats(stack);
    }

    public static void ensureStats(ItemStack stack, @Nullable Level level) {
        applyRuntimeStats(stack);
    }

    private static void applyStats(ItemStack stack, @Nullable Level level) {
        applyStats(stack, level == null ? null : level.registryAccess());
    }

    private static void applyStats(ItemStack stack, @Nullable HolderLookup.Provider registries) {
        if (!stack.isEmpty()) {
            applyRuntimeStats(stack);
            applyEnchantments(stack, registries);
        }
    }

    /** 创造栏创建新刀时使用注册表，默认附魔无需在后续 Tick 补齐。 */
    public static ItemStack createFromLookup(HolderLookup.Provider registries) {
        ItemStack defined = SlashBladeStateSupport.getDefinedStack(registries, ID, slashbladeItem());
        if (!defined.isEmpty()) {
            applyRuntimeStats(defined);
            return defined;
        }
        ItemStack stack = new ItemStack(slashbladeItem());
        applyStats(stack, registries);
        return stack;
    }

    /** 已有武器的属性维护不补齐、降级或覆盖附魔。 */
    private static void applyRuntimeStats(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        SlashBladeStateSupport.applyRuntimeState(
                stack,
                DESCRIPTION_ID,
                2000,
                1000000.0F,
                SlashBladeStateSupport.proudSoul(stack),
                SlashBladeStateSupport.killCount(stack),
                SlashBladeStateSupport.refine(stack),
                VACUUM_DECAY_COLLAPSE,
                COMBO_ROOT_ID,
                CarryType.NINJA,
                SUMMONED_SWORD_COLOR,
                BLADE_TEXTURE,
                BLADE_MODEL,
                SPECIAL_EFFECTS
        );
    }

    private static void applyEnchantments(ItemStack stack, @Nullable HolderLookup.Provider registries) {
        if (registries == null) {
            return;
        }
        var enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
        stack.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.SMITE), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.BANE_OF_ARTHROPODS), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.KNOCKBACK), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.LOOTING), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.SWEEPING_EDGE), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.POWER), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.PUNCH), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.INFINITY), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.QUICK_CHARGE), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.MULTISHOT), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.PIERCING), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.MENDING), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.THORNS), 10);
    }
}
