package QWQ.QingYi.annihilationbladeex.mixin;

import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修复拔刀剑投射物从旧存档恢复时可能出现的落地状态不一致。
 *
 * <p>SlashBlade 会分别保存 {@code inGround} 与 {@code inBlockState}。当方块状态标签缺失、
 * 但落地标记仍为 true 时，其 tick 会直接对空方块状态调用 equals，导致存档在实体加载后崩溃。
 * 在原逻辑读取该字段前，用实体当前位置的实际方块状态补全它。</p>
 */
@Mixin(value = EntityAbstractSummonedSword.class, remap = false)
public abstract class EntityAbstractSummonedSwordMixin {
    @Shadow(remap = false)
    private boolean inGround;

    @Shadow(remap = false)
    private BlockState inBlockState;

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void annihilationbladeex$repairMissingInBlockState(CallbackInfo callbackInfo) {
        this.annihilationbladeex$repairMissingInBlockState();
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Ljava/lang/Object;equals(Ljava/lang/Object;)Z", ordinal = 0),
            remap = false
    )
    private boolean annihilationbladeex$compareInBlockStateSafely(Object instance, Object blockState) {
        if (instance == null && blockState instanceof BlockState currentBlockState) {
            this.inBlockState = currentBlockState;
            return true;
        }

        return instance != null && instance.equals(blockState);
    }

    private void annihilationbladeex$repairMissingInBlockState() {
        if (this.inGround && this.inBlockState == null) {
            EntityAbstractSummonedSword sword = (EntityAbstractSummonedSword) (Object) this;
            this.inBlockState = sword.level().getBlockState(sword.getOnPos());
        }
    }
}
