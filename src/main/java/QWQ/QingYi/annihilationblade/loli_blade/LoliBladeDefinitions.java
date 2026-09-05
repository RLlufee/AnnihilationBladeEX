package QWQ.QingYi.annihilationblade.loli_blade;

import QWQ.QingYi.annihilationblade.common.NamedBladeStacks;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import mods.flammpfeil.slashblade.init.SBItems;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

public final class LoliBladeDefinitions {
   public static final String NAME = "loli_blade";
   public static final String DESCRIPTION_ID = "item.annihilationblade.loli_blade";
   private static final ResourceLocation BLADE_MODEL = ResourceLocation.fromNamespaceAndPath("annihilationblade", "model/loli_blade.obj");
   private static final ResourceLocation BLADE_TEXTURE = ResourceLocation.fromNamespaceAndPath("annihilationblade", "model/loli_blade.png");
   private static final ResourceLocation LOLI_SLASH_ART = ResourceLocation.fromNamespaceAndPath("annihilationblade", "loli_area_execution");
   private static final String LOLI_FACING_EXECUTION = "annihilationblade:loli_facing_execution";
   private static final int SUMMONED_SWORD_COLOR = 16738740;
   private static final String OWNER_TAG = "OwnerUUID";

   private LoliBladeDefinitions() {
   }

   public static ItemStack createStack() {
      return createStack(null);
   }

   public static ItemStack createStack(@Nullable Level level) {
      ItemStack datapackStack = NamedBladeStacks.get(level, NAME);
      if (!datapackStack.isEmpty()) {
         ensureStats(datapackStack);
         return datapackStack;
      }

      ItemStack stack = new ItemStack(SBItems.slashblade);
      applyFallbackStats(stack);
      return stack;
   }

   public static void ensureStats(ItemStack stack) {
      if (!stack.isEmpty()) {
         ensureIdentity(stack);
         applyEnchantments(stack);
         applyBladeState(stack);
      }
   }

   public static void ensureStats(ItemStack stack, @Nullable Level level) {
      if (!stack.isEmpty()) {
         NamedBladeStacks.copyDefinitionTag(stack, level, NAME, null);
         ensureStats(stack);
      }
   }

   private static void applyFallbackStats(ItemStack stack) {
      if (!stack.isEmpty()) {
         ensureIdentity(stack);
         applyBladeState(stack);
      }
   }

   private static void ensureIdentity(ItemStack stack) {
      CompoundTag tag = stack.getOrCreateTag();
      tag.putBoolean("IsLoliBlade", true);
      tag.putString("ModelName", BLADE_MODEL.toString());
      tag.putString("TextureName", BLADE_TEXTURE.toString());
      tag.putString("SlashArts", LOLI_SLASH_ART.toString());
      tag.putInt("SummonedSwordColor", SUMMONED_SWORD_COLOR);
      tag.putBoolean("Unbreakable", true);
   }

   public static boolean isLoliBlade(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      if (DESCRIPTION_ID.equals(stack.getDescriptionId())) {
         return true;
      }

      CompoundTag tag = stack.getTag();
      return tag != null && tag.getBoolean("IsLoliBlade");
   }

   /** 首次进入玩家物品栏时绑定主人，NBT 会随物品复制和分裂保留。 */
   public static void bindUnownedStacks(Player player) {
      refreshAndBind(player.getMainHandItem(), player.getUUID());
      refreshAndBind(player.getOffhandItem(), player.getUUID());
      for (ItemStack stack : player.getInventory().items) {
         refreshAndBind(stack, player.getUUID());
      }
   }

   public static boolean isOwnedBy(ItemStack stack, Player player) {
      return player != null && isOwnedBy(stack, player.getUUID());
   }

   public static boolean isOwnedBy(ItemStack stack, UUID playerId) {
      if (!isLoliBlade(stack) || playerId == null) {
         return false;
      }

      CompoundTag tag = stack.getTag();
      if (tag == null) {
         return false;
      }

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

   private static void bindIfUnowned(ItemStack stack, UUID playerId) {
      if (!isLoliBlade(stack) || hasOwner(stack)) {
         return;
      }

      stack.getOrCreateTag().putUUID(OWNER_TAG, playerId);
   }

   private static void refreshAndBind(ItemStack stack, UUID playerId) {
      if (!isLoliBlade(stack)) {
         return;
      }

      ensureStats(stack);
      bindIfUnowned(stack, playerId);
   }

   private static boolean hasOwner(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && (tag.hasUUID(OWNER_TAG) || !tag.getString(OWNER_TAG).isEmpty());
   }

   private static void applyBladeState(ItemStack stack) {
      stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
         state.setTranslationKey(DESCRIPTION_ID);
         state.setSlashArtsKey(LOLI_SLASH_ART);
         state.setBaseAttackModifier(4.0F);
         state.setMaxDamage(1000);
         state.setDefaultBewitched(true);
         state.setModel(BLADE_MODEL);
         state.setTexture(BLADE_TEXTURE);
         state.setColorCode(SUMMONED_SWORD_COLOR);
         ListTag specialEffects = new ListTag();
         specialEffects.add(StringTag.valueOf(LOLI_FACING_EXECUTION));
         state.setSpecialEffects(specialEffects);
         state.setBroken(false);
         stack.getOrCreateTag().put("bladeState", state.serializeNBT());
      });
   }

   private static void applyEnchantments(ItemStack stack) {
      Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
      putMaxEnchant(enchantments, Enchantments.UNBREAKING, 10);
      putMaxEnchant(enchantments, Enchantments.MOB_LOOTING, 10);
      putMaxEnchant(enchantments, Enchantments.MULTISHOT, 10);
      putMaxEnchant(enchantments, Enchantments.POWER_ARROWS, 10);
      putMaxEnchant(enchantments, Enchantments.SOUL_SPEED, 10);
      putMaxEnchant(enchantments, Enchantments.FALL_PROTECTION, 10);
      putMaxEnchant(enchantments, Enchantments.ALL_DAMAGE_PROTECTION, 10);
      putMaxEnchant(enchantments, Enchantments.KNOCKBACK, 10);
      putMaxEnchant(enchantments, Enchantments.MENDING, 10);
      EnchantmentHelper.setEnchantments(enchantments, stack);
   }

   private static void putMaxEnchant(Map<Enchantment, Integer> enchantments, Enchantment enchantment, int level) {
      enchantments.put(enchantment, Math.max(level, enchantments.getOrDefault(enchantment, 0)));
   }
}
