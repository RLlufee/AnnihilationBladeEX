package QWQ.QingYi.annihilationblade.mixin;

import QWQ.QingYi.annihilationblade.config.ModConfig;
import QWQ.QingYi.annihilationblade.loli_blade.LoliBladeProtectionManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 方法级不可杀：对受保护的萝莉之刃持有者，在字节码层拦截整条死亡链
 * （hurt / die / actuallyHurt / kill / tickDeath）。
 * <p>
 * 这是 Forge 事件层（{@code LivingAttackEvent} / {@code LivingHurtEvent} / {@code LivingDeathEvent}）
 * 永远做不到的——事件可被更底层的代码绕开，而 Mixin 堵的是方法入口本身。
 * {@code priority = Integer.MAX_VALUE} 确保我们的取消优先于其它 Mixin。
 */
@Mixin(value = LivingEntity.class, priority = Integer.MAX_VALUE)
public abstract class LoliBladeLivingEntityMixin {

   @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
   private void loliInvincibleHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci) {
      if (isProtected()) {
         ci.cancel();
         ci.setReturnValue(false);
      }
   }

   @Inject(method = "die", at = @At("HEAD"), cancellable = true)
   private void loliInvincibleDie(DamageSource source, CallbackInfo ci) {
      if (isProtected()) {
         ci.cancel();
      }
   }

   @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
   private void loliInvincibleActuallyHurt(DamageSource source, float amount, CallbackInfo ci) {
      if (isProtected()) {
         ci.cancel();
      }
   }

   @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
   private void loliInvincibleKill(CallbackInfo ci) {
      if (isProtected()) {
         ci.cancel();
      }
   }

   @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
   private void loliInvincibleTickDeath(CallbackInfo ci) {
      if (isProtected()) {
         ci.cancel();
      }
   }

   private boolean isProtected() {
      // Mixin 中的 this 是 Mixin 类本身，需先转回目标类型再做 instanceof 判定。
      LivingEntity self = (LivingEntity) (Object) this;
      if (!(self instanceof Player player)) {
         return false;
      }
      // 配置开关关闭时退化为原事件层防御
      if (!ModConfig.COMMON.loliBlade.ultimateInvincible.getValue()) {
         return false;
      }
      return LoliBladeProtectionManager.isProtected(player.getUUID());
   }
}
