package qwq.qingyi.annihilationblade.event;

import cn.sh1rocu.slashblade.api.event.LivingExperienceDropEvent;
import cn.sh1rocu.slashblade.api.event.PlayerTickEvent;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.AbyssalDecree;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.CausalityCollapse;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.Dankong;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.PhantomJudgement;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.StarlessJudgement;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.TerminusEcho;
import qwq.qingyi.annihilationblade.annihilation_blade.specialeffect.VoidDominion;
import qwq.qingyi.annihilationblade.blood_prison.logic.BloodPrisonLogic;
import qwq.qingyi.annihilationblade.infinity_stellaris.logic.CosmicStringCutLogic;
import qwq.qingyi.annihilationblade.infinity_stellaris.logic.CurvatureRuptureLogic;
import qwq.qingyi.annihilationblade.infinity_stellaris.logic.EntropyDissolutionLogic;
import qwq.qingyi.annihilationblade.infinity_stellaris.logic.GammaThunderburstLogic;
import qwq.qingyi.annihilationblade.infinity_stellaris.logic.InfinityStellarisLogic;
import qwq.qingyi.annihilationblade.infinity_stellaris.logic.VacuumDecayCollapseLogic;
import qwq.qingyi.annihilationblade.loli_blade.LoliBladeDefinitions;
import qwq.qingyi.annihilationblade.loli_blade.logic.LoliBladeCombatLogic;
import qwq.qingyi.annihilationblade.loli_blade.logic.LoliBladeDefenseLogic;
import qwq.qingyi.annihilationblade.loli_blade.specialeffect.LoliFacingExecution;
import qwq.qingyi.annihilationblade.nightfall_dragon.logic.NightfallDragonFinalFormLogic;
import qwq.qingyi.annihilationblade.nightfall_dragon.logic.NightfallDragonReachLogic;
import qwq.qingyi.annihilationblade.nightfall_dragon.specialeffect.DemonicBloodParasite;
import qwq.qingyi.annihilationblade.nightfall_dragon.specialeffect.DragonPressureDomain;
import qwq.qingyi.annihilationblade.nightfall_dragon.specialeffect.OuterGodScar;
import qwq.qingyi.annihilationblade.nightfall_dragon.specialeffect.ReverseScaleHunt;

import java.util.UUID;

/**
 * 模组公共/服务端事件总控中心，统一桥接 Fabric API、SlashBlade-Refabricated 与方法级 Mixin 钩子。
 */
