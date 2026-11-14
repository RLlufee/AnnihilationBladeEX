package qwq.qingyi.annihilationblade.loli_blade.specialeffect;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent.DoSlashEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.server.level.ServerPlayer;
import qwq.qingyi.annihilationblade.loli_blade.LoliBladeDefinitions;
import qwq.qingyi.annihilationblade.loli_blade.logic.LoliBladeCombatLogic;
import qwq.qingyi.annihilationblade.registry.ModSpecialEffects;

/**
 * 萝莉之刃面朝处决潜能：非潜行挥刀时触发前方扇形目标立即结算（需先于其他潜能注册监听）。
 */
public class LoliFacingExecution extends SpecialEffect {
    public LoliFacingExecution() {
        super(0, false, false);
    }

    public static void onDoingSlash(DoSlashEvent event) {
        if (!(event.getUser() instanceof ServerPlayer player)) {
            return;
        }

        if (player.isShiftKeyDown()) {
            return;
        }

        ISlashBladeState state = event.getSlashBladeState();
        if (!state.hasSpecialEffect(ModSpecialEffects.LOLI_FACING_EXECUTION_ID)) {
            return;
        }

        if (!LoliBladeDefinitions.isOwnedBy(player.getMainHandItem(), player)) {
            return;
        }

        LoliBladeCombatLogic.handleFacingSpecialEffect(player);
    }
}
