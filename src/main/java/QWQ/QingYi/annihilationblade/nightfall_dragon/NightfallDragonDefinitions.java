package QWQ.QingYi.annihilationblade.nightfall_dragon;

import QWQ.QingYi.annihilationblade.common.NamedBladeStacks;
import QWQ.QingYi.annihilationblade.registry.ModSpecialEffects;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import mods.flammpfeil.slashblade.init.SBItems;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

public final class NightfallDragonDefinitions {
   public static final String NAME = "nightfall_dragon";
   public static final String DESCRIPTION_ID = "item.annihilationblade.nightfall_dragon";
   public static final String AWAKENED_DESCRIPTION_ID = "item.annihilationblade.nightfall_dragon.awakened";
   public static final String FINAL_DESCRIPTION_ID = "item.annihilationblade.nightfall_dragon.final";
   public static final String IDENTITY_TAG = "AnnihilationBladeNightfallDragon";
   public static final String FORM_TAG = "NightfallDragonForm";
   public static final String FORM_SEALED = "sealed";
   public static final String FORM_AWAKENED = "awakened";
   public static final String FORM_FINAL = "final";
   public static final ResourceLocation BLADE_MODEL = ResourceLocation.fromNamespaceAndPath("annihilationblade", "model/nightfall_dragon.obj");
   public static final ResourceLocation BLADE_TEXTURE = ResourceLocation.fromNamespaceAndPath("annihilationblade", "model/nightfall_dragon.png");
   public static final ResourceLocation HALO_TEXTURE = ResourceLocation.fromNamespaceAndPath("annihilationblade", "model/nightfall_dragon_halo.png");
   private static final ResourceLocation EMPTY_SLASH_ART = ResourceLocation.fromNamespaceAndPath("slashblade", "none");
   private static final ResourceLocation SEALED_SLASH_ART = ResourceLocation.fromNamespaceAndPath("annihilationblade", "nightfall_judgement_cut");
   private static final ResourceLocation AWAKENED_SLASH_ART = ResourceLocation.fromNamespaceAndPath("annihilationblade", "dragon_head_charge");
   public static final int SEALED_SUMMONED_SWORD_COLOR = 11158783;
   public static final int AWAKENED_SUMMONED_SWORD_COLOR = 16443135;
   public static final int FINAL_SUMMONED_SWORD_COLOR = 13938487;
   public static final int FINAL_VOID_PURPLE = 11544319;
   private static final Set<String> FORM_TRANSLATION_KEYS = Set.of(DESCRIPTION_ID, AWAKENED_DESCRIPTION_ID, FINAL_DESCRIPTION_ID);

