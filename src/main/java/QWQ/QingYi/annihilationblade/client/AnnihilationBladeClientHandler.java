package QWQ.QingYi.annihilationblade.client;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import com.iafenvoy.jupiter.ConfigManager;
import com.iafenvoy.jupiter.render.screen.ConfigSelectScreen;
import QWQ.QingYi.annihilationblade.registry.ModItems;
import java.util.Objects;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = Annihilationblade.MODID, bus = Bus.MOD, value = Dist.CLIENT)
public final class AnnihilationBladeClientHandler {
   private AnnihilationBladeClientHandler() {
   }

   @SubscribeEvent
   public static void onClientSetup(FMLClientSetupEvent event) {
      ConfigManager.getInstance().registerConfigHandler(ModConfig.CLIENT);
      ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
         () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> ConfigSelectScreen
            .builder(Component.translatable("config.annihilationblade.title"), parent)
            .common(ModConfig.COMMON)
            .client(ModConfig.CLIENT)
            .build()));
      event.enqueueWork(() -> ItemProperties.register(
         ModItems.ANNIHILATION_BLADE.get(),
         ResourceLocation.fromNamespaceAndPath("slashblade", "user"),
         (ClampedItemPropertyFunction)(stack, level, entity, seed) -> {
            BladeModel.user = entity;
            return 0.0F;
         }
      ));
   }

   @SubscribeEvent
   public static void onModelBaking(ModelEvent.ModifyBakingResult event) {
      bakeBlade(ModItems.ANNIHILATION_BLADE.get(), event);
   }

   private static void bakeBlade(Item blade, ModelEvent.ModifyBakingResult event) {
      ModelResourceLocation location = new ModelResourceLocation(Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(blade)), "inventory");
      BladeModel model = new BladeModel(event.getModels().get(location), event.getModelBakery());
      event.getModels().put(location, model);
   }
}
