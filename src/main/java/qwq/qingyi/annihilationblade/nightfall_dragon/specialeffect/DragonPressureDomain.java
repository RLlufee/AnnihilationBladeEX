package qwq.qingyi.annihilationblade.nightfall_dragon.specialeffect;

import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import qwq.qingyi.annihilationblade.common.SlashBladeStateSupport;
import qwq.qingyi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;
import qwq.qingyi.annihilationblade.registry.ModSpecialEffects;

/**
 * 龙威领域：手持具备该潜能的夜临玄龙时，持续赋予持有者强力增益并释放龙息粒子环。
 */
public class DragonPressureDomain extends SpecialEffect {
    private static final int BUFF_TICKS = 60;
    private static final int AMPLIFIER_III = 2;
    private static final int AURA_INTERVAL_TICKS = 20;
    private static final int AURA_DRAGON_BREATH_PARTICLES = 10;
    private static final int AURA_ENCHANT_PARTICLES = 15;

    public DragonPressureDomain() {
        super(0, false, false);
    }

    public static void onPlayerTick(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        ItemStack stack = NightfallDragonItemSupport.heldNightfallDragon(player);
        if (stack.isEmpty() || !hasDragonPressure(stack)) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.SPEED, BUFF_TICKS, AMPLIFIER_III, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, BUFF_TICKS, AMPLIFIER_III, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, AMPLIFIER_III, false, true, true));
        if (!hasFinalGodBody(stack)) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, BUFF_TICKS, AMPLIFIER_III, false, true, true));
        }

        if (player.tickCount % AURA_INTERVAL_TICKS == 0 && player.level() instanceof ServerLevel level) {
            level.sendParticles(
                    PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
                    player.getX(),
                    player.getY() + 0.9D,
                    player.getZ(),
                    AURA_DRAGON_BREATH_PARTICLES,
                    0.85D,
                    0.5D,
                    0.85D,
                    0.025D
            );
            level.sendParticles(
                    ParticleTypes.ENCHANT,
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    AURA_ENCHANT_PARTICLES,
                    1.05D,
                    0.65D,
                    1.05D,
                    0.08D
            );
        }
    }

    private static boolean hasDragonPressure(ItemStack stack) {
        return SlashBladeStateSupport.isSlashBlade(stack)
                && SlashBladeStateSupport.state(stack)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.DRAGON_PRESSURE_DOMAIN_ID))
                .orElse(false);
    }

    private static boolean hasFinalGodBody(ItemStack stack) {
        return SlashBladeStateSupport.isSlashBlade(stack)
                && SlashBladeStateSupport.state(stack)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.DRAGON_GOD_BODY_ID))
                .orElse(false);
    }
}
