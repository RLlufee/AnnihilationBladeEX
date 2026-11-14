package qwq.qingyi.annihilationblade.blood_prison;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.init.SBItems;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.common.SlashBladeStateSupport;

import javax.annotation.Nullable;
import java.util.List;

public final class BloodPrisonDefinitions {
    public static final String NAME = "blood_prison";
    public static final Identifier ID = AnnihilationBladeex.prefix(NAME);
    public static final Identifier INFERNAL_SLAUGHTER_ID = AnnihilationBladeex.prefix("infernal_slaughter");
    public static final Identifier BLOOD_LEECH_ID = AnnihilationBladeex.prefix("blood_leech");
    public static final Identifier SPIRIT_SHIELD_ID = AnnihilationBladeex.prefix("spirit_shield");
    public static final Identifier PHANTOM_MARK_ID = AnnihilationBladeex.prefix("phantom_mark");
    public static final String DESCRIPTION_ID = "item." + AnnihilationBladeex.MODID + ".blood_prison";
    private static final Identifier COMBO_ROOT_ID = Identifier.fromNamespaceAndPath("slashblade", "standby");
    private static final Identifier BLADE_MODEL = AnnihilationBladeex.prefix("model/blood_prison.obj");
    private static final Identifier BLADE_TEXTURE = AnnihilationBladeex.prefix("model/blood_prison.png");
    private static final List<Identifier> SPECIAL_EFFECTS = List.of(
            BLOOD_LEECH_ID,
            SPIRIT_SHIELD_ID,
            PHANTOM_MARK_ID,
            Identifier.fromNamespaceAndPath("slashblade", "wither_edge")
    );

    private BloodPrisonDefinitions() {
    }

    public static ItemStack createStack() {
        ItemStack stack = createFallbackStack();
        ensureStats(stack);
        return stack;
    }

    public static ItemStack createStack(@Nullable Level level) {
        if (level != null) {
            ItemStack stack = getDefinedStack(level);
            if (!stack.isEmpty()) {
                ensureStats(stack, level);
                return stack;
            }
        }
        ItemStack stack = createFallbackStack();
        ensureStats(stack);
        applyEnchantments(stack, level == null ? null : level.registryAccess());
        return stack;
    }

    public static ItemStack createStack(HolderLookup.Provider registries) {
        ItemStack defined = SlashBladeStateSupport.getDefinedStack(registries, ID, null);
        if (!defined.isEmpty()) {
            ensureStats(defined, registries);
            return defined;
        }
        ItemStack stack = createFallbackStack();
        ensureStats(stack, registries);
        applyEnchantments(stack, registries);
        return stack;
    }

    public static boolean isBloodPrison(ItemStack stack) {
        return !stack.isEmpty() && DESCRIPTION_ID.equals(SlashBladeStateSupport.translationKey(stack));
    }

    public static void ensureStats(ItemStack stack) {
        ensureStats(stack, (HolderLookup.Provider) null);
    }

    public static void ensureStats(ItemStack stack, @Nullable Level level) {
        ensureStats(stack, level == null ? null : level.registryAccess());
    }

    public static void ensureStats(ItemStack stack, Entity holder) {
        ensureStats(stack, holder == null ? null : holder.registryAccess());
    }

    private static ItemStack getDefinedStack(Level level) {
        try {
            SlashBladeDefinition definition = SlashBlade.getSlashBladeDefinitionRegistry(level).getValue(ID);
            return definition == null ? ItemStack.EMPTY : definition.getBlade();
        } catch (RuntimeException exception) {
            AnnihilationBladeex.LOGGER.warn("读取血狱名刀定义失败: {}", ID, exception);
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack createFallbackStack() {
        return new ItemStack(SBItems.SLASHBLADE);
    }

    private static void ensureStats(ItemStack stack, @Nullable HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return;
        }

        SlashBladeStateSupport.applyRuntimeState(
                stack,
                DESCRIPTION_ID,
                2400,
                16.0F,
                SlashBladeStateSupport.proudSoul(stack),
                SlashBladeStateSupport.killCount(stack),
                SlashBladeStateSupport.refine(stack),
                INFERNAL_SLAUGHTER_ID,
                COMBO_ROOT_ID,
                CarryType.NINJA,
                0xFFFF2020,
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
        stack.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.MENDING), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.POWER), 10);
    }
}
