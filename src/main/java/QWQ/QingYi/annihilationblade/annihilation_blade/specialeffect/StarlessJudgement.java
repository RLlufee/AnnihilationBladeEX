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
import mods.flammpfeil.slashblade.event.SlashBladeEvent.DoSlashEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "annihilationblade")
public class StarlessJudgement extends SpecialEffect {
   private static final Map<UUID, Long> LAST_TRIGGER = new HashMap<>();

   public StarlessJudgement() {
      super(0, false, false);
   }

   public static void clearPlayer(UUID playerId) {
      LAST_TRIGGER.remove(playerId);
   }

   @SubscribeEvent
   public static void onDoingSlash(DoSlashEvent event) {
      if (event.getUser() instanceof ServerPlayer player) {
         if (!Dankong.isActive(player)) {
            ISlashBladeState state = event.getSlashBladeState();
            if (state.hasSpecialEffect(ModSpecialEffects.STARLESS_JUDGEMENT.getId())) {
               ModConfig.StarlessJudgement config = ModConfig.COMMON.annihilationBlade.starlessJudgement;
               if (SpecialEffectSupport.tryStartCooldown(LAST_TRIGGER, player, player.level().getGameTime(), config.cooldownTicks.getValue())) {
                  ServerLevel level = player.serverLevel();
                  double range = config.range.getValue();
                  double width = config.width.getValue();
                  double visualScale = config.visualScale.getValue();
                  Vec3 direction = player.getLookAngle().normalize();
                  Vec3 right = SpecialEffectSupport.rightOf(direction);
                  Vec3 start = player.getEyePosition().add(direction.scale(1.4));
                  level.playSound(null, start.x, start.y, start.z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.2F, 1.75F);
                  level.playSound(null, start.x, start.y, start.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.8F, 1.35F);
                  List<LivingEntity> targets = SpecialEffectSupport.beamTargets(level, player, start, direction, range, width, config.maxTargets.getValue());
                  // 先发事件再处决：客户端需要在目标被移除前解析到它们
                  AnnihilationFxEvents.starlessJudgement(level, player, start, direction, range, width, visualScale, targets);

                  for (int index = 0; index < targets.size(); index++) {
                     LivingEntity target = targets.get(index);
                     Vec3 targetCenter = SpecialEffectSupport.centerOf(target);
                     double projection = targetCenter.subtract(start).dot(direction);
                     Vec3 nearest = start.add(direction.scale(Math.max(0.0, Math.min(range, projection))));
                     SpecialEffectSupport.pullToward(target, nearest, 0.1);
                     TerminusLogic.execute(target, player);
                  }
               }
            }
         }
      }
   }
}
