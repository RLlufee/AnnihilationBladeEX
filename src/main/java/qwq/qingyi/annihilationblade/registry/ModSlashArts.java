package qwq.qingyi.annihilationblade.registry;

import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.blood_prison.logic.BloodPrisonLogic;
import qwq.qingyi.annihilationblade.infinity_stellaris.logic.VacuumDecayCollapseLogic;
import qwq.qingyi.annihilationblade.loli_blade.logic.LoliBladeCombatLogic;
import qwq.qingyi.annihilationblade.nightfall_dragon.logic.CosmicNightfallDescentLogic;
import qwq.qingyi.annihilationblade.nightfall_dragon.logic.NightfallDragonJudgementCutLogic;
import qwq.qingyi.annihilationblade.nightfall_dragon.logic.ScaleGuardLogic;

/**
 * 模组拔刀剑剑技（SA）注册表。
 */
public final class ModSlashArts {
    public static final Identifier SPATIAL_FRACTURE_ID = AnnihilationBladeex.prefix("spatial_fracture");
    public static final Identifier INFERNAL_SLAUGHTER_ID = AnnihilationBladeex.prefix("infernal_slaughter");
    public static final Identifier VACUUM_DECAY_COLLAPSE_ID = AnnihilationBladeex.prefix("vacuum_decay_collapse");
    public static final Identifier NIGHTFALL_JUDGEMENT_CUT_ID = AnnihilationBladeex.prefix("nightfall_judgement_cut");
    public static final Identifier SCALE_GUARD_ID = AnnihilationBladeex.prefix("scale_guard");
    public static final Identifier COSMIC_NIGHTFALL_DESCENT_ID = AnnihilationBladeex.prefix("cosmic_nightfall_descent");
    public static final Identifier LOLI_AREA_EXECUTION_ID = AnnihilationBladeex.prefix("loli_area_execution");

    public static final SlashArts SPATIAL_FRACTURE = register(
            SPATIAL_FRACTURE_ID,
            new SlashArts(entity -> ModComboStates.SPATIAL_FRACTURE_STATE_ID)
    );

    public static final SlashArts INFERNAL_SLAUGHTER = register(
            INFERNAL_SLAUGHTER_ID,
            new SlashArts(entity -> {
                if (entity instanceof Player player && !player.level().isClientSide()) {
                    BloodPrisonLogic.activateDomain(player);
                }
                return ModComboStates.INFERNAL_SLAUGHTER_STATE_ID;
            })
    );

    public static final SlashArts VACUUM_DECAY_COLLAPSE = register(
            VACUUM_DECAY_COLLAPSE_ID,
            new SlashArts(entity -> {
                if (entity instanceof Player player && !player.level().isClientSide()) {
                    VacuumDecayCollapseLogic.prepareCast(player);
                }
                return ModComboStates.VACUUM_DECAY_COLLAPSE_STATE_ID;
            })
    );

    public static final SlashArts NIGHTFALL_JUDGEMENT_CUT = register(
            NIGHTFALL_JUDGEMENT_CUT_ID,
            new SlashArts(entity -> {
                if (entity instanceof Player player && !player.level().isClientSide()) {
                    NightfallDragonJudgementCutLogic.prepareCast(player);
                }
                return ModComboStates.NIGHTFALL_JUDGEMENT_CUT_STATE_ID;
            })
    );

    public static final SlashArts SCALE_GUARD = register(
            SCALE_GUARD_ID,
            new SlashArts(entity -> {
                if (entity instanceof Player player && !player.level().isClientSide()) {
                    ScaleGuardLogic.prepareCast(player);
                }
                return ModComboStates.SCALE_GUARD_STATE_ID;
            })
    );

    public static final SlashArts COSMIC_NIGHTFALL_DESCENT = register(
            COSMIC_NIGHTFALL_DESCENT_ID,
            new SlashArts(entity -> {
                if (entity instanceof Player player && !player.level().isClientSide()) {
                    CosmicNightfallDescentLogic.prepareCast(player);
                }
                return ModComboStates.COSMIC_NIGHTFALL_DESCENT_STATE_ID;
            })
    );

    public static final SlashArts LOLI_AREA_EXECUTION = register(
            LOLI_AREA_EXECUTION_ID,
            new SlashArts(entity -> {
                if (entity instanceof Player player && !player.level().isClientSide()) {
                    LoliBladeCombatLogic.prepareCast(player);
                }
                return ModComboStates.LOLI_AREA_EXECUTION_STATE_ID;
            })
    );

    private ModSlashArts() {
    }

    private static SlashArts register(Identifier id, SlashArts arts) {
        return Registry.register(SlashArtsRegistry.SLASH_ARTS, id, arts);
    }

    public static void register() {
        // 触发静态初始化完成注册
    }
}
