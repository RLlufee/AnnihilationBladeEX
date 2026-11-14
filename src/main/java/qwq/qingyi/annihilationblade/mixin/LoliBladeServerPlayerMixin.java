package qwq.qingyi.annihilationblade.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qwq.qingyi.annihilationblade.config.ModConfig;
import qwq.qingyi.annihilationblade.loli_blade.LoliBladeProtectionManager;

/**
 * {@link ServerPlayer} 覆盖了 {@code hurtServer / die}，同步在入口处拦截受保护玩家的受击与死亡。
 */
@Mixin(value = ServerPlayer.class, priority = Integer.MAX_VALUE)
public abstract class LoliBladeServerPlayerMixin {

   @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
   private void annihilationblade$loliServerPlayerHurtServer(
      ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir
   ) {
      if (annihilationblade$isProtected()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "die", at = @At("HEAD"), cancellable = true)
   private void annihilationblade$loliServerPlayerDie(DamageSource source, CallbackInfo ci) {
      if (annihilationblade$isProtected()) {
         ci.cancel();
      }
   }

   private boolean annihilationblade$isProtected() {
      ServerPlayer self = (ServerPlayer) (Object) this;
      return ModConfig.COMMON.loliBlade.ultimateInvincible.getValue()
         && LoliBladeProtectionManager.isProtected(self.getUUID());
   }
}
