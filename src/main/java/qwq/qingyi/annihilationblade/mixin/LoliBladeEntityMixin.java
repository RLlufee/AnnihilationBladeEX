package qwq.qingyi.annihilationblade.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qwq.qingyi.annihilationblade.config.ModConfig;
import qwq.qingyi.annihilationblade.loli_blade.LoliBladeEntityAccess;
import qwq.qingyi.annihilationblade.loli_blade.LoliBladeProtectionManager;

/**
 * 拦截受保护玩家被 {@code remove(KILLED)} 硬移除，提供 {@code unsetRemoved} 与更新抑制能力（替代反射）。
 * <p>
 * 仅拦截 {@code KILLED}（不拦截 {@code DISCARDED} / {@code CHANGED_DIMENSION}），
 * 以免影响正常登出与维度切换。
 */
@Mixin(value = Entity.class, priority = Integer.MAX_VALUE)
public abstract class LoliBladeEntityMixin implements LoliBladeEntityAccess {

   @Unique
   private boolean annihilationblade$updateSuppressed;

   @Override
   @Invoker("unsetRemoved")
   public abstract void annihilationblade$unsetRemoved();

   @Override
   public boolean annihilationblade$isUpdateSuppressed() {
      return this.annihilationblade$updateSuppressed;
   }

   @Override
   public void annihilationblade$setUpdateSuppressed(boolean suppressed) {
      this.annihilationblade$updateSuppressed = suppressed;
   }

   @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
   private void annihilationblade$loliBlockRemove(Entity.RemovalReason reason, CallbackInfo ci) {
      Entity self = (Entity) (Object) this;
      if (reason == Entity.RemovalReason.KILLED
         && self instanceof Player player
         && ModConfig.COMMON.loliBlade.ultimateInvincible.getValue()
         && LoliBladeProtectionManager.isProtected(player.getUUID())) {
         ci.cancel();
      }
   }

   @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
   private void annihilationblade$suppressTick(CallbackInfo ci) {
      if (this.annihilationblade$updateSuppressed) {
         ci.cancel();
      }
   }

   @Inject(method = "baseTick", at = @At("HEAD"), cancellable = true)
   private void annihilationblade$suppressBaseTick(CallbackInfo ci) {
      if (this.annihilationblade$updateSuppressed) {
         ci.cancel();
      }
   }
}
