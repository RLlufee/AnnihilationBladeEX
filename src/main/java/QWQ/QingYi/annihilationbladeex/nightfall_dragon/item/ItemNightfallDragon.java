package QWQ.QingYi.annihilationbladeex.nightfall_dragon.item;

import QWQ.QingYi.annihilationbladeex.nightfall_dragon.NightfallDragonDefinitions;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ItemNightfallDragon extends ItemSlashBlade {
   public ItemNightfallDragon() {
      super(Tiers.NETHERITE, 100, -2.4F, new Item.Properties().fireResistant().stacksTo(1));
   }

   @Override
   public boolean isDamageable(@NotNull ItemStack stack) {
      return false;
   }

   @Override
   public @NotNull ItemStack getDefaultInstance() {
      return NightfallDragonDefinitions.createStack();
   }

   @Override
   public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
      super.inventoryTick(stack, level, entity, slotId, isSelected);
      if (!level.isClientSide && entity.tickCount % 20 == 0) {
         NightfallDragonDefinitions.ensureStats(stack, level);
      }
   }

   @Override
   public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entityLiving, int timeLeft) {
      NightfallDragonDefinitions.ensureStats(stack, entityLiving.level());
      super.releaseUsing(stack, level, entityLiving, timeLeft);
   }
}
