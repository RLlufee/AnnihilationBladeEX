package QWQ.QingYi.annihilationblade.nightfall_dragon.client.fx;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import QWQ.QingYi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;
import QWQ.QingYi.annihilationblade.registry.ModSpecialEffects;
import QWQ.QingYi.annihilationblade.render.client.core.FxManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * 每 tick 扫描附近玩家：手持终极形态魔龙夜陨且带「龙神之躯」/「绝对湮灭结界」SE 时，为其维持一个 {@link NightfallAuraFx}。
 * 判断完全基于客户端已同步的物品数据，服务端无需为常驻光环发送任何数据。
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Annihilationblade.MODID, value = Dist.CLIENT)
public final class NightfallAuraTracker {
   private static final double SCAN_RANGE = 160.0;
   private static final Int2ObjectOpenHashMap<NightfallAuraFx> AURAS = new Int2ObjectOpenHashMap<>();
   private static ClientLevel lastLevel;

   private NightfallAuraTracker() {
   }

   @SubscribeEvent
   public static void onClientTick(TickEvent.ClientTickEvent event) {
      if (event.phase != TickEvent.Phase.END) {
         return;
      }
      Minecraft minecraft = Minecraft.getInstance();
      ClientLevel level = minecraft.level;
      if (level != lastLevel) {
         lastLevel = level;
         AURAS.clear();
      }
      if (level == null || minecraft.isPaused()) {
         return;
      }
      AURAS.values().removeIf(NightfallAuraFx::removed);
      Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
      float radius = (float)(double)ModConfig.COMMON.nightfallDragon.absoluteDomainRadius.getValue();
      for (Player player : level.players()) {
         if (player.isSpectator() || player.distanceToSqr(camera) > SCAN_RANGE * SCAN_RANGE) {
            continue;
         }
         boolean godBody = NightfallDragonItemSupport.holdsFinalWithEffect(player, ModSpecialEffects.DRAGON_GOD_BODY.getId());
         boolean domain = NightfallDragonItemSupport.holdsFinalWithEffect(player, ModSpecialEffects.ABSOLUTE_ANNIHILATION_DOMAIN.getId());
         if (!godBody && !domain) {
            continue;
         }
         NightfallAuraFx aura = AURAS.get(player.getId());
         if (aura == null || aura.removed()) {
            aura = new NightfallAuraFx(player.getId());
            AURAS.put(player.getId(), aura);
            FxManager.add(aura);
         }
         aura.update(godBody, domain, radius);
      }
   }
}
