package QWQ.QingYi.annihilationbladeex.infinity_stellaris.item;

import QWQ.QingYi.annihilationbladeex.infinity_stellaris.InfinityStellarisDefinitions;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class InfinityStellarisItemSupport {
   private static final ResourceLocation ENTROPY_DISSOLUTION = ResourceLocation.fromNamespaceAndPath("annihilationbladeex", "entropy_dissolution");
   private static final ResourceLocation CURVATURE_RUPTURE = ResourceLocation.fromNamespaceAndPath("annihilationbladeex", "curvature_rupture");
   private static final ResourceLocation GAMMA_THUNDERBURST = ResourceLocation.fromNamespaceAndPath("annihilationbladeex", "gamma_thunderburst");
   private static final ResourceLocation COSMIC_STRING_CUT = ResourceLocation.fromNamespaceAndPath("annihilationbladeex", "cosmic_string_cut");

   private InfinityStellarisItemSupport() {
   }

   public static boolean isInfinityStellaris(ItemStack stack) {
      if (stack.isEmpty() || !(stack.getItem() instanceof ItemSlashBlade)) {
         return false;
      }
      return InfinityStellarisDefinitions.DESCRIPTION_ID.equals(stack.getDescriptionId())
         || mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess.getData(stack)
            .map(data -> InfinityStellarisDefinitions.DESCRIPTION_ID.equals(data.translationKey()) || hasInfinitySpecialEffects(data.specialEffects()))
            .orElse(false);
   }

   private static boolean hasInfinitySpecialEffects(java.util.Collection<ResourceLocation> specialEffects) {
      return specialEffects.contains(ENTROPY_DISSOLUTION)
         && specialEffects.contains(CURVATURE_RUPTURE)
         && specialEffects.contains(GAMMA_THUNDERBURST)
         && specialEffects.contains(COSMIC_STRING_CUT);
   }

   public static boolean isHoldingInfinityStellaris(LivingEntity entity) {
      return isInfinityStellaris(entity.getMainHandItem()) || isInfinityStellaris(entity.getOffhandItem());
   }

   public static boolean hasInfinityStellarisInInventory(Player player) {
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         if (isInfinityStellaris(player.getInventory().getItem(i))) {
            return true;
         }
      }
      return false;
   }

   public static ItemStack heldInfinityStellaris(LivingEntity entity) {
      ItemStack mainHand = entity.getMainHandItem();
      if (isInfinityStellaris(mainHand)) {
         return mainHand;
      }

      ItemStack offHand = entity.getOffhandItem();
      return isInfinityStellaris(offHand) ? offHand : ItemStack.EMPTY;
   }

   public static boolean isDirectInfinityAttack(Player player, net.minecraft.world.damagesource.DamageSource source) {
      return isHoldingInfinityStellaris(player) && source.getEntity() == player;
   }

   public static boolean isInfinitySlashEntityAttack(Player player, net.minecraft.world.entity.Entity directSource) {
      return directSource != null && directSource.getType().toString().contains("slashblade") && isHoldingInfinityStellaris(player);
   }
}
