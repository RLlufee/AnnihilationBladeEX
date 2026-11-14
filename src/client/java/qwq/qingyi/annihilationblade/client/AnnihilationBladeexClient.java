package qwq.qingyi.annihilationblade.client;

import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import mods.flammpfeil.slashblade.client.ClientHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import qwq.qingyi.annihilationblade.AnnihilationBladeex;
import qwq.qingyi.annihilationblade.client.annihilation_blade.ClientBladeVision;
import qwq.qingyi.annihilationblade.client.annihilation_blade.DankongBlinkKeyHandler;
import qwq.qingyi.annihilationblade.client.blood_prison.BloodPrisonClientEffects;
import qwq.qingyi.annihilationblade.client.blood_prison.BloodPrisonOverlay;
import qwq.qingyi.annihilationblade.client.infinity_stellaris.InfinityStellarisAiErasureKeyHandler;
import qwq.qingyi.annihilationblade.client.infinity_stellaris.InfinityStellarisClientEvents;
import qwq.qingyi.annihilationblade.client.network.ModClientNetwork;
import qwq.qingyi.annihilationblade.client.nightfall_dragon.NightfallDragonClientEvents;
import qwq.qingyi.annihilationblade.client.nightfall_dragon.NightfallDragonModeKeyHandler;
import qwq.qingyi.annihilationblade.client.nightfall_dragon.NightfallDragonRenderEvents;
import qwq.qingyi.annihilationblade.config.ModConfig;
import qwq.qingyi.annihilationblade.registry.ModItems;

/**
 * 模组 Fabric 客户端入口，负责客户端配置、网络收包、按键映射、HUD、实体渲染与拔刀剑模型烘焙包装。
 */
public class AnnihilationBladeexClient implements ClientModInitializer, ModelLoadingPlugin {
    public static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(AnnihilationBladeex.prefix("annihilationbladeex"));

    @Override
    public void onInitializeClient() {
        ConfigRegistry.INSTANCE.register(
                AnnihilationBladeex.MOD_ID,
                net.neoforged.fml.config.ModConfig.Type.CLIENT,
                ModConfig.CLIENT_SPEC
        );
        ModClientNetwork.register();
        DankongBlinkKeyHandler.register();
        InfinityStellarisAiErasureKeyHandler.register();
        NightfallDragonModeKeyHandler.register();
        BloodPrisonClientEffects.register();
        BloodPrisonOverlay.register();
        ClientBladeVision.register();
        InfinityStellarisClientEvents.register();
        NightfallDragonClientEvents.register();
        NightfallDragonRenderEvents.register();
        ModelLoadingPlugin.register(this);
    }

    @Override
    public void initialize(Context pluginContext) {
        Identifier annihilationId = BuiltInRegistries.ITEM.getKey(ModItems.ANNIHILATION_BLADE);
        Identifier nightfallId = BuiltInRegistries.ITEM.getKey(ModItems.NIGHTFALL_DRAGON);
        pluginContext.modifyItemModelAfterBake().register((model, context) -> {
            Identifier itemId = context.itemId();
            if (annihilationId.equals(itemId) || nightfallId.equals(itemId)) {
                return ClientHandler.bakeBlade(model, context.bakingContext());
            }
            return model;
        });
    }
}
