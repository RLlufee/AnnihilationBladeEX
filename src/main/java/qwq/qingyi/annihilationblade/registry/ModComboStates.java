package qwq.qingyi.annihilationblade.registry;

import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.registry.combo.ComboState.Builder;
import mods.flammpfeil.slashblade.registry.combo.ComboState.TimeLineTickAction;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.annihilation_blade.logic.SpatialFractureExecutor;
import qwq.qingyi.annihilationblade.blood_prison.logic.BloodPrisonLogic;
import qwq.qingyi.annihilationblade.infinity_stellaris.logic.VacuumDecayCollapseLogic;
import qwq.qingyi.annihilationblade.loli_blade.logic.LoliBladeCombatLogic;
import qwq.qingyi.annihilationblade.nightfall_dragon.logic.CosmicNightfallDescentLogic;
import qwq.qingyi.annihilationblade.nightfall_dragon.logic.NightfallDragonJudgementCutLogic;
import qwq.qingyi.annihilationblade.nightfall_dragon.logic.ScaleGuardLogic;

/**
 * 模组拔刀剑 ComboState（SA 动作时间轴状态）注册表。
 */
public final class ModComboStates {
    private static final Identifier SLASHBLADE_NONE = Identifier.fromNamespaceAndPath("slashblade", "none");

    public static final Identifier SPATIAL_FRACTURE_STATE_ID = AnnihilationBladeex.prefix("spatial_fracture_state");
    public static final Identifier INFERNAL_SLAUGHTER_STATE_ID = AnnihilationBladeex.prefix("infernal_slaughter_state");
    public static final Identifier VACUUM_DECAY_COLLAPSE_STATE_ID = AnnihilationBladeex.prefix("vacuum_decay_collapse_state");
    public static final Identifier NIGHTFALL_JUDGEMENT_CUT_STATE_ID = AnnihilationBladeex.prefix("nightfall_judgement_cut_state");
    public static final Identifier SCALE_GUARD_STATE_ID = AnnihilationBladeex.prefix("scale_guard_state");
    public static final Identifier COSMIC_NIGHTFALL_DESCENT_STATE_ID = AnnihilationBladeex.prefix("cosmic_nightfall_descent_state");
    public static final Identifier LOLI_AREA_EXECUTION_STATE_ID = AnnihilationBladeex.prefix("loli_area_execution_state");

    public static final ComboState SPATIAL_FRACTURE_STATE = register(
            SPATIAL_FRACTURE_STATE_ID,
            Builder.newInstance()
                    .priority(100)
                    .startAndEnd(400, 460)
                    .motionLoc(DefaultResources.ExMotionLocation)
                    .next(entity -> SLASHBLADE_NONE)
                    .nextOfTimeout(entity -> SLASHBLADE_NONE)
                    .addTickAction(
                            TimeLineTickAction.getBuilder()
                                    .put(5, entity -> entity.level().playSound(
                                            null, entity.getX(), entity.getY(), entity.getZ(),
                                            SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.0F, 0.5F
                                    ))
                                    .put(15, entity -> {
                                        if (entity instanceof Player player && !player.level().isClientSide()) {
                                            SpatialFractureExecutor.unleash(player);
                                        }
                                    })
                                    .build()
                    )
                    .build()
    );

    public static final ComboState INFERNAL_SLAUGHTER_STATE = register(
            INFERNAL_SLAUGHTER_STATE_ID,
            Builder.newInstance()
                    .priority(90)
                    .startAndEnd(500, 540)
                    .motionLoc(DefaultResources.ExMotionLocation)
                    .next(entity -> SLASHBLADE_NONE)
                    .nextOfTimeout(entity -> SLASHBLADE_NONE)
                    .addTickAction(TimeLineTickAction.getBuilder().put(8, entity -> {
                        if (entity instanceof Player player && !player.level().isClientSide()) {
                            BloodPrisonLogic.activateDomain(player);
                        }
                    }).build())
                    .build()
    );

