package QWQ.QingYi.annihilationblade.nightfall_dragon.specialeffect;

import QWQ.QingYi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;
import QWQ.QingYi.annihilationblade.registry.ModSpecialEffects;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "annihilationblade")
public class DragonPressureDomain extends SpecialEffect {
   private static final int BUFF_TICKS = 60;
   private static final int AMPLIFIER_III = 2;

   public DragonPressureDomain() {
      super(0, false, false);
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase != Phase.END || event.player.level().isClientSide) {
         return;
      }

      Player player = event.player;
      ItemStack stack = NightfallDragonItemSupport.heldNightfallDragon(player);
      if (stack.isEmpty() || !hasDragonPressure(stack)) {
         return;
      }

      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BUFF_TICKS, AMPLIFIER_III, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BUFF_TICKS, AMPLIFIER_III, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, AMPLIFIER_III, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, BUFF_TICKS, AMPLIFIER_III, false, true, true));
      if (player.tickCount % 10 == 0 && player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(), 5, 0.7, 0.45, 0.7, 0.02);
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + player.getBbHeight() * 0.48, player.getZ(), 10, 0.9, 0.55, 0.9, 0.08);
      }
   }

   private static boolean hasDragonPressure(ItemStack stack) {
      return stack.getItem() instanceof ItemSlashBlade
         && stack.getCapability(ItemSlashBlade.BLADESTATE)
            .map(state -> state.hasSpecialEffect(ModSpecialEffects.DRAGON_PRESSURE_DOMAIN.getId()))
            .orElse(false);
   }
}