   private NightfallDragonDefinitions() {
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
         applyRuntimeStats(stack);
      }
   }

   public static void ensureStats(ItemStack stack, @Nullable Level level) {
      if (!stack.isEmpty()) {
         String form = getForm(stack);
         NamedBladeStacks.copyDefinitionTag(stack, level, NAME, null);
         applyRuntimeStats(stack, form);
      }
   }

   public static boolean isNightfallDragon(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      boolean stateMatch = stack.getCapability(ItemSlashBlade.BLADESTATE).map(state ->
         FORM_TRANSLATION_KEYS.contains(state.getTranslationKey())
            || state.getModel().map(BLADE_MODEL::equals).orElse(false)
            || state.getTexture().map(BLADE_TEXTURE::equals).orElse(false)
      ).orElse(false);
      if (stateMatch) {
         return true;
      }

      CompoundTag tag = stack.getTag();
      return tag != null
         && (BLADE_MODEL.toString().equals(tag.getString("ModelName"))
            || BLADE_TEXTURE.toString().equals(tag.getString("TextureName")));
   }

   private static void applyFallbackStats(ItemStack stack) {
      if (!stack.isEmpty()) {
         CompoundTag tag = stack.getOrCreateTag();
         tag.putString("SlashArts", SEALED_SLASH_ART.toString());
         tag.putBoolean("Unbreakable", true);
         tag.putString(FORM_TAG, FORM_SEALED);
         applyRuntimeStats(stack);
      }
   }

   private static void applyRuntimeStats(ItemStack stack) {
      applyRuntimeStats(stack, getForm(stack));
   }

   public static String getForm(ItemStack stack) {
      if (stack.isEmpty()) {
         return FORM_SEALED;
      }

      CompoundTag tag = stack.getTag();
      if (tag != null) {
         String form = tag.getString(FORM_TAG);
         if (FORM_FINAL.equals(form)) {
            return FORM_FINAL;
         }

         if (FORM_AWAKENED.equals(form)) {
            return FORM_AWAKENED;
         }
      }

      return stack.getCapability(ItemSlashBlade.BLADESTATE)
         .map(state -> {
            if (FINAL_DESCRIPTION_ID.equals(state.getTranslationKey())) {
               return FORM_FINAL;
            }

            return AWAKENED_DESCRIPTION_ID.equals(state.getTranslationKey()) ? FORM_AWAKENED : FORM_SEALED;
         })
         .orElse(FORM_SEALED);
   }

   public static boolean isAwakened(ItemStack stack) {
      return FORM_AWAKENED.equals(getForm(stack));
   }

   public static boolean isFinal(ItemStack stack) {
      return FORM_FINAL.equals(getForm(stack));
   }

   public static void applyForm(ItemStack stack, String form) {
      if (!stack.isEmpty()) {
         applyRuntimeStats(stack, normalizeForm(form));
      }
   }

   public static String toggleForm(ItemStack stack) {
      String current = getForm(stack);
      String next = FORM_SEALED;
      if (FORM_SEALED.equals(current)) {
         next = FORM_AWAKENED;
      } else if (FORM_AWAKENED.equals(current)) {
         next = FORM_FINAL;
      }

      applyForm(stack, next);
      return next;
   }

   private static void applyRuntimeStats(ItemStack stack, String form) {
      CompoundTag tag = stack.getOrCreateTag();
      String normalizedForm = normalizeForm(form);
      boolean awakened = FORM_AWAKENED.equals(normalizedForm);
      boolean finalForm = FORM_FINAL.equals(normalizedForm);
      ResourceLocation slashArt = finalForm ? EMPTY_SLASH_ART : awakened ? AWAKENED_SLASH_ART : SEALED_SLASH_ART;
      tag.putString("ModelName", BLADE_MODEL.toString());
      tag.putString("TextureName", BLADE_TEXTURE.toString());
      tag.putString("SlashArts", slashArt.toString());
      tag.putInt("SummonedSwordColor", finalForm ? FINAL_SUMMONED_SWORD_COLOR : awakened ? AWAKENED_SUMMONED_SWORD_COLOR : SEALED_SUMMONED_SWORD_COLOR);
      tag.putBoolean("Unbreakable", true);
      tag.putBoolean(IDENTITY_TAG, true);
      tag.putString(FORM_TAG, normalizedForm);
      ensureNightfallEnchantments(stack);
      stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
         state.setTranslationKey(finalForm ? FINAL_DESCRIPTION_ID : awakened ? AWAKENED_DESCRIPTION_ID : DESCRIPTION_ID);
         state.setSlashArtsKey(slashArt);
         state.setBaseAttackModifier(22.0F);
         state.setMaxDamage(2400);
         state.setDefaultBewitched(true);
         state.setModel(BLADE_MODEL);
         state.setTexture(BLADE_TEXTURE);
         state.setColorCode(finalForm ? FINAL_SUMMONED_SWORD_COLOR : awakened ? AWAKENED_SUMMONED_SWORD_COLOR : SEALED_SUMMONED_SWORD_COLOR);
         state.setSpecialEffects(createSpecialEffects(normalizedForm));
         state.setBroken(false);
         tag.put("bladeState", state.serializeNBT());
      });
   }

   private static void ensureNightfallEnchantments(ItemStack stack) {
      Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
      enchantments.put(Enchantments.UNBREAKING, Math.max(10, enchantments.getOrDefault(Enchantments.UNBREAKING, 0)));
      EnchantmentHelper.setEnchantments(enchantments, stack);
   }

   private static String normalizeForm(String form) {
      if (FORM_FINAL.equals(form)) {
         return FORM_FINAL;
      }

      return FORM_AWAKENED.equals(form) ? FORM_AWAKENED : FORM_SEALED;
   }

   private static ListTag createSpecialEffects(String form) {
      ListTag specialEffects = new ListTag();
      if (FORM_FINAL.equals(form)) {
         addFinalInheritedEffects(specialEffects);
         specialEffects.add(StringTag.valueOf(ModSpecialEffects.DRAGON_GOD_BODY.getId().toString()));
         specialEffects.add(StringTag.valueOf(ModSpecialEffects.ABSOLUTE_ANNIHILATION_DOMAIN.getId().toString()));
         specialEffects.add(StringTag.valueOf(ModSpecialEffects.MYRIAD_DRAGON_BLADE_STORM.getId().toString()));
         specialEffects.add(StringTag.valueOf(ModSpecialEffects.WORLD_CLEAVING_SLASH.getId().toString()));
      } else if (FORM_AWAKENED.equals(form)) {
         addAwakenedEffects(specialEffects);
      } else {
         addSealedEffects(specialEffects);
      }

      return specialEffects;
   }

   private static void addSealedEffects(ListTag specialEffects) {
      specialEffects.add(StringTag.valueOf(ModSpecialEffects.DEMONIC_BLOOD_PARASITE.getId().toString()));
      specialEffects.add(StringTag.valueOf(ModSpecialEffects.OUTER_GOD_SCAR.getId().toString()));
   }

   private static void addAwakenedEffects(ListTag specialEffects) {
      addSealedEffects(specialEffects);
      specialEffects.add(StringTag.valueOf(ModSpecialEffects.DRAGON_PRESSURE_DOMAIN.getId().toString()));
      specialEffects.add(StringTag.valueOf(ModSpecialEffects.REVERSE_SCALE_HUNT.getId().toString()));
   }

   private static void addFinalInheritedEffects(ListTag specialEffects) {
      specialEffects.add(StringTag.valueOf(ModSpecialEffects.DEMONIC_BLOOD_PARASITE.getId().toString()));
      specialEffects.add(StringTag.valueOf(ModSpecialEffects.DRAGON_PRESSURE_DOMAIN.getId().toString()));
   }
}
