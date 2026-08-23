package QWQ.QingYi.annihilationbladeex.nightfall_dragon;

import QWQ.QingYi.annihilationbladeex.common.NamedBladeStacks;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateData;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.capability.slashblade.SlashBladeDataComponents;
import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("null")
public final class NightfallDragonDefinitions {
   private static final String MODID = "annihilationbladeex";
   public static final String NAME = "nightfall_dragon";
   public static final ResourceLocation ID = prefix(NAME);
   public static final String DESCRIPTION_ID = "item." + MODID + ".nightfall_dragon";
   public static final String AWAKENED_DESCRIPTION_ID = "item." + MODID + ".nightfall_dragon.awakened";
   public static final String FINAL_DESCRIPTION_ID = "item." + MODID + ".nightfall_dragon.final";
   public static final String IDENTITY_TAG = "AnnihilationBladeNightfallDragon";
   public static final String FORM_TAG = "NightfallDragonForm";
   public static final String FORM_SEALED = "sealed";
   public static final String FORM_AWAKENED = "awakened";
   public static final String FORM_FINAL = "final";
   public static final ResourceLocation BLADE_MODEL = prefix("model/nightfall_dragon.obj");
   public static final ResourceLocation BLADE_TEXTURE = prefix("model/nightfall_dragon.png");
   public static final ResourceLocation HALO_TEXTURE = prefix("model/nightfall_dragon_halo.png");
   private static final ResourceLocation COMBO_ROOT_ID = ResourceLocation.fromNamespaceAndPath("slashblade", "standby");
   private static final ResourceLocation SEALED_SLASH_ART = prefix("nightfall_judgement_cut");
   private static final ResourceLocation AWAKENED_SLASH_ART = prefix("scale_guard");
   private static final ResourceLocation FINAL_SLASH_ART = prefix("cosmic_nightfall_descent");
   public static final float BASE_ATTACK_DAMAGE = 22.0F;
   public static final int SEALED_SUMMONED_SWORD_COLOR = 11158783;
   public static final int AWAKENED_SUMMONED_SWORD_COLOR = 16443135;
   public static final int FINAL_SUMMONED_SWORD_COLOR = 13938487;
   public static final int FINAL_VOID_PURPLE = 11544319;
   private static final Set<String> FORM_TRANSLATION_KEYS = Set.of(DESCRIPTION_ID, AWAKENED_DESCRIPTION_ID, FINAL_DESCRIPTION_ID);

   private NightfallDragonDefinitions() {
   }

   private static ResourceLocation prefix(String path) {
      return ResourceLocation.fromNamespaceAndPath(MODID, path);
   }

   private static Item bladeItem() {
      return BuiltInRegistries.ITEM.get(ID);
   }

   public static ItemStack createStack() {
      return createStack(null);
   }

   public static ItemStack createStack(@Nullable Level level) {
      if (level != null) {
         ItemStack datapackStack = NamedBladeStacks.get(level, NAME, bladeItem());
         if (!datapackStack.isEmpty()) {
            ensureStats(datapackStack, level);
            return datapackStack;
         }
      }

      ItemStack stack = new ItemStack(bladeItem());
      applyFallbackStats(stack, null);
      return stack;
   }

   public static void ensureStats(ItemStack stack) {
      ensureStats(stack, (HolderLookup.Provider)null);
   }

   public static void ensureStats(ItemStack stack, @Nullable Level level) {
      ensureStats(stack, level == null ? null : level.registryAccess());
   }

   public static void ensureStats(ItemStack stack, @Nullable HolderLookup.Provider registries) {
      if (!stack.isEmpty()) {
         String form = getForm(stack);
         applyRuntimeStats(stack, form, registries);
      }
   }

   public static boolean isNightfallDragon(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      boolean stateMatch = BladeStateAccess.of(stack).map(state ->
         FORM_TRANSLATION_KEYS.contains(state.getTranslationKey())
            || state.getModel().map(BLADE_MODEL::equals).orElse(false)
            || state.getTexture().map(BLADE_TEXTURE::equals).orElse(false)
      ).orElse(false);
      if (stateMatch) {
         return true;
      }

      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      if (customData != null) {
         CompoundTag tag = customData.copyTag();
         return tag.getBoolean(IDENTITY_TAG)
            || BLADE_MODEL.toString().equals(tag.getString("ModelName"))
            || BLADE_TEXTURE.toString().equals(tag.getString("TextureName"));
      }

      return false;
   }

   private static void applyFallbackStats(ItemStack stack, @Nullable HolderLookup.Provider registries) {
      if (!stack.isEmpty()) {
         applyRuntimeStats(stack, FORM_SEALED, registries);
      }
   }

   public static String getForm(ItemStack stack) {
      if (stack.isEmpty()) {
         return FORM_SEALED;
      }

      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      if (customData != null) {
         CompoundTag tag = customData.copyTag();
         String form = tag.getString(FORM_TAG);
         if (FORM_FINAL.equals(form)) {
            return FORM_FINAL;
         }
         if (FORM_AWAKENED.equals(form)) {
            return FORM_AWAKENED;
         }
      }

      return BladeStateAccess.of(stack)
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
      applyForm(stack, form, null);
   }

   public static void applyForm(ItemStack stack, String form, @Nullable HolderLookup.Provider registries) {
      if (!stack.isEmpty()) {
         applyRuntimeStats(stack, normalizeForm(form), registries);
      }
   }

   public static String toggleForm(ItemStack stack) {
      return toggleForm(stack, null);
   }

