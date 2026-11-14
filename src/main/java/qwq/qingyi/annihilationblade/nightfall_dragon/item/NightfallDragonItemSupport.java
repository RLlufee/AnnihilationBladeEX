package qwq.qingyi.annihilationblade.nightfall_dragon.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import qwq.qingyi.annihilationblade.common.SlashBladeStateSupport;
import qwq.qingyi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;

public final class NightfallDragonItemSupport {
    private NightfallDragonItemSupport() {
    }

    public static boolean isNightfallDragon(ItemStack stack) {
        if (!SlashBladeStateSupport.isSlashBlade(stack)) {
            return false;
        }
        if (NightfallDragonDefinitions.isNightfallDragon(stack)) {
            return true;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.copyTag().getBooleanOr(NightfallDragonDefinitions.IDENTITY_TAG, false);
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
