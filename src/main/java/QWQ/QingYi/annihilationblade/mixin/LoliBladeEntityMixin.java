package QWQ.QingYi.annihilationblade.mixin;

import QWQ.QingYi.annihilationblade.config.ModConfig;
import QWQ.QingYi.annihilationblade.loli_blade.LoliBladeProtectionManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截受保护玩家被 {@code remove(KILLED)} 硬移除，作为忒修斯重建之前的最后一道闸门。
 * <p>
 * 这是 v1/v2 分析中标注的盲区——其它 god-mod（如 Endless 的 swordKill）直接
 * {@code target.kill(); target.remove(KILLED)} 时，事件层完全无感知。此处从方法入口堵死。
 * 仅拦截 {@code KILLED}（不拦截 {@code DISCARDED} / {@code CHANGED_DIMENSION}），
 * 以免影响正常登出与维度切换。
 */
@Mixin(value = Entity.class, priority = Integer.MAX_VALUE)
public abstract class LoliBladeEntityMixin {

   @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
   private void loliBlockRemove(Entity.RemovalReason reason, CallbackInfo ci) {
      // Mixin 中的 this 是 Mixin 类本身，需先转回目标类型再做 instanceof 判定。
      Entity self = (Entity) (Object) this;
      if (reason == Entity.RemovalReason.KILLED
            && self instanceof Player player
            && ModConfig.COMMON.loliBlade.ultimateInvincible.getValue()
            && LoliBladeProtectionManager.isProtected(player.getUUID())) {
         ci.cancel();
      }
   }
}
