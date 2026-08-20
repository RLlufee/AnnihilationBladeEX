package QWQ.QingYi.annihilationblade.nightfall_dragon.logic;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import QWQ.QingYi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;
import QWQ.QingYi.annihilationblade.nightfall_dragon.entity.DragonHeadChargeEntity;
import QWQ.QingYi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class DragonHeadChargeLogic {
   private DragonHeadChargeLogic() {
   }

   public static void prepareCast(Player player) {
      if (player instanceof ServerPlayer serverPlayer && canUseAwakenedArt(serverPlayer)) {
         ServerLevel level = serverPlayer.serverLevel();
         Vec3 eye = serverPlayer.getEyePosition();
         Vec3 look = serverPlayer.getLookAngle().normalize();
         Vec3 center = eye.add(look.scale(2.2D));
         level.sendParticles(ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, 24, 0.8D, 0.35D, 0.8D, 0.04D);
         level.playSound(null, center.x, center.y, center.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.6F, 0.7F);
      }
   }

   public static void unleash(Player player) {
      if (!(player instanceof ServerPlayer serverPlayer) || !canUseAwakenedArt(serverPlayer)) {
         return;
      }

      ServerLevel level = serverPlayer.serverLevel();
      Vec3 direction = serverPlayer.getLookAngle().normalize();
      DragonHeadChargeEntity head = DragonHeadChargeEntity.create(level, serverPlayer, direction);
      if (head == null) {
         return;
      }

      level.addFreshEntity(head);
      Annihilationblade.LOGGER.info(
         "Spawned dragon_head_charge id={} at [{}, {}, {}], dir=[{}, {}, {}]",
         head.getId(),
         head.getX(),
         head.getY(),
         head.getZ(),
         direction.x,
         direction.y,
         direction.z
      );
      Vec3 roar = serverPlayer.getEyePosition().add(direction.scale(3.0D));
      level.playSound(null, roar.x, roar.y, roar.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.4F, 0.65F);
      level.playSound(null, roar.x, roar.y, roar.z, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 1.4F, 0.55F);
      level.sendParticles(ParticleTypes.DRAGON_BREATH, roar.x, roar.y, roar.z, 36, 0.9D, 0.45D, 0.9D, 0.08D);
   }

   private static boolean canUseAwakenedArt(ServerPlayer player) {
      ItemStack stack = player.getMainHandItem();
      return NightfallDragonItemSupport.isNightfallDragon(stack) && NightfallDragonDefinitions.FORM_AWAKENED.equals(NightfallDragonDefinitions.getForm(stack));
   }
}
