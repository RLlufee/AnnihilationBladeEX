package qwq.qingyi.annihilationblade.annihilation_blade;

import com.mojang.logging.LogUtils;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.common.SlashBladeStateSupport;
import qwq.qingyi.annihilationblade.registry.ModItems;

import javax.annotation.Nullable;
import java.util.List;

public final class AnnihilationBladeDefinitions {
    private static final String MODID = AnnihilationBladeex.MODID;
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String NAME = "annihilation_blade";
    public static final Identifier ID = prefix(NAME);
    public static final String DESCRIPTION_ID = "item." + MODID + ".annihilation_blade";
    private static final int LEGACY_INITIAL_PROUD_SOUL = 100000;
    private static final int LEGACY_INITIAL_KILL_COUNT = 10000;
    private static final int LEGACY_INITIAL_REFINE = 1000;
    private static final Identifier COMBO_ROOT_ID = Identifier.fromNamespaceAndPath("slashblade", "standby");
    private static final Identifier BLADE_MODEL = prefix("model/annihilation_blade.obj");
    private static final Identifier BLADE_TEXTURE = prefix("model/annihilation_blade.png");
    private static final Identifier SPATIAL_FRACTURE_ID = prefix("spatial_fracture");
    private static final List<Identifier> SPECIAL_EFFECTS = List.of(
            prefix("dankong"),
            prefix("world_rift"),
            prefix("terminus_echo"),
            prefix("void_dominion"),
            prefix("causality_collapse"),
            prefix("starless_judgement"),
            prefix("phantom_judgement"),
            prefix("abyssal_decree")
    );

    private AnnihilationBladeDefinitions() {
    }

    private static Identifier prefix(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private static Item bladeItem() {
        return ModItems.ANNIHILATION_BLADE;
    }

    public static ItemStack createStack() {
        ItemStack stack = new ItemStack(bladeItem());
        applyStats(stack);
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
        ItemStack stack = new ItemStack(bladeItem());
        applyStats(stack, level);
        return stack;
    }

    public static void applyStats(ItemStack stack) {
        applyStats(stack, (HolderLookup.Provider) null);
    }

    public static void applyStats(ItemStack stack, @Nullable Level level) {
        applyStats(stack, level == null ? null : level.registryAccess());
    }

    public static void ensureStats(ItemStack stack) {
        applyRuntimeStats(stack);
    }

    public static void ensureStats(ItemStack stack, @Nullable Level level) {
        applyRuntimeStats(stack);
    }

    public static void refreshHeldStats(ItemStack stack, Entity holder) {
        applyRuntimeStats(stack);
    }

    private static ItemStack getDefinedStack(Level level) {
        try {
            SlashBladeDefinition definition = SlashBlade.getSlashBladeDefinitionRegistry(level).getValue(ID);
            return definition == null ? ItemStack.EMPTY : definition.getBlade(bladeItem());
        } catch (RuntimeException exception) {
            LOGGER.warn("读取终焉之刃名刀定义失败: {}", ID, exception);
            return ItemStack.EMPTY;
        }
    }

    private static void applyStats(ItemStack stack, @Nullable HolderLookup.Provider registries) {
        if (!stack.isEmpty()) {
            applyRuntimeStats(stack);
            applyEnchantments(stack, registries);
        }
    }

    /** 已有武器只维护刀属性，保留玩家当前的附魔组件。 */
    private static void applyRuntimeStats(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        int currentProud = SlashBladeStateSupport.proudSoul(stack);
        int currentKill = SlashBladeStateSupport.killCount(stack);
        int currentRefine = SlashBladeStateSupport.refine(stack);
        boolean legacyInitialProgress = currentProud == LEGACY_INITIAL_PROUD_SOUL
                && currentKill == LEGACY_INITIAL_KILL_COUNT
                && currentRefine == LEGACY_INITIAL_REFINE;
        int proudSoul = legacyInitialProgress ? 0 : currentProud;
        int killCount = legacyInitialProgress ? 0 : currentKill;
        int refine = legacyInitialProgress ? 0 : currentRefine;

        SlashBladeStateSupport.applyRuntimeState(
                stack,
                DESCRIPTION_ID,
                2000,
                50.0F,
                proudSoul,
                killCount,
                refine,
                SPATIAL_FRACTURE_ID,
                COMBO_ROOT_ID,
                CarryType.NINJA,
                0xFFAA00FF,
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
        stack.enchant(enchantments.getOrThrow(Enchantments.FIRE_ASPECT), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.SMITE), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.BANE_OF_ARTHROPODS), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.LOOTING), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.SWEEPING_EDGE), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.POWER), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.MENDING), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.THORNS), 10);
    }

    public static ItemStack createFromLookup(HolderLookup.Provider registries) {
        ItemStack defined = SlashBladeStateSupport.getDefinedStack(registries, ID, bladeItem());
        if (!defined.isEmpty()) {
            applyRuntimeStats(defined);
            return defined;
        }
        ItemStack stack = createStack();
        applyEnchantments(stack, registries);
        return stack;
    }
}