    public static final ComboState VACUUM_DECAY_COLLAPSE_STATE = register(
            VACUUM_DECAY_COLLAPSE_STATE_ID,
            Builder.newInstance()
                    .priority(95)
                    .startAndEnd(560, 620)
                    .motionLoc(DefaultResources.ExMotionLocation)
                    .next(entity -> SLASHBLADE_NONE)
                    .nextOfTimeout(entity -> SLASHBLADE_NONE)
                    .addTickAction(
                            TimeLineTickAction.getBuilder()
                                    .put(4, entity -> entity.level().playSound(
                                            null, entity.getX(), entity.getY(), entity.getZ(),
                                            SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.2F, 0.65F
                                    ))
                                    .put(16, entity -> {
                                        if (entity instanceof Player player && !player.level().isClientSide()) {
                                            VacuumDecayCollapseLogic.unleash(player);
                                        }
                                    })
                                    .build()
                    )
                    .build()
    );

    public static final ComboState NIGHTFALL_JUDGEMENT_CUT_STATE = register(
            NIGHTFALL_JUDGEMENT_CUT_STATE_ID,
            Builder.newInstance()
                    .priority(80)
                    .startAndEnd(640, 700)
                    .motionLoc(DefaultResources.ExMotionLocation)
                    .next(entity -> SLASHBLADE_NONE)
                    .nextOfTimeout(entity -> SLASHBLADE_NONE)
                    .addTickAction(
                            TimeLineTickAction.getBuilder()
                                    .put(4, entity -> entity.level().playSound(
                                            null, entity.getX(), entity.getY(), entity.getZ(),
                                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.65F
                                    ))
                                    .put(8, entity -> {
                                        if (entity instanceof Player player && !player.level().isClientSide()) {
                                            NightfallDragonJudgementCutLogic.unleash(player);
                                        }
                                    })
                                    .build()
                    )
                    .build()
    );

    public static final ComboState SCALE_GUARD_STATE = register(
            SCALE_GUARD_STATE_ID,
            Builder.newInstance()
                    .priority(85)
                    .startAndEnd(720, 780)
                    .motionLoc(DefaultResources.ExMotionLocation)
                    .next(entity -> SLASHBLADE_NONE)
                    .nextOfTimeout(entity -> SLASHBLADE_NONE)
                    .addTickAction(
                            TimeLineTickAction.getBuilder()
                                    .put(3, entity -> entity.level().playSound(
                                            null, entity.getX(), entity.getY(), entity.getZ(),
                                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4F, 0.55F
                                    ))
                                    .put(9, entity -> {
                                        if (entity instanceof Player player && !player.level().isClientSide()) {
                                            ScaleGuardLogic.unleash(player);
                                        }
                                    })
                                    .build()
                    )
                    .build()
    );

    public static final ComboState COSMIC_NIGHTFALL_DESCENT_STATE = register(
            COSMIC_NIGHTFALL_DESCENT_STATE_ID,
            Builder.newInstance()
                    .priority(110)
                    .startAndEnd(800, 860)
                    .motionLoc(DefaultResources.ExMotionLocation)
                    .next(entity -> SLASHBLADE_NONE)
                    .nextOfTimeout(entity -> SLASHBLADE_NONE)
                    .addTickAction(
                            TimeLineTickAction.getBuilder()
                                    .put(3, entity -> entity.level().playSound(
                                            null, entity.getX(), entity.getY(), entity.getZ(),
                                            SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.4F, 0.55F
                                    ))
                                    .put(10, entity -> {
                                        if (entity instanceof Player player && !player.level().isClientSide()) {
                                            CosmicNightfallDescentLogic.unleash(player);
                                        }
                                    })
                                    .build()
                    )
                    .build()
    );

    public static final ComboState LOLI_AREA_EXECUTION_STATE = register(
            LOLI_AREA_EXECUTION_STATE_ID,
            Builder.newInstance()
                    .priority(120)
                    .startAndEnd(880, 940)
                    .motionLoc(DefaultResources.ExMotionLocation)
                    .next(entity -> SLASHBLADE_NONE)
                    .nextOfTimeout(entity -> SLASHBLADE_NONE)
                    .addTickAction(TimeLineTickAction.getBuilder().put(8, entity -> {
                        if (entity instanceof Player player && !player.level().isClientSide()) {
                            LoliBladeCombatLogic.handleAreaSlashArt(player);
                        }
                    }).build())
                    .build()
    );

    private ModComboStates() {
    }

    private static ComboState register(Identifier id, ComboState state) {
        return Registry.register(ComboStateRegistry.COMBO_STATE, id, state);
    }

    public static void register() {
        // 触发静态初始化完成注册
    }
}
