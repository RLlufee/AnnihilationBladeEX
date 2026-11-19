package QWQ.QingYi.annihilationblade.render;

/**
 * 特效事件类型编号（服务端与客户端共用）。
 * <p>
 * 只允许在末尾追加新编号，已有编号不可复用或改值，否则会造成新旧版本客户端解析错乱。
 */
public final class FxTypes {
   /** 湮灭之刃 SA「空间破碎」。 */
   public static final int SPATIAL_FRACTURE = 1;
   /** 无尽星空 SA「真空衰变·坍缩」（持续特效）。 */
   public static final int VACUUM_DECAY = 2;
   /** 魔龙夜陨 SA「神陨·宇宙夜陨降临」（持续特效）。 */
   public static final int COSMIC_DESCENT = 3;

   private FxTypes() {
   }
}
