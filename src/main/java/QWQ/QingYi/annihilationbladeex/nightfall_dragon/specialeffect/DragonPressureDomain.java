package QWQ.QingYi.annihilationbladeex.nightfall_dragon.specialeffect;

import QWQ.QingYi.annihilationbladeex.nightfall_dragon.item.NightfallDragonItemSupport;
import QWQ.QingYi.annihilationbladeex.registry.ModSpecialEffects;
import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "annihilationbladeex")
public class DragonPressureDomain extends SpecialEffect {
   private static final int BUFF_TICKS = 60;
   private static final int AMPLIFIER_III = 2;
   private static final int AURA_INTERVAL_TICKS = 20;
   private static final int AURA_DRAGON_BREATH_PARTICLES = 10;
   private static final int AURA_ENCHANT_PARTICLES = 15;

   public DragonPressureDomain() {
      super(0, false, false);
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent.Post event) {
      Player player = event.getEntity();
      if (player.level().isClientSide) {
         return;
      }

      ItemStack stack = NightfallDragonItemSupport.heldNightfallDragon(player);
      if (stack.isEmpty() || !hasDragonPressure(stack)) {
         return;
      }

      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, BUFF_TICKS, AMPLIFIER_III, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BUFF_TICKS, AMPLIFIER_III, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, AMPLIFIER_III, false, true, true));
      if (!hasFinalGodBody(stack)) {
         player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, BUFF_TICKS, AMPLIFIER_III, false, true, true));
      }

      if (player.tickCount % AURA_INTERVAL_TICKS == 0 && player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(), AURA_DRAGON_BREATH_PARTICLES, 0.85, 0.5, 0.85, 0.025);
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + player.getBbHeight() * 0.48, player.getZ(), AURA_ENCHANT_PARTICLES, 1.05, 0.65, 1.05, 0.08);
      }
   }

   private static boolean hasDragonPressure(ItemStack stack) {
      return stack.getItem() instanceof ItemSlashBlade
         && BladeStateAccess.of(stack)
            .map(state -> state.hasSpecialEffect(ModSpecialEffects.DRAGON_PRESSURE_DOMAIN.getId()))
            .orElse(false);
   }

   private static boolean hasFinalGodBody(ItemStack stack) {
      return stack.getItem() instanceof ItemSlashBlade
         && BladeStateAccess.of(stack)
            .map(state -> state.hasSpecialEffect(ModSpecialEffects.DRAGON_GOD_BODY.getId()))
            .orElse(false);
   }
}
