package QWQ.QingYi.annihilationblade.nightfall_dragon.specialeffect;

import QWQ.QingYi.annihilationblade.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;
import QWQ.QingYi.annihilationblade.registry.ModSpecialEffects;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = "annihilationblade")
public class DemonicBloodParasite extends SpecialEffect {
   private static final double EXTRA_DAMAGE_PERCENT = 0.05;
   private static final int EFFECT_TICKS = 120;
   private static final Map<UUID, Map<UUID, Integer>> BLOOD_MARKS = new HashMap<>();
   private static final Set<UUID> INTERNAL_DAMAGE = new java.util.HashSet<>();

   public DemonicBloodParasite() {
      super(0, false, false);
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onHurt(LivingHurtEvent event) {
      LivingEntity target = event.getEntity();
      if (target.level().isClientSide || isInternalDamage(target)) {
         return;
      }

      Player player = findNightfallAttacker(event);
      if (player == null || !SlashBladeTargeting.canAttack(player, target)) {
         return;
      }

      if (hasMark(player, target)) {
         convertToTrueDamage(event, player, target);
      }

      int marks = addMark(player, target);
      target.addEffect(new MobEffectInstance(MobEffects.WITHER, EFFECT_TICKS, Math.min(marks, 5) - 1, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, EFFECT_TICKS, 0, false, true, true));
      if (target.level() instanceof ServerLevel level) {
         spawnAbyssalFlame(level, target, marks);
      }

      hurtInternally(target, player, (float)(target.getMaxHealth() * EXTRA_DAMAGE_PERCENT));
   }

   public static void applySummonedSwordMark(Player player, LivingEntity target) {
      if (target.level().isClientSide || !SlashBladeTargeting.canAttack(player, target)) {
         return;
      }

      int marks = addMark(player, target);
      target.addEffect(new MobEffectInstance(MobEffects.WITHER, EFFECT_TICKS, Math.min(marks, 5) - 1, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, EFFECT_TICKS, 0, false, true, true));
      if (target.level() instanceof ServerLevel level) {
         spawnAbyssalFlame(level, target, marks);
      }

      hurtInternally(target, player, (float)(target.getMaxHealth() * EXTRA_DAMAGE_PERCENT));
   }

   public static boolean isInternalDamage(LivingEntity target) {
      return target != null && INTERNAL_DAMAGE.contains(target.getUUID());
   }

   public static void clearPlayer(UUID playerId) {
      BLOOD_MARKS.remove(playerId);
   }

   @SubscribeEvent
   public static void onDeath(LivingDeathEvent event) {
      UUID targetId = event.getEntity().getUUID();
      for (Iterator<Map.Entry<UUID, Map<UUID, Integer>>> iterator = BLOOD_MARKS.entrySet().iterator(); iterator.hasNext();) {
         Map<UUID, Integer> marks = iterator.next().getValue();
         marks.remove(targetId);
         if (marks.isEmpty()) {
            iterator.remove();
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerLogout(PlayerLoggedOutEvent event) {
      clearPlayer(event.getEntity().getUUID());
   }

   @SubscribeEvent
   public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
      clearPlayer(event.getEntity().getUUID());
   }

   private static Player findNightfallAttacker(LivingHurtEvent event) {
      Entity source = event.getSource().getEntity();
      Entity directSource = event.getSource().getDirectEntity();
      if (!(source instanceof Player player)) {
         return null;
      }

      return hasDemonicBlood(player)
         && (NightfallDragonItemSupport.isDirectNightfallAttack(player, event.getSource())
         || directSource instanceof EntityAbstractSummonedSword
         || NightfallDragonItemSupport.isNightfallSlashEntityAttack(player, directSource))
            ? player
            : null;
   }

   private static boolean hasDemonicBlood(Player player) {
      ItemStack stack = NightfallDragonItemSupport.heldNightfallDragon(player);
      return !stack.isEmpty()
         && stack.getItem() instanceof ItemSlashBlade
         && stack.getCapability(ItemSlashBlade.BLADESTATE)
            .map(state -> state.hasSpecialEffect(ModSpecialEffects.DEMONIC_BLOOD_PARASITE.getId()))
            .orElse(false);
   }

   private static boolean hasMark(Player player, LivingEntity target) {
      Map<UUID, Integer> playerMarks = BLOOD_MARKS.get(player.getUUID());
      return playerMarks != null && playerMarks.getOrDefault(target.getUUID(), 0) > 0;
   }

   private static int addMark(Player player, LivingEntity target) {
      Map<UUID, Integer> playerMarks = BLOOD_MARKS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
      int marks = playerMarks.getOrDefault(target.getUUID(), 0) + 1;
      playerMarks.put(target.getUUID(), marks);
      return marks;
   }

   private static void convertToTrueDamage(LivingHurtEvent event, Player player, LivingEntity target) {
      float amount = event.getAmount();
      event.setAmount(0.0F);
      hurtInternally(target, player, amount);
   }

   private static void hurtInternally(LivingEntity target, Player player, float amount) {
      if (amount <= 0.0F || target.level().isClientSide) {
         return;
      }

      INTERNAL_DAMAGE.add(target.getUUID());
      try {
         target.invulnerableTime = 0;
         target.hurt(target.level().damageSources().indirectMagic(player, player), amount);
      } finally {
         INTERNAL_DAMAGE.remove(target.getUUID());
      }
   }

   private static void spawnAbyssalFlame(ServerLevel level, LivingEntity target, int marks) {
      Vec3 center = target.position();
      double radius = Math.max(0.85, target.getBbWidth() * (1.0 + marks * 0.02));
      BlockParticleOption cryingObsidian = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CRYING_OBSIDIAN.defaultBlockState());
      level.sendParticles(cryingObsidian, center.x, center.y + 0.04, center.z, 24, radius * 0.45, 0.03, radius * 0.45, 0.02);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y + 0.12, center.z, 18, radius * 0.55, 0.08, radius * 0.55, 0.015);
      for (int i = 0; i < 24; i++) {
         double angle = (Math.PI * 2.0) * i / 24.0;
         Vec3 pos = center.add(Math.cos(angle) * radius, 0.08, Math.sin(angle) * radius);
         level.sendParticles(cryingObsidian, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
      }
      BlockPos below = target.blockPosition().below();
      if (level.getBlockState(below).isAir()) {
         level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.2, center.z, 10, radius * 0.4, 0.08, radius * 0.4, 0.05);
      }
   }
}
