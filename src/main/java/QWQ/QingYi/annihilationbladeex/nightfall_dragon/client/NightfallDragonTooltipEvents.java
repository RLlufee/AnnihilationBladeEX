package QWQ.QingYi.annihilationbladeex.nightfall_dragon.client;

import QWQ.QingYi.annihilationbladeex.AnnihilationBladeEX;
import QWQ.QingYi.annihilationbladeex.config.ModConfig;
import QWQ.QingYi.annihilationbladeex.nightfall_dragon.NightfallDragonDefinitions;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = AnnihilationBladeEX.MODID, value = Dist.CLIENT)
public final class NightfallDragonTooltipEvents {
   private NightfallDragonTooltipEvents() {
   }

   @SubscribeEvent
   public static void onRenderTooltipPre(RenderTooltipEvent.Pre event) {
      if (!ModConfig.CLIENT.clientTooltips.enableNightfallDragonRenderer.getValue()) {
         return;
      }

      ItemStack stack = event.getItemStack();
      if (!NightfallDragonDefinitions.isNightfallDragon(stack)) {
         return;
      }

      Minecraft minecraft = Minecraft.getInstance();
      TooltipFlag tooltipFlag = minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
      Item.TooltipContext context = minecraft.level == null ? Item.TooltipContext.EMPTY : Item.TooltipContext.of(minecraft.level);
      List<Component> vanillaLines = stack.getTooltipLines(context, minecraft.player, tooltipFlag);
      NightfallDragonTooltipRenderer.render(event.getGraphics(), event.getFont(), stack, vanillaLines, event.getX(), event.getY());
      event.setCanceled(true);
   }
}