   public static String toggleForm(ItemStack stack, @Nullable HolderLookup.Provider registries) {
      String current = getForm(stack);
      String next = FORM_SEALED;
      if (FORM_SEALED.equals(current)) {
         next = FORM_AWAKENED;
      } else if (FORM_AWAKENED.equals(current)) {
         next = FORM_FINAL;
      }

      applyForm(stack, next, registries);
      return next;
   }

   private static void applyRuntimeStats(ItemStack stack, String form, @Nullable HolderLookup.Provider registries) {
      String normalizedForm = normalizeForm(form);
      boolean awakened = FORM_AWAKENED.equals(normalizedForm);
      boolean finalForm = FORM_FINAL.equals(normalizedForm);
      ResourceLocation slashArt = finalForm ? FINAL_SLASH_ART : awakened ? AWAKENED_SLASH_ART : SEALED_SLASH_ART;
      int color = finalForm ? FINAL_SUMMONED_SWORD_COLOR : awakened ? AWAKENED_SUMMONED_SWORD_COLOR : SEALED_SUMMONED_SWORD_COLOR;
      String descriptionId = finalForm ? FINAL_DESCRIPTION_ID : awakened ? AWAKENED_DESCRIPTION_ID : DESCRIPTION_ID;

      CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
         tag.putString("ModelName", BLADE_MODEL.toString());
         tag.putString("TextureName", BLADE_TEXTURE.toString());
         tag.putString("SlashArts", slashArt.toString());
         tag.putInt("SummonedSwordColor", color);
         tag.putBoolean("Unbreakable", true);
         tag.putBoolean(IDENTITY_TAG, true);
         tag.putString(FORM_TAG, normalizedForm);
      });

      stack.set(DataComponents.MAX_DAMAGE, 2400);
      stack.setDamageValue(0);
      stack.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
      stack.set(DataComponents.RARITY, Rarity.EPIC);

      int proudSoul = BladeStateAccess.of(stack).map(ISlashBladeState::getProudSoulCount).orElse(0);
      int killCount = BladeStateAccess.of(stack).map(ISlashBladeState::getKillCount).orElse(0);
      int refine = BladeStateAccess.of(stack).map(ISlashBladeState::getRefine).orElse(0);

      stack.set(SlashBladeDataComponents.BLADE_STATE_DATA.get(), new BladeStateData(
         descriptionId,
         BASE_ATTACK_DAMAGE,
         proudSoul,
         killCount,
         refine,
         false,
         false,
         slashArt,
         true,
         COMBO_ROOT_ID,
         CarryType.NINJA,
         color,
         false,
         Vec3.ZERO,
         Optional.of(BLADE_TEXTURE),
         Optional.of(BLADE_MODEL),
         createSpecialEffects(normalizedForm)
      ));
      BladeStateAccess.ensureRuntimeComponent(stack);
      applyEnchantments(stack, registries);
   }

   private static void applyEnchantments(ItemStack stack, @Nullable HolderLookup.Provider registries) {
      if (registries == null) {
         return;
      }
      var enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
      stack.enchant(enchantments.getOrThrow(Enchantments.SHARPNESS), 10);
      stack.enchant(enchantments.getOrThrow(Enchantments.POWER), 10);
      stack.enchant(enchantments.getOrThrow(Enchantments.INFINITY), 10);
      stack.enchant(enchantments.getOrThrow(Enchantments.MENDING), 10);
      stack.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 10);
      stack.enchant(enchantments.getOrThrow(Enchantments.LOOTING), 10);
      stack.enchant(enchantments.getOrThrow(Enchantments.MULTISHOT), 10);
      stack.enchant(enchantments.getOrThrow(Enchantments.FEATHER_FALLING), 10);
      stack.enchant(enchantments.getOrThrow(Enchantments.SWEEPING_EDGE), 10);
   }

   public static ItemStack createFromLookup(HolderLookup.Provider registries) {
      if (registries != null) {
         var lookup = SlashBlade.getSlashBladeDefinitionRegistry(registries);
         var key = ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY, ID);
         Optional<ItemStack> defined = lookup.get(key).map(holder -> holder.value().getBlade(bladeItem(), registries));
         if (defined.isPresent()) {
            ItemStack stack = defined.get();
            applyRuntimeStats(stack, FORM_SEALED, registries);
            return stack;
         }
      }
      ItemStack stack = createStack();
      applyEnchantments(stack, registries);
      return stack;
   }

   private static String normalizeForm(String form) {
      if (FORM_FINAL.equals(form)) {
         return FORM_FINAL;
      }
      return FORM_AWAKENED.equals(form) ? FORM_AWAKENED : FORM_SEALED;
   }

   private static List<ResourceLocation> createSpecialEffects(String form) {
      List<ResourceLocation> list = new ArrayList<>();
      if (FORM_FINAL.equals(form)) {
         list.add(prefix("demonic_blood_parasite"));
         list.add(prefix("dragon_pressure_domain"));
         list.add(prefix("dragon_god_body"));
         list.add(prefix("absolute_annihilation_domain"));
         list.add(prefix("myriad_dragon_blade_storm"));
         list.add(prefix("world_cleaving_slash"));
      } else if (FORM_AWAKENED.equals(form)) {
         list.add(prefix("demonic_blood_parasite"));
         list.add(prefix("outer_god_scar"));
         list.add(prefix("dragon_pressure_domain"));
         list.add(prefix("reverse_scale_hunt"));
      } else {
         list.add(prefix("demonic_blood_parasite"));
         list.add(prefix("outer_god_scar"));
      }
      return list;
   }
}
