package QWQ.QingYi.annihilationbladeex;

import QWQ.QingYi.annihilationbladeex.config.ModConfig;
import QWQ.QingYi.annihilationbladeex.registry.ModItems;
import com.iafenvoy.jupiter.ConfigManager;
import com.iafenvoy.jupiter.render.screen.ConfigSelectScreen;
import mods.flammpfeil.slashblade.client.renderer.SlashBladeTEISR;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = AnnihilationBladeEX.MODID, bus = Bus.MOD, value = Dist.CLIENT)
public final class AnnihilationBladeEXClient {
    private AnnihilationBladeEXClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ConfigManager.getInstance().registerConfigHandler(ModConfig.CLIENT);
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                () -> (container, parent) -> ConfigSelectScreen
                        .builder(Component.translatable("config.annihilationbladeex.title"), parent)
                        .common(ModConfig.COMMON)
                        .client(ModConfig.CLIENT)
                        .build());
        event.enqueueWork(() -> {
            registerBladeUserProperty(ModItems.ANNIHILATION_BLADE.get());
            registerBladeUserProperty(ModItems.NIGHTFALL_DRAGON.get());
        });
    }

    private static void registerBladeUserProperty(Item item) {
        ItemProperties.register(item,
                ResourceLocation.parse("slashblade:user"),
                (ClampedItemPropertyFunction) (stack, level, entity, seed) -> {
                    BladeModel.user = entity;
                    return 0;
                });
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        var extensions = new IClientItemExtensions() {
            final BlockEntityWithoutLevelRenderer renderer = new SlashBladeTEISR(
                    Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels());

            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        };
        event.registerItem(extensions, ModItems.ANNIHILATION_BLADE.get(), ModItems.NIGHTFALL_DRAGON.get());
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        bakeBladeModel(ModItems.ANNIHILATION_BLADE.get(), event);
        bakeBladeModel(ModItems.NIGHTFALL_DRAGON.get(), event);
    }

    private static void bakeBladeModel(Item item, ModelEvent.ModifyBakingResult event) {
        ModelResourceLocation loc = ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(item));
        var bakedModel = event.getModels().get(loc);
        if (bakedModel != null) {
            event.getModels().put(loc, new BladeModel(bakedModel, event.getModelBakery()));
        }
    }
}
