package QWQ.QingYi.annihilationblade.annihilation_blade.specialeffect;

import QWQ.QingYi.annihilationblade.annihilation_blade.logic.TerminusLogic;
import QWQ.QingYi.annihilationblade.annihilation_blade.visual.AnnihilationFxEvents;
import QWQ.QingYi.annihilationblade.common.SpecialEffectSupport;
import QWQ.QingYi.annihilationblade.config.ModConfig;
import QWQ.QingYi.annihilationblade.registry.ModSpecialEffects;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent.HitEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "annihilationblade")
public class CausalityCollapse extends SpecialEffect {
   private static final Map<UUID, Long> LAST_TRIGGER = new HashMap<>();

   public CausalityCollapse() {
      super(0, false, false);
   }

   public static void clearPlayer(UUID playerId) {
      LAST_TRIGGER.remove(playerId);
   }

   /** 链的视觉起点：施法者身前略低处（大致是挥刀时刀身的位置），避免第一人称时链从眼睛里长出来。 */
   private static Vec3 bladeOrigin(Player player) {
      Vec3 look = player.getLookAngle();
      return player.getEyePosition().add(look.scale(0.9)).add(0.0, -0.35, 0.0);
   }

   @SubscribeEvent
   public static void onSlashBladeHit(HitEvent event) {
      ISlashBladeState state = event.getSlashBladeState();
      if (state.hasSpecialEffect(ModSpecialEffects.CAUSALITY_COLLAPSE.getId())) {
         if (event.getUser() instanceof Player player) {
            LivingEntity firstTarget = event.getTarget();
            if (firstTarget != null) {
               if (firstTarget.level() instanceof ServerLevel level) {
                  ModConfig.CausalityCollapse config = ModConfig.COMMON.annihilationBlade.causalityCollapse;
                  if (SpecialEffectSupport.tryStartCooldown(LAST_TRIGGER, player, level.getGameTime(), config.cooldownTicks.getValue())) {
                     List<LivingEntity> chain = SpecialEffectSupport.nearestChain(level, player, firstTarget, config.chainRadius.getValue(), config.maxChain.getValue());
                     if (!chain.isEmpty()) {
                        Vec3 previous = player.getEyePosition();
                        Vec3 anchor = SpecialEffectSupport.centerOf(chain.get(0));
                        level.playSound(null, anchor.x, anchor.y, anchor.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.85F);
                        level.playSound(null, anchor.x, anchor.y, anchor.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 0.7F);
                        // 先发事件再处决：客户端需要在目标被移除前解析到整条链
                        AnnihilationFxEvents.causalityCollapse(
                           level, player, bladeOrigin(player), config.chainRadius.getValue(), config.visualScale.getValue(), chain
                        );

                        for (int index = 0; index < chain.size(); index++) {
                           LivingEntity target = chain.get(index);
                           Vec3 targetCenter = SpecialEffectSupport.centerOf(target);
                           SpecialEffectSupport.pullToward(target, previous, 0.12);
                           TerminusLogic.execute(target, player);
                           previous = targetCenter;
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
