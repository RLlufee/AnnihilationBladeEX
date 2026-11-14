package qwq.qingyi.annihilationblade.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qwq.qingyi.annihilationblade.config.ModConfig;
import qwq.qingyi.annihilationblade.loli_blade.LoliBladeEntityAccess;
import qwq.qingyi.annihilationblade.loli_blade.LoliBladeLivingEntityAccess;
import qwq.qingyi.annihilationblade.loli_blade.LoliBladeProtectionManager;

/**
 * 方法级不可杀与物理抹除支持：
 * 对受保护的萝莉之刃持有者，在字节码层拦截 {@code hurtServer / die / actuallyHurt / kill / tickDeath}；
 * 并暴露 {@code DATA_HEALTH_ID} 以供物理抹除强制置零生命同步数据。
 */
@Mixin(value = LivingEntity.class, priority = Integer.MAX_VALUE)
public abstract class LoliBladeLivingEntityMixin implements LoliBladeLivingEntityAccess {

   @Accessor("DATA_HEALTH_ID")
   static EntityDataAccessor<Float> annihilationblade$getDataHealthId() {
      throw new AssertionError();
   }

   @Override
   public void annihilationblade$forceZeroHealth() {
      LivingEntity self = (LivingEntity) (Object) this;
      self.getEntityData().set(annihilationblade$getDataHealthId(), 0.0F);
   }

   @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
   private void annihilationblade$loliInvincibleHurtServer(
      ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir
   ) {
      if (annihilationblade$isProtected()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "die", at = @At("HEAD"), cancellable = true)
   private void annihilationblade$loliInvincibleDie(DamageSource source, CallbackInfo ci) {
      if (annihilationblade$isProtected()) {
         ci.cancel();
      }
   }

   @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
   private void annihilationblade$loliInvincibleActuallyHurt(
      ServerLevel level, DamageSource source, float amount, CallbackInfo ci
   ) {
      if (annihilationblade$isProtected()) {
         ci.cancel();
      }
   }

   @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
   private void annihilationblade$loliInvincibleKill(ServerLevel level, CallbackInfo ci) {
      if (annihilationblade$isProtected()) {
         ci.cancel();
      }
   }

   @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
   private void annihilationblade$loliInvincibleTickDeath(CallbackInfo ci) {
      if (annihilationblade$isProtected()) {
         ci.cancel();
      }
   }

   @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
   private void annihilationblade$suppressLivingTick(CallbackInfo ci) {
      if (((LoliBladeEntityAccess) (Object) this).annihilationblade$isUpdateSuppressed()) {
         ci.cancel();
      }
   }

   private boolean annihilationblade$isProtected() {
      LivingEntity self = (LivingEntity) (Object) this;
      if (!(self instanceof Player player)) {
         return false;
      }
      if (!ModConfig.COMMON.loliBlade.ultimateInvincible.getValue()) {
         return false;
      }
      return LoliBladeProtectionManager.isProtected(player.getUUID());
   }
}
