package QWQ.QingYi.annihilationblade.nightfall_dragon.item;

import QWQ.QingYi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class NightfallDragonItemSupport {
   private NightfallDragonItemSupport() {
   }

   public static boolean isNightfallDragon(ItemStack stack) {
      return !stack.isEmpty()
         && stack.getItem() instanceof ItemSlashBlade
         && (NightfallDragonDefinitions.isNightfallDragon(stack)
            || NightfallDragonDefinitions.DESCRIPTION_ID.equals(stack.getDescriptionId())
            || NightfallDragonDefinitions.AWAKENED_DESCRIPTION_ID.equals(stack.getDescriptionId())
            || NightfallDragonDefinitions.FINAL_DESCRIPTION_ID.equals(stack.getDescriptionId())
            || stack.hasTag() && stack.getTag().getBoolean(NightfallDragonDefinitions.IDENTITY_TAG));
   }

   public static boolean isHoldingNightfallDragon(LivingEntity entity) {
      return isNightfallDragon(entity.getMainHandItem()) || isNightfallDragon(entity.getOffhandItem());
   }

   public static boolean hasFinalNightfallDragonInInventory(Player player) {
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (isNightfallDragon(stack) && NightfallDragonDefinitions.isFinal(stack)) {
            return true;
         }
      }

      return false;
   }

   public static ItemStack finalNightfallDragonInInventory(Player player) {
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         ItemStack stack = player.getInventory().getItem(i);
         if (isNightfallDragon(stack) && NightfallDragonDefinitions.isFinal(stack)) {
            return stack;
         }
      }

      return ItemStack.EMPTY;
   }

   public static ItemStack heldNightfallDragon(LivingEntity entity) {
      ItemStack mainHand = entity.getMainHandItem();
      if (isNightfallDragon(mainHand)) {
         return mainHand;
      }

      ItemStack offHand = entity.getOffhandItem();
      return isNightfallDragon(offHand) ? offHand : ItemStack.EMPTY;
   }

   public static boolean isDirectNightfallAttack(Player player, DamageSource source) {
      return isHoldingNightfallDragon(player) && source.getEntity() == player && source.getDirectEntity() == player;
   }

   public static boolean isNightfallSlashEntityAttack(Player player, Entity directSource) {
      return directSource != null && directSource.getType().toString().contains("slashblade") && isHoldingNightfallDragon(player);
   }
}
