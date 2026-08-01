package QWQ.QingYi.annihilationblade.nightfall_dragon;

import QWQ.QingYi.annihilationblade.common.NamedBladeStacks;
import QWQ.QingYi.annihilationblade.registry.ModSpecialEffects;
import java.util.Set;
import javax.annotation.Nullable;
import mods.flammpfeil.slashblade.init.SBItems;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class NightfallDragonDefinitions {
   public static final String NAME = "nightfall_dragon";
   public static final String DESCRIPTION_ID = "item.annihilationblade.nightfall_dragon";
   public static final String AWAKENED_DESCRIPTION_ID = "item.annihilationblade.nightfall_dragon.awakened";
   public static final String IDENTITY_TAG = "AnnihilationBladeNightfallDragon";
   public static final String FORM_TAG = "NightfallDragonForm";
   public static final String FORM_SEALED = "sealed";
   public static final String FORM_AWAKENED = "awakened";
   public static final ResourceLocation BLADE_MODEL = ResourceLocation.fromNamespaceAndPath("annihilationblade", "model/nightfall_dragon.obj");
   public static final ResourceLocation BLADE_TEXTURE = ResourceLocation.fromNamespaceAndPath("annihilationblade", "model/nightfall_dragon.png");
   public static final ResourceLocation HALO_TEXTURE = ResourceLocation.fromNamespaceAndPath("annihilationblade", "model/nightfall_dragon_halo.png");
   private static final ResourceLocation EMPTY_SLASH_ART = ResourceLocation.fromNamespaceAndPath("slashblade", "none");
   public static final int SEALED_SUMMONED_SWORD_COLOR = 11158783;
   public static final int AWAKENED_SUMMONED_SWORD_COLOR = 16443135;
   private static final Set<String> FORM_TRANSLATION_KEYS = Set.of(DESCRIPTION_ID, AWAKENED_DESCRIPTION_ID);

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
         tag.putString("SlashArts", EMPTY_SLASH_ART.toString());
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
         if (FORM_AWAKENED.equals(form)) {
            return FORM_AWAKENED;
         }
      }

      return stack.getCapability(ItemSlashBlade.BLADESTATE)
         .map(state -> AWAKENED_DESCRIPTION_ID.equals(state.getTranslationKey()) ? FORM_AWAKENED : FORM_SEALED)
         .orElse(FORM_SEALED);
   }

   public static boolean isAwakened(ItemStack stack) {
      return FORM_AWAKENED.equals(getForm(stack));
   }

   public static void applyForm(ItemStack stack, String form) {
      if (!stack.isEmpty()) {
         applyRuntimeStats(stack, FORM_AWAKENED.equals(form) ? FORM_AWAKENED : FORM_SEALED);
      }
   }

   public static String toggleForm(ItemStack stack) {
      String next = isAwakened(stack) ? FORM_SEALED : FORM_AWAKENED;
      applyForm(stack, next);
      return next;
   }

   private static void applyRuntimeStats(ItemStack stack, String form) {
      CompoundTag tag = stack.getOrCreateTag();
      boolean awakened = FORM_AWAKENED.equals(form);
      tag.putString("ModelName", BLADE_MODEL.toString());
      tag.putString("TextureName", BLADE_TEXTURE.toString());
      tag.putInt("SummonedSwordColor", awakened ? AWAKENED_SUMMONED_SWORD_COLOR : SEALED_SUMMONED_SWORD_COLOR);
      tag.putBoolean("Unbreakable", true);
      tag.putBoolean(IDENTITY_TAG, true);
      tag.putString(FORM_TAG, awakened ? FORM_AWAKENED : FORM_SEALED);
      stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
         state.setTranslationKey(awakened ? AWAKENED_DESCRIPTION_ID : DESCRIPTION_ID);
         state.setSlashArtsKey(EMPTY_SLASH_ART);
         state.setBaseAttackModifier(22.0F);
         state.setMaxDamage(2400);
         state.setDefaultBewitched(true);
         state.setModel(BLADE_MODEL);
         state.setTexture(BLADE_TEXTURE);
         state.setColorCode(awakened ? AWAKENED_SUMMONED_SWORD_COLOR : SEALED_SUMMONED_SWORD_COLOR);
         state.setSpecialEffects(createSpecialEffects(awakened));
         state.setBroken(false);
         tag.put("bladeState", state.serializeNBT());
      });
   }

   private static ListTag createSpecialEffects(boolean awakened) {
      ListTag specialEffects = new ListTag();
      if (awakened) {
         specialEffects.add(StringTag.valueOf(ModSpecialEffects.DRAGON_PRESSURE_DOMAIN.getId().toString()));
         specialEffects.add(StringTag.valueOf(ModSpecialEffects.REVERSE_SCALE_HUNT.getId().toString()));
      } else {
         specialEffects.add(StringTag.valueOf(ModSpecialEffects.DEMONIC_BLOOD_PARASITE.getId().toString()));
         specialEffects.add(StringTag.valueOf(ModSpecialEffects.OUTER_GOD_SCAR.getId().toString()));
      }

      return specialEffects;
   }
}
