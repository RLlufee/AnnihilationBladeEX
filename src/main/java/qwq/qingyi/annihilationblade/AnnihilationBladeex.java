package qwq.qingyi.annihilationblade;

import com.mojang.logging.LogUtils;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import qwq.qingyi.annihilationblade.common.ServerTickScheduler;
import qwq.qingyi.annihilationblade.config.ModConfig;
import qwq.qingyi.annihilationblade.event.AnnihilationFabricEvents;
import qwq.qingyi.annihilationblade.loli_blade.LoliBladeTheseusGuard;
import qwq.qingyi.annihilationblade.network.ModNetwork;
import qwq.qingyi.annihilationblade.registry.ModComboStates;
import qwq.qingyi.annihilationblade.registry.ModCreativeTabs;
import qwq.qingyi.annihilationblade.registry.ModEntities;
import qwq.qingyi.annihilationblade.registry.ModItems;
import qwq.qingyi.annihilationblade.registry.ModSlashArts;
import qwq.qingyi.annihilationblade.registry.ModSounds;
import qwq.qingyi.annihilationblade.registry.ModSpecialEffects;

import java.util.List;

/**
 * AnnihilationBladeEX / 终焉圣殿模组主入口（Fabric 26.1.2）。
 */
public class AnnihilationBladeex implements ModInitializer {
    public static final String MOD_ID = "annihilationbladeex";
    public static final String MODID = MOD_ID;
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Identifier BLADE_ID = prefix("annihilation_blade");
    public static final Identifier SPATIAL_FRACTURE_ID = prefix("spatial_fracture");
    public static final Identifier DANKONG_ID = prefix("dankong");
    public static final Identifier WORLD_RIFT_ID = prefix("world_rift");
    public static final Identifier TERMINUS_ECHO_ID = prefix("terminus_echo");
    public static final Identifier VOID_DOMINION_ID = prefix("void_dominion");
    public static final Identifier CAUSALITY_COLLAPSE_ID = prefix("causality_collapse");
    public static final Identifier STARLESS_JUDGEMENT_ID = prefix("starless_judgement");
    public static final Identifier PHANTOM_JUDGEMENT_ID = prefix("phantom_judgement");
    public static final Identifier ABYSSAL_DECREE_ID = prefix("abyssal_decree");
    public static final String BLADE_TRANSLATION_KEY = "item." + MODID + ".annihilation_blade";

    public static final List<Identifier> GOD_SPECIAL_EFFECTS = List.of(
            DANKONG_ID,
            WORLD_RIFT_ID,
            TERMINUS_ECHO_ID,
            VOID_DOMINION_ID,
            CAUSALITY_COLLAPSE_ID,
            STARLESS_JUDGEMENT_ID,
            PHANTOM_JUDGEMENT_ID,
            ABYSSAL_DECREE_ID
    );

    @Override
    public void onInitialize() {
        ConfigRegistry.INSTANCE.register(MOD_ID, net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfig.COMMON_SPEC);
        ModSounds.register();
        ModEntities.register();
        ModItems.register();
        ModSpecialEffects.register();
        ModComboStates.register();
        ModSlashArts.register();
        ModCreativeTabs.register();
        ModNetwork.register();
        ServerTickScheduler.register();
        LoliBladeTheseusGuard.register();
        AnnihilationFabricEvents.register();
    }

    public static Identifier prefix(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
