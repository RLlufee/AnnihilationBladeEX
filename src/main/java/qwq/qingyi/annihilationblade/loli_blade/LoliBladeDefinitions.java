package qwq.qingyi.annihilationblade.loli_blade;

import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.init.SBItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.common.NamedBladeStacks;
import qwq.qingyi.annihilationblade.common.SlashBladeStateSupport;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class LoliBladeDefinitions {
    public static final String NAME = "loli_blade";
    public static final Identifier ID = AnnihilationBladeex.prefix(NAME);
    public static final String DESCRIPTION_ID = "item." + AnnihilationBladeex.MODID + ".loli_blade";
    private static final Identifier COMBO_ROOT_ID = Identifier.fromNamespaceAndPath("slashblade", "standby");
    private static final Identifier BLADE_MODEL = AnnihilationBladeex.prefix("model/loli_blade.obj");
    private static final Identifier BLADE_TEXTURE = AnnihilationBladeex.prefix("model/loli_blade.png");
    private static final Identifier LOLI_SLASH_ART = AnnihilationBladeex.prefix("loli_area_execution");
    private static final Identifier LOLI_FACING_EXECUTION = AnnihilationBladeex.prefix("loli_facing_execution");
    private static final int SUMMONED_SWORD_COLOR = 16738740;
    private static final String IDENTITY_TAG = "IsLoliBlade";
    private static final String OWNER_TAG = "OwnerUUID";

    private LoliBladeDefinitions() {
    }

    public static ItemStack createStack() {
        ItemStack stack = new ItemStack(slashBladeItem());
        applyStats(stack, null);
        return stack;
    }

    public static ItemStack createStack(@Nullable Level level) {
        ItemStack datapackStack = NamedBladeStacks.get(level, NAME);
        if (!datapackStack.isEmpty()) {
            ensureStats(datapackStack, level);
            return datapackStack;
        }
        ItemStack stack = new ItemStack(slashBladeItem());
        applyStats(stack, level == null ? null : level.registryAccess());
        applyEnchantments(stack, level == null ? null : level.registryAccess());
        return stack;
    }

    public static ItemStack createFromLookup(HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(slashBladeItem());
        applyStats(stack, registries);
        applyEnchantments(stack, registries);
        return stack;
    }

    private static Item slashBladeItem() {
        return SBItems.SLASHBLADE;
    }

    public static void ensureStats(ItemStack stack) {
        applyStats(stack, null);
    }

    public static void ensureStats(ItemStack stack, @Nullable Level level) {
        applyStats(stack, level == null ? null : level.registryAccess());
    }

    public static boolean isLoliBlade(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        boolean stateMatch = SlashBladeStateSupport.state(stack).map(state ->
                DESCRIPTION_ID.equals(state.getTranslationKey())
                        || state.getModel().map(BLADE_MODEL::equals).orElse(false)
                        || state.getTexture().map(BLADE_TEXTURE::equals).orElse(false)
                        || state.hasSpecialEffect(LOLI_FACING_EXECUTION)
        ).orElse(false);
        if (stateMatch || DESCRIPTION_ID.equals(SlashBladeStateSupport.translationKey(stack))) {
            return true;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        CompoundTag tag = customData.copyTag();
        return tag.getBooleanOr(IDENTITY_TAG, false)
                || BLADE_MODEL.toString().equals(tag.getStringOr("ModelName", ""))
                || BLADE_TEXTURE.toString().equals(tag.getStringOr("TextureName", ""));
    }

    /** 首次进入玩家物品栏时绑定主人，绑定信息保存在自定义数据组件中。 */
    public static void bindUnownedStacks(Player player) {
        refreshAndBind(player.getMainHandItem(), player.getUUID(), player.level());
        refreshAndBind(player.getOffhandItem(), player.getUUID(), player.level());
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            refreshAndBind(stack, player.getUUID(), player.level());
        }
    }

    public static boolean isOwnedBy(ItemStack stack, Player player) {
        return player != null && isOwnedBy(stack, player.getUUID());
    }

    public static boolean isOwnedBy(ItemStack stack, UUID playerId) {
        if (!isLoliBlade(stack) || playerId == null) {
            return false;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        CompoundTag tag = customData.copyTag();
        Optional<UUID> storedOwner = tag.read(OWNER_TAG, UUIDUtil.CODEC);
        if (storedOwner.isPresent()) {
            return playerId.equals(storedOwner.get());
        }

        String legacyOwner = tag.getStringOr(OWNER_TAG, "");
        if (!legacyOwner.isEmpty()) {
            try {
                return playerId.equals(UUID.fromString(legacyOwner));
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }

        return false;
    }

    private static void refreshAndBind(ItemStack stack, UUID playerId, @Nullable Level level) {
        if (!isLoliBlade(stack)) {
            return;
        }

        ensureStats(stack, level);
        bindIfUnowned(stack, playerId);
    }

    private static void bindIfUnowned(ItemStack stack, UUID playerId) {
        if (hasOwner(stack)) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.store(OWNER_TAG, UUIDUtil.CODEC, playerId));
    }

    private static boolean hasOwner(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        CompoundTag tag = customData.copyTag();
        return tag.read(OWNER_TAG, UUIDUtil.CODEC).isPresent() || !tag.getStringOr(OWNER_TAG, "").isEmpty();
    }

    private static void applyStats(ItemStack stack, @Nullable HolderLookup.Provider registries) {
        if (stack.isEmpty()) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean(IDENTITY_TAG, true);
            tag.putString("ModelName", BLADE_MODEL.toString());
            tag.putString("TextureName", BLADE_TEXTURE.toString());
            tag.putString("SlashArts", LOLI_SLASH_ART.toString());
            tag.putInt("SummonedSwordColor", SUMMONED_SWORD_COLOR);
            tag.putBoolean("Unbreakable", true);
        });

        SlashBladeStateSupport.applyRuntimeState(
                stack,
                DESCRIPTION_ID,
                1000,
                4.0F,
                SlashBladeStateSupport.proudSoul(stack),
                SlashBladeStateSupport.killCount(stack),
                SlashBladeStateSupport.refine(stack),
                LOLI_SLASH_ART,
                COMBO_ROOT_ID,
                CarryType.NINJA,
                SUMMONED_SWORD_COLOR,
                BLADE_TEXTURE,
                BLADE_MODEL,
                List.of(LOLI_FACING_EXECUTION)
        );
    }

    private static void applyEnchantments(ItemStack stack, @Nullable HolderLookup.Provider registries) {
        if (registries == null) {
            return;
        }
        var enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
        stack.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.LOOTING), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.MULTISHOT), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.POWER), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.SOUL_SPEED), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.FEATHER_FALLING), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.PROTECTION), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.KNOCKBACK), 10);
        stack.enchant(enchantments.getOrThrow(Enchantments.MENDING), 10);
    }
}
