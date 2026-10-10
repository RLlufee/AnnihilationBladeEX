package QWQ.QingYi.annihilationbladeex.infinity_stellaris;

import QWQ.QingYi.annihilationbladeex.common.NamedBladeStacks;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateData;
import mods.flammpfeil.slashblade.capability.slashblade.SlashBladeDataComponents;
import mods.flammpfeil.slashblade.client.renderer.CarryType;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class InfinityStellarisDefinitions {
   public static final String NAME = "infinity_stellaris";
   public static final String DESCRIPTION_ID = "item.annihilationbladeex.infinity_stellaris";
   private static final ResourceLocation COMBO_ROOT_ID = ResourceLocation.fromNamespaceAndPath("slashblade", "standby");
   private static final ResourceLocation BLADE_MODEL = ResourceLocation.fromNamespaceAndPath("annihilationbladeex", "model/infinity_stellaris.obj");
   private static final ResourceLocation BLADE_TEXTURE = ResourceLocation.fromNamespaceAndPath("annihilationbladeex", "model/infinity_stellaris.png");
   private static final ResourceLocation VACUUM_DECAY_COLLAPSE = ResourceLocation.fromNamespaceAndPath("annihilationbladeex", "vacuum_decay_collapse");
   private static final int SUMMONED_SWORD_COLOR = 4248287;
   private static final List<ResourceLocation> SPECIAL_EFFECTS = List.of(
      ResourceLocation.fromNamespaceAndPath("annihilationbladeex", "entropy_dissolution"),
      ResourceLocation.fromNamespaceAndPath("annihilationbladeex", "curvature_rupture"),
      ResourceLocation.fromNamespaceAndPath("annihilationbladeex", "gamma_thunderburst"),
      ResourceLocation.fromNamespaceAndPath("annihilationbladeex", "cosmic_string_cut")
   );

   private InfinityStellarisDefinitions() {
   }

   public static ItemStack createStack() {
      return createStack(null);
   }

   public static ItemStack createStack(@Nullable Level level) {
      var slashbladeItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("slashblade", "slashblade"));
      ItemStack datapackStack = NamedBladeStacks.get(level, NAME, slashbladeItem);
      if (!datapackStack.isEmpty()) {
         ensureStats(datapackStack, level);
         return datapackStack;
      }

      ItemStack stack = new ItemStack(slashbladeItem);
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
      var slashbladeItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("slashblade", "slashblade"));
      if (registries != null) {
         var lookup = SlashBlade.getSlashBladeDefinitionRegistry(registries);
         var key = ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath("annihilationbladeex", NAME));
         Optional<ItemStack> defined = lookup.get(key).map(holder -> holder.value().getBlade(slashbladeItem, registries));
         if (defined.isPresent()) {
            ItemStack stack = defined.get();
            applyRuntimeStats(stack);
            return stack;
         }
      }
      ItemStack stack = new ItemStack(slashbladeItem);
      applyStats(stack, registries);
      return stack;
   }

   /** 已有武器的属性维护不补齐、降级或覆盖附魔。 */
   private static void applyRuntimeStats(ItemStack stack) {
      if (stack.isEmpty()) {
         return;
      }

      BladeStateData current = BladeStateAccess.getDataOrDefault(stack);
      int proudSoul = current.proudSoul();
      int killCount = current.killCount();
      int refine = current.refine();

      stack.set(DataComponents.MAX_DAMAGE, 2000);
      stack.setDamageValue(0);
      stack.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
      stack.set(DataComponents.RARITY, Rarity.EPIC);
      stack.set(SlashBladeDataComponents.BLADE_STATE_DATA.get(), new BladeStateData(
         DESCRIPTION_ID,
         1000000.0F,
         proudSoul,
         killCount,
         refine,
         false,
         false,
         VACUUM_DECAY_COLLAPSE,
         true,
         false, // destructable（2.0.7 新增）：断裂时不销毁物品
         COMBO_ROOT_ID,
         CarryType.NINJA,
         SUMMONED_SWORD_COLOR,
         false,
         Vec3.ZERO,
         Optional.of(BLADE_TEXTURE),
         Optional.of(BLADE_MODEL),
         SPECIAL_EFFECTS
      ));
      BladeStateAccess.ensureRuntimeComponent(stack);
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
