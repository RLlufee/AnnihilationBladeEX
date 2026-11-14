package qwq.qingyi.annihilationblade.loli_blade;

/**
 * 生物实体 Mixin 鸭子接口：通过 {@code DATA_HEALTH_ID} 同步强制压零生命值。
 */
public interface LoliBladeLivingEntityAccess {
   /** 通过 {@code EntityDataAccessor<Float>} 强制将生命同步数据置为 {@code 0.0F}。 */
   void annihilationblade$forceZeroHealth();
}