public final class AnnihilationFabricEvents {
    private AnnihilationFabricEvents() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(AnnihilationFabricEvents::onAllowDamage);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(AnnihilationFabricEvents::onAfterDamage);
        ServerLivingEntityEvents.ALLOW_DEATH.register(AnnihilationFabricEvents::onAllowDeath);

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }
            return LoliBladeCombatLogic.handleEntityAttack(player, entity);
        });

        PlayerTickEvent.END.register(event -> onPlayerTick(event.getEntity()));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onPlayerLogout(handler.getPlayer()));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> onPlayerChangedDimension(player)
        );

        // 先注册原 1.21.1 中的 HIGHEST 优先级斩击监听器，确保执行顺序一致
        SlashBladeEvent.DO_SLASH.register(LoliFacingExecution::onDoingSlash);
        SlashBladeEvent.DO_SLASH.register(Dankong::onDoingSlash);
        SlashBladeEvent.DO_SLASH.register(VoidDominion::onDoingSlash);
        SlashBladeEvent.DO_SLASH.register(StarlessJudgement::onDoingSlash);
        SlashBladeEvent.DO_SLASH.register(PhantomJudgement::onDoingSlash);
        SlashBladeEvent.DO_SLASH.register(AbyssalDecree::onDoingSlash);
        SlashBladeEvent.DO_SLASH.register(TerminusEcho::onDoingSlash);
        SlashBladeEvent.DO_SLASH.register(BloodPrisonLogic::onSlash);
        SlashBladeEvent.DO_SLASH.register(CosmicStringCutLogic::onDoingSlash);
        SlashBladeEvent.DO_SLASH.register(OuterGodScar::onDoingSlash);
        SlashBladeEvent.DO_SLASH.register(ReverseScaleHunt::onDoingSlash);
        SlashBladeEvent.DO_SLASH.register(NightfallDragonFinalFormLogic::onDoingSlash);

        SlashBladeEvent.HIT.register(CausalityCollapse::onSlashBladeHit);
        SlashBladeEvent.SUMMONEDSWORD_ONHIT_ENTITY.register(PhantomJudgement::onSummonedSwordHit);

        LivingExperienceDropEvent.EVENT.register(event -> {
            if (VacuumDecayCollapseLogic.shouldCancelDrops(event.getEntity())) {
                event.setDroppedExperience(0);
                event.setCanceled(true);
            }
        });
    }

    private static boolean onAllowDamage(LivingEntity target, DamageSource source, float amount) {
        // 先检查高优先级免伤与拦截逻辑，任意返回 true 即取消本次受击
        if (LoliBladeDefenseLogic.shouldCancelIncomingDamage(target, source, amount)) {
            return false;
        }
        if (LoliBladeCombatLogic.handleLivingAttack(target, source)) {
            return false;
        }
        if (NightfallDragonFinalFormLogic.shouldCancelIncomingDamage(target, source, amount)) {
            return false;
        }
        if (InfinityStellarisLogic.shouldCancelIncomingDamage(target)) {
            return false;
        }
        if (ModEventHandler.shouldCancelGodBladeAttack(target, source)) {
            return false;
        }
        if (ModEventHandler.shouldCancelLivingAttack(target)) {
            return false;
        }

        // 未被取消时依次触发不拦截事件的受击附加特效
        BloodPrisonLogic.onAttack(target, source);
        EntropyDissolutionLogic.onHurt(target, source);
        GammaThunderburstLogic.onHurt(target, source);
        return true;
    }

    /**
     * 供 {@link qwq.qingyi.annihilationblade.mixin.LivingEntityDamageMixin} 与
     * {@link qwq.qingyi.annihilationblade.mixin.PlayerDamageMixin} 调用的护甲前伤害修改钩子；
     * 返回 {@link Float#NEGATIVE_INFINITY} 时直接中止后续原版扣血流程。
     */
    public static float onLivingDamagePre(LivingEntity target, DamageSource source, float amount) {
        if (LoliBladeDefenseLogic.shouldCancelLivingHurt(target)) {
            return Float.NEGATIVE_INFINITY;
        }
        if (DemonicBloodParasite.onHurt(target, source, amount)) {
            return Float.NEGATIVE_INFINITY;
        }
        float modified = BloodPrisonLogic.onHurt(target, source, amount);
        return ModEventHandler.onHurt(target, source, modified);
    }

    private static void onAfterDamage(
            LivingEntity target,
            DamageSource source,
            float baseDamageTaken,
            float damageTaken,
            boolean blocked
    ) {
        NightfallDragonFinalFormLogic.onFinalFormDamage(target, source, damageTaken);
        LoliBladeCombatLogic.handleLivingDamage(target, source, damageTaken);
    }

    private static boolean onAllowDeath(LivingEntity target, DamageSource source, float damageAmount) {
        if (LoliBladeDefenseLogic.shouldCancelDeath(target)) {
            return false;
        }
        if (NightfallDragonFinalFormLogic.shouldCancelDeath(target)) {
            return false;
        }
        if (InfinityStellarisLogic.shouldCancelDeath(target)) {
            return false;
        }
        if (ModEventHandler.shouldCancelLivingDeath(target)) {
            return false;
        }

        BloodPrisonLogic.onDeath(target, source);
        DemonicBloodParasite.onDeath(target);
        return true;
    }

    /** 供 {@link qwq.qingyi.annihilationblade.mixin.LivingEntityDropsMixin} 调用的无掉落判定钩子。 */
    public static boolean shouldCancelDrops(LivingEntity entity) {
        return VacuumDecayCollapseLogic.shouldCancelDrops(entity);
    }

    private static void onPlayerTick(Player player) {
        if (!player.level().isClientSide()) {
            LoliBladeDefinitions.bindUnownedStacks(player);
        }
        LoliBladeDefenseLogic.onPlayerTick(player);
        ModEventHandler.onPlayerTick(player);
        Dankong.onPlayerTick(player);
        AbyssalDecree.onPlayerTick(player);
        PhantomJudgement.onPlayerTick(player);
        TerminusEcho.onPlayerTick(player);
        BloodPrisonLogic.onPlayerTick(player);
        CurvatureRuptureLogic.onPlayerTick(player);
        InfinityStellarisLogic.onPlayerTick(player);
        NightfallDragonReachLogic.onPlayerTick(player);
        DragonPressureDomain.onPlayerTick(player);
        NightfallDragonFinalFormLogic.onPlayerTick(player);
    }

    private static void onPlayerLogout(Player player) {
        UUID id = player.getUUID();
        ModEventHandler.onPlayerLogout(player);
        Dankong.clearPlayer(id);
        AbyssalDecree.clearPlayer(id);
        PhantomJudgement.clearPlayer(player.level(), id);
        TerminusEcho.clearPlayer(id);
        VoidDominion.clearPlayer(id);
        StarlessJudgement.clearPlayer(id);
        CausalityCollapse.clearPlayer(id);
        BloodPrisonLogic.onPlayerLogout(player);
        CurvatureRuptureLogic.clearPlayer(player);
        EntropyDissolutionLogic.clearPlayer(id);
        CosmicStringCutLogic.clearPlayer(id);
        InfinityStellarisLogic.onPlayerLogout(player);
        NightfallDragonReachLogic.onPlayerLogout(player);
        DemonicBloodParasite.onPlayerLogout(player);
        NightfallDragonFinalFormLogic.onPlayerLogout(player);
        LoliBladeDefenseLogic.clearPlayerState(player);
        LoliBladeCombatLogic.clearPlayerState(id);
    }

    private static void onPlayerChangedDimension(Player player) {
        UUID id = player.getUUID();
        ModEventHandler.onPlayerChangedDimension(player);
        Dankong.clearPlayer(id);
        AbyssalDecree.clearPlayer(id);
        PhantomJudgement.clearPlayer(player.level(), id);
        TerminusEcho.clearPlayer(id);
        VoidDominion.clearPlayer(id);
        StarlessJudgement.clearPlayer(id);
        CausalityCollapse.clearPlayer(id);
        BloodPrisonLogic.onPlayerChangedDimension(player);
        CurvatureRuptureLogic.clearPlayer(player);
        EntropyDissolutionLogic.clearPlayer(id);
        CosmicStringCutLogic.clearPlayer(id);
        InfinityStellarisLogic.onPlayerChangedDimension(player);
        NightfallDragonReachLogic.onPlayerChangedDimension(player);
        DemonicBloodParasite.onPlayerChangedDimension(player);
        NightfallDragonFinalFormLogic.onPlayerChangedDimension(player);
        LoliBladeDefenseLogic.clearPlayerState(player);
        LoliBladeCombatLogic.clearPlayerState(id);
    }
}
