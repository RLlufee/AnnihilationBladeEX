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
   /** 血狱 SA「炼狱·杀戮」领域（持续特效）。 */
   public static final int BLOOD_DOMAIN = 4;
   /** 血狱领域内的闪击斩（一次性）。direction 字段携带「终点 - 起点」位移。 */
   public static final int BLOOD_DASH = 5;
   /** 血狱十印幻影剑爆破（一次性）。intParam = 剑数 | 命中延迟 << 8。 */
   public static final int BLOOD_BURST = 6;
   /** 魔龙夜陨 SA「夜陨次元斩」整体领域与终幕（持续特效）。intParam = 总斩数。 */
   public static final int JUDGEMENT_DOMAIN = 7;
   /** 夜陨次元斩单次切痕（一次性）。intParam = 斩序号，seed 低 24 位携带刀色。 */
   public static final int JUDGEMENT_CUT = 8;
   /** 魔龙夜陨 SA「鳞之卫」（一次性，客户端按剑阵公式复现每柄剑的轨迹）。intParam = 剑数。 */
   public static final int SCALE_GUARD = 9;
   /** 萝莉刀审判（一次性）。intParam = 0 锥形（SE）/ 1 范围（SA）；entityIds = 被审判的目标。 */
   public static final int LOLI_JUDGEMENT = 10;
   /**
    * 湮灭之刃 · 终焉印记（普通命中的终焉判定，一次性，客户端并入同一个池化实例）。
    * radius = 目标体宽，scale = 目标体高，entityIds = [目标]。
    */
   public static final int TERMINUS_MARK = 11;
   /**
    * 湮灭之刃 SE「断空·瞬狱终闪」（持续特效）。intParam 低 4 位为种类：0 开场 / 1 闪现（高位为斩序号）。
    * 开场：origin = 起点胸口；闪现：origin = 落点胸口，direction = 落点 − 出发点，entityIds = [目标]。scale = 视觉倍率。
    */
   public static final int DANKONG = 12;
   /**
    * 湮灭之刃 SE「裂界·因果伤痕」（一次性，连锁时每层一个包）。origin = 伤口中心，radius = 牵引半径 × 视觉倍率，
    * scale = 视觉倍率，intParam = 当前层数 | 最大层数 << 4，entityIds = 被拖入伤口的目标。
    */
   public static final int WORLD_RIFT = 13;

   private FxTypes() {
   }
}
