package qwq.qingyi.annihilationblade.infinity_stellaris.item;

import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.common.SlashBladeStateSupport;
import qwq.qingyi.annihilationblade.infinity_stellaris.InfinityStellarisDefinitions;

import java.util.Collection;

public final class InfinityStellarisItemSupport {
    private static final Identifier ENTROPY_DISSOLUTION = AnnihilationBladeex.prefix("entropy_dissolution");
    private static final Identifier CURVATURE_RUPTURE = AnnihilationBladeex.prefix("curvature_rupture");
    private static final Identifier GAMMA_THUNDERBURST = AnnihilationBladeex.prefix("gamma_thunderburst");
    private static final Identifier COSMIC_STRING_CUT = AnnihilationBladeex.prefix("cosmic_string_cut");

    private InfinityStellarisItemSupport() {
    }

    public static boolean isInfinityStellaris(ItemStack stack) {
        if (!SlashBladeStateSupport.isSlashBlade(stack)) {
            return false;
        }
        return InfinityStellarisDefinitions.DESCRIPTION_ID.equals(SlashBladeStateSupport.translationKey(stack))
                || SlashBladeStateSupport.state(stack)
                .map(state -> InfinityStellarisDefinitions.DESCRIPTION_ID.equals(state.getTranslationKey())
                        || hasInfinitySpecialEffects(state.getSpecialEffects()))
                .orElse(false);
    }

    private static boolean hasInfinitySpecialEffects(Collection<Identifier> specialEffects) {
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

    public static boolean isDirectInfinityAttack(Player player, DamageSource source) {
        return isHoldingInfinityStellaris(player) && source.getEntity() == player;
    }

    public static boolean isInfinitySlashEntityAttack(Player player, Entity directSource) {
        return directSource != null && directSource.getType().toString().contains("slashblade") && isHoldingInfinityStellaris(player);
    }
}
