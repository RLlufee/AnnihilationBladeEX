package QWQ.QingYi.annihilationbladeex.loli_blade;

import QWQ.QingYi.annihilationbladeex.AnnihilationBladeEX;
import QWQ.QingYi.annihilationbladeex.common.NamedBladeStacks;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateData;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.capability.slashblade.SlashBladeDataComponents;
import mods.flammpfeil.slashblade.client.renderer.CarryType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class LoliBladeDefinitions {
   public static final String NAME = "loli_blade";
   public static final ResourceLocation ID = AnnihilationBladeEX.prefix(NAME);
   public static final String DESCRIPTION_ID = "item." + AnnihilationBladeEX.MODID + ".loli_blade";
   private static final ResourceLocation COMBO_ROOT_ID = ResourceLocation.fromNamespaceAndPath("slashblade", "standby");
   private static final ResourceLocation BLADE_MODEL = AnnihilationBladeEX.prefix("model/loli_blade.obj");
   private static final ResourceLocation BLADE_TEXTURE = AnnihilationBladeEX.prefix("model/loli_blade.png");
   private static final ResourceLocation LOLI_SLASH_ART = AnnihilationBladeEX.prefix("loli_area_execution");
   private static final ResourceLocation LOLI_FACING_EXECUTION = AnnihilationBladeEX.prefix("loli_facing_execution");
   private static final int SUMMONED_SWORD_COLOR = 16738740;
   private static final String IDENTITY_TAG = "IsLoliBlade";
   private static final String OWNER_TAG = "OwnerUUID";

   private LoliBladeDefinitions() {
   }

   public static ItemStack createStack() {
      ItemStack stack = new ItemStack(slashBladeItem());
      applyStats(stack, (HolderLookup.Provider)null);
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

   private static net.minecraft.world.item.Item slashBladeItem() {
      return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("slashblade", "slashblade"));
   }

   public static void ensureStats(ItemStack stack) {
      applyStats(stack, (HolderLookup.Provider)null);
   }

   public static void ensureStats(ItemStack stack, @Nullable Level level) {
      applyStats(stack, level == null ? null : level.registryAccess());
   }

   public static boolean isLoliBlade(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      boolean stateMatch = BladeStateAccess.of(stack).map(state ->
         DESCRIPTION_ID.equals(state.getTranslationKey())
            || state.getModel().map(BLADE_MODEL::equals).orElse(false)
            || state.getTexture().map(BLADE_TEXTURE::equals).orElse(false)
            || state.hasSpecialEffect(LOLI_FACING_EXECUTION)
      ).orElse(false);
      if (stateMatch || DESCRIPTION_ID.equals(stack.getDescriptionId())) {
         return true;
      }

      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      if (customData == null) {
         return false;
      }

      CompoundTag tag = customData.copyTag();
      return tag.getBoolean(IDENTITY_TAG)
         || BLADE_MODEL.toString().equals(tag.getString("ModelName"))
         || BLADE_TEXTURE.toString().equals(tag.getString("TextureName"));
   }

   /** 首次进入玩家物品栏时绑定主人，绑定信息保存在 1.21.1 的自定义数据组件中。 */
   public static void bindUnownedStacks(Player player) {
      refreshAndBind(player.getMainHandItem(), player.getUUID(), player.level());
      refreshAndBind(player.getOffhandItem(), player.getUUID(), player.level());
      for (ItemStack stack : player.getInventory().items) {
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
      if (tag.hasUUID(OWNER_TAG)) {
         return playerId.equals(tag.getUUID(OWNER_TAG));
      }

      String legacyOwner = tag.getString(OWNER_TAG);
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

      CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putUUID(OWNER_TAG, playerId));
   }

   private static boolean hasOwner(ItemStack stack) {
      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      if (customData == null) {
         return false;
      }

      CompoundTag tag = customData.copyTag();
      return tag.hasUUID(OWNER_TAG) || !tag.getString(OWNER_TAG).isEmpty();
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

      stack.set(DataComponents.MAX_DAMAGE, 1000);
      stack.setDamageValue(0);
      stack.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
      stack.set(DataComponents.RARITY, Rarity.EPIC);

      int proudSoul = BladeStateAccess.of(stack).map(ISlashBladeState::getProudSoulCount).orElse(0);
      int killCount = BladeStateAccess.of(stack).map(ISlashBladeState::getKillCount).orElse(0);
      int refine = BladeStateAccess.of(stack).map(ISlashBladeState::getRefine).orElse(0);
      stack.set(SlashBladeDataComponents.BLADE_STATE_DATA.get(), new BladeStateData(
         DESCRIPTION_ID,
         4.0F,
         proudSoul,
         killCount,
         refine,
         false,
         false,
         LOLI_SLASH_ART,
         true,
         false, // destructable（2.0.7 新增）：断裂时不销毁物品
         COMBO_ROOT_ID,
         CarryType.NINJA,
         SUMMONED_SWORD_COLOR,
         false,
         Vec3.ZERO,
         Optional.of(BLADE_TEXTURE),
         Optional.of(BLADE_MODEL),
         List.of(LOLI_FACING_EXECUTION)
      ));
      BladeStateAccess.ensureRuntimeComponent(stack);
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
