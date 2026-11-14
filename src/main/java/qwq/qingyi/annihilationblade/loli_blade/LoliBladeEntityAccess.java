package qwq.qingyi.annihilationblade.loli_blade;

/**
 * 实体 Mixin 鸭子接口：替代 1.21.1 对 {@code unsetRemoved} 与 {@code canUpdate} 的反射访问。
 */
public interface LoliBladeEntityAccess {
   /** 调用 {@code Entity#unsetRemoved()} 恢复被移除实体的存在状态。 */
   void annihilationblade$unsetRemoved();

   /** 查询当前实体是否被萝莉之刃终极抹除禁止更新。 */
   boolean annihilationblade$isUpdateSuppressed();

   /** 设置当前实体是否禁止执行 tick/baseTick 更新。 */
   void annihilationblade$setUpdateSuppressed(boolean suppressed);
}
