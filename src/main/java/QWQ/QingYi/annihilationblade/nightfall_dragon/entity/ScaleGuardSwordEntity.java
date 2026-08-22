package QWQ.QingYi.annihilationblade.nightfall_dragon.entity;

import QWQ.QingYi.annihilationblade.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationblade.nightfall_dragon.NightfallDragonDefinitions;
import QWQ.QingYi.annihilationblade.nightfall_dragon.item.NightfallDragonItemSupport;
import QWQ.QingYi.annihilationblade.registry.ModEntities;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mods.flammpfeil.slashblade.SlashBlade.RegistryEvents;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

/**
 * 【鳞之卫】单柄剑的服务端控制器。
 *
 * 这个实体不负责渲染；可见部分由重锋原生 EntityAbstractSummonedSword 承担。
 */
public class ScaleGuardSwordEntity extends Entity {
   private static final EntityDataAccessor<Integer> SWORD_INDEX = SynchedEntityData.defineId(ScaleGuardSwordEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> SWORD_COUNT = SynchedEntityData.defineId(ScaleGuardSwordEntity.class, EntityDataSerializers.INT);

   // 【鳞之卫】调参区：需要改手感时优先改这里。
   public static final int ORBIT_TICKS = 40;                 // 绕旋阶段持续时间（单位：Tick，1秒 = 20 Ticks）。在此期间幻影剑围绕玩家旋转。
   public static final int EXPAND_TICKS = 10;                // 向外扩张/展开阶段持续时间。剑从玩家身旁的绕旋半径往外推到最远端。
   public static final int HOLD_TICKS = 10;                  // 悬停/维持阶段持续时间。剑在扩张后的位置静止悬浮。
   public static final int RISE_TICKS = 8;                   // 上升阶段持续时间。剑快速向上拔高升空。
   public static final int SLAM_TICKS = 5;                   // 猛砸阶段持续时间。剑从高空以极快速度垂直砸向地面。
   public static final double SWORD_SIZE_SCALE = 2.0D;       // 幻影剑的大小缩放比例。此值会等比例缩放渲染尺寸、碰撞判定半径和高度偏置。
   public static final double ORBIT_RADIUS = 2.4D;           // 绕旋阶段幻影剑围绕玩家旋转的初始半径。
   public static final double OUTWARD_DISTANCE = 3.0D;       // 向外扩张时增加的额外距离（即最终展开半径 = ORBIT_RADIUS + OUTWARD_DISTANCE）。
   public static final double SWORD_HEIGHT = 1.35D;          // 绕旋阶段幻影剑相对于玩家足底的垂直高度（Y轴偏移）。
   public static final double RISE_HEIGHT = 7.5D;            // 升空阶段幻影剑最大抬升的高度值。
   public static final double TOUCH_RADIUS = 2.0D;          // 幻影剑在运动/扫击过程中与目标发生接触判定（蹭伤）的碰撞半径。
   public static final double IMPACT_RADIUS = 5.0D;          // 猛砸落地爆炸伤害的判定半径范围。
   public static final float TOUCH_DAMAGE = 24.0F;           // 运动接触时的固定基础魔法伤害。
   public static final float PANEL_DAMAGE_MULTIPLIER = 10.0F; // 猛砸爆炸的总伤害中，基于面板伤害的倍率因子。
   public static final float MAX_HEALTH_DAMAGE_RATIO = 0.10F;// 猛砸爆炸附加的目标最大生命值伤害比例（如 0.10 即 10% 最大生命值百分比伤害）。
   public static final int TOUCH_DAMAGE_INTERVAL = 1;        // 接触伤害对同一个目标的无敌帧/冷却间隔（单位：Tick）。
   public static final float ORBIT_SPEED_START_DEGREES = 7.0F;   // 绕旋旋转的初始角速度（度/Tick）。
   public static final float ORBIT_SPEED_ACCEL_DEGREES = 22.0F;  // 绕旋阶段旋转的角加速度，使幻影剑随时间推移加速旋转。
   public static final float SWORD_ROLL_SPEED_DEGREES = 18.0F;   // 剑身自身横滚（Roll）自转旋转的速度（度/Tick）。
   public static final float SWORD_ROLL_OFFSET_DEGREES = 31.0F;  // 多把幻影剑之间在横滚角度上的初始相位偏置，使其呈现错落交叠的效果。
   public static final int SWORD_COLOR = 0xDA43FF;           // 幻影剑渲染颜色（ARGB十六进制，此值代表某种亮粉紫色）。
   private static final int TOTAL_LIFE = ORBIT_TICKS + EXPAND_TICKS + HOLD_TICKS + RISE_TICKS + SLAM_TICKS + 8; // 实体总生命周期（各阶段耗时总和加上8个Tick的消失渐变缓冲）。
   private static final Vec3 UPRIGHT_DIRECTION = new Vec3(0.0D, 1.0D, 0.0D); // 剑尖朝上的垂直朝向向量。
   private static final Vec3 SLAM_DIRECTION = new Vec3(0.0D, -1.0D, 0.0D); // 剑尖朝下的垂直下砸朝向向量。

   private final Map<UUID, Long> touchCooldowns = new HashMap<>();
   private UUID ownerId;
   private UUID visualSwordId;
   private Vec3 lockedCenter;
   private boolean impacted;

   public ScaleGuardSwordEntity(EntityType<? extends ScaleGuardSwordEntity> entityType, Level level) {
      super(entityType, level);
      this.noCulling = true;
   }

   public static ScaleGuardSwordEntity create(ServerLevel level, ServerPlayer owner, int index, int swordCount) {
      ScaleGuardSwordEntity controller = ModEntities.SCALE_GUARD_SWORD.get().create(level);
      if (controller == null) {
         return null;
      }

      controller.ownerId = owner.getUUID();
      controller.entityData.set(SWORD_INDEX, index);
      controller.entityData.set(SWORD_COUNT, swordCount);
      Vec3 position = controller.computeOrbitPosition(owner, 0.0F);
      controller.setPos(position.x, position.y, position.z);
      controller.setRotFromRadial(position.subtract(owner.position()));
      return controller;
   }

   public static ScaleGuardSwordEntity createInstance(PlayMessages.SpawnEntity packet, Level level) {
      return new ScaleGuardSwordEntity(ModEntities.SCALE_GUARD_SWORD.get(), level);
   }

   @Override
   public void tick() {
      super.tick();
      Vec3 previous = this.position();
      Vec3 next = computeNextPosition();
      this.setPos(next.x, next.y, next.z);
      this.setDeltaMovement(next.subtract(previous));
      this.setRotFromRadial(radialFromCenter(next));

      if (!this.level().isClientSide) {
         syncVisualSummonedSword(previous, next);
         damageTouchedTargets(previous, next);
         if (!this.impacted && this.tickCount >= ORBIT_TICKS + EXPAND_TICKS + HOLD_TICKS + RISE_TICKS + SLAM_TICKS) {
            this.impacted = true;
            impact();
         }
      }

      if (this.tickCount >= TOTAL_LIFE) {
         discardVisualSummonedSword();
         this.discard();
      }
   }

   @Override
   public void remove(RemovalReason reason) {
      if (!this.level().isClientSide) {
         discardVisualSummonedSword();
      }

      super.remove(reason);
   }

   @Override
   protected void defineSynchedData() {
      this.entityData.define(SWORD_INDEX, 0);
      this.entityData.define(SWORD_COUNT, 16);
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) {
         this.ownerId = tag.getUUID("Owner");
      }

      if (tag.hasUUID("VisualSword")) {
         this.visualSwordId = tag.getUUID("VisualSword");
      }

      this.entityData.set(SWORD_INDEX, tag.getInt("SwordIndex"));
      this.entityData.set(SWORD_COUNT, Math.max(1, tag.getInt("SwordCount")));
      if (tag.contains("LockedX")) {
         this.lockedCenter = new Vec3(tag.getDouble("LockedX"), tag.getDouble("LockedY"), tag.getDouble("LockedZ"));
      }

      this.impacted = tag.getBoolean("Impacted");
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }

      if (this.visualSwordId != null) {
         tag.putUUID("VisualSword", this.visualSwordId);
      }

      tag.putInt("SwordIndex", getSwordIndex());
      tag.putInt("SwordCount", getSwordCount());
      if (this.lockedCenter != null) {
         tag.putDouble("LockedX", this.lockedCenter.x);
         tag.putDouble("LockedY", this.lockedCenter.y);
         tag.putDouble("LockedZ", this.lockedCenter.z);
      }

      tag.putBoolean("Impacted", this.impacted);
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return NetworkHooks.getEntitySpawningPacket(this);
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      double viewDistance = 160.0D * getViewScale();
      return distance < viewDistance * viewDistance;
   }

   public int getSwordIndex() {
      return this.entityData.get(SWORD_INDEX);
   }

   public int getSwordCount() {
      return Math.max(1, this.entityData.get(SWORD_COUNT));
   }

   private Vec3 computeNextPosition() {
      ServerPlayer owner = resolveOwner();
      if (this.tickCount < ORBIT_TICKS && owner != null) {
         return computeOrbitPosition(owner, 0.0F);
      }

      ensureLockedCenter(owner);
      Vec3 center = this.lockedCenter != null ? this.lockedCenter : this.position();
      double baseAngle = baseAngle();
      Vec3 radial = new Vec3(Math.cos(baseAngle), 0.0D, Math.sin(baseAngle));
      Vec3 orbitPoint = center.add(radial.scale(ORBIT_RADIUS * SWORD_SIZE_SCALE));
      Vec3 expandedPoint = center.add(radial.scale((ORBIT_RADIUS + OUTWARD_DISTANCE) * SWORD_SIZE_SCALE));

      int expandEnd = ORBIT_TICKS + EXPAND_TICKS;
      int holdEnd = expandEnd + HOLD_TICKS;
      int riseEnd = holdEnd + RISE_TICKS;

      if (this.tickCount < expandEnd) {
         double progress = smooth((this.tickCount - ORBIT_TICKS + 1.0D) / EXPAND_TICKS);
         return orbitPoint.lerp(expandedPoint, progress);
      }

      if (this.tickCount < holdEnd) {
         return expandedPoint;
      }

      if (this.tickCount < riseEnd) {
         double progress = smooth((this.tickCount - holdEnd + 1.0D) / RISE_TICKS);
         return expandedPoint.add(0.0D, RISE_HEIGHT * SWORD_SIZE_SCALE * progress, 0.0D);
      }

      double progress = smooth((this.tickCount - riseEnd + 1.0D) / SLAM_TICKS);
      return expandedPoint.add(0.0D, RISE_HEIGHT * SWORD_SIZE_SCALE * (1.0D - progress) - 0.75D * progress, 0.0D);
   }

   private Vec3 computeOrbitPosition(ServerPlayer owner, float partialTicks) {
      double age = this.tickCount + partialTicks;
      double acceleration = age / Math.max(1.0D, ORBIT_TICKS);
      double angle = baseAngle() + Math.toRadians(age * (ORBIT_SPEED_START_DEGREES + ORBIT_SPEED_ACCEL_DEGREES * acceleration));
      return owner.position().add(Math.cos(angle) * ORBIT_RADIUS * SWORD_SIZE_SCALE, SWORD_HEIGHT * SWORD_SIZE_SCALE, Math.sin(angle) * ORBIT_RADIUS * SWORD_SIZE_SCALE);
   }

   private void syncVisualSummonedSword(Vec3 previous, Vec3 next) {
      if (!(this.level() instanceof ServerLevel level)) {
         return;
      }

      ServerPlayer owner = resolveOwner();
      if (owner == null) {
         discardVisualSummonedSword();
         return;
      }

      EntityAbstractSummonedSword sword = getOrCreateVisualSummonedSword(level, owner);
      Vec3 direction = visualSwordDirection();
      sword.setColor(SWORD_COLOR);
      sword.setNoClip(true);
      sword.setDamage(0.0D);
      sword.setPierce((byte)0);
      sword.setDelay(Math.max(8, TOTAL_LIFE - this.tickCount + 8));
      sword.setDeltaMovement(Vec3.ZERO);
      sword.moveTo(next.x, next.y, next.z, yawToFace(direction), pitchToFace(direction));
      sword.setRoll(getSwordIndex() * SWORD_ROLL_OFFSET_DEGREES + this.tickCount * SWORD_ROLL_SPEED_DEGREES);
      sword.hasImpulse = true;
   }

   private Vec3 visualSwordDirection() {
      int slamStart = ORBIT_TICKS + EXPAND_TICKS + HOLD_TICKS + RISE_TICKS;
      return this.tickCount >= slamStart ? SLAM_DIRECTION : UPRIGHT_DIRECTION;
   }

   private EntityAbstractSummonedSword getOrCreateVisualSummonedSword(ServerLevel level, ServerPlayer owner) {
      if (this.visualSwordId != null && level.getEntity(this.visualSwordId) instanceof EntityAbstractSummonedSword sword && sword.isAlive()) {
         return sword;
      }

      EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(RegistryEvents.SummonedSword, level);
      sword.setOwner(owner);
      sword.setShooter(owner);
      sword.setNoClip(true);
      sword.setDamage(0.0D);
      sword.setPierce((byte)0);
      sword.setDelay(TOTAL_LIFE + 8);
      sword.setColor(SWORD_COLOR);
      sword.setRoll(getSwordIndex() * SWORD_ROLL_OFFSET_DEGREES);
      sword.setPos(this.getX(), this.getY(), this.getZ());
      level.addFreshEntity(sword);
      this.visualSwordId = sword.getUUID();
      return sword;
   }

   private void discardVisualSummonedSword() {
      if (this.visualSwordId != null && this.level() instanceof ServerLevel level) {
         if (level.getEntity(this.visualSwordId) instanceof EntityAbstractSummonedSword sword) {
            sword.discard();
         }

         this.visualSwordId = null;
      }
   }

   private void ensureLockedCenter(ServerPlayer owner) {
      if (this.lockedCenter == null) {
         if (owner != null) {
            this.lockedCenter = owner.position().add(0.0D, SWORD_HEIGHT * SWORD_SIZE_SCALE, 0.0D);
         } else {
            this.lockedCenter = this.position();
         }
      }
   }

   private void damageTouchedTargets(Vec3 previous, Vec3 next) {
      ServerPlayer owner = resolveOwner();
      if (owner == null) {
         return;
      }

      long gameTime = this.level().getGameTime();
      AABB sweep = new AABB(previous, next).inflate(TOUCH_RADIUS * SWORD_SIZE_SCALE, 1.35D * SWORD_SIZE_SCALE, TOUCH_RADIUS * SWORD_SIZE_SCALE);
      List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, sweep, target -> SlashBladeTargeting.canAttack(owner, target));
      for (LivingEntity target : targets) {
         UUID targetId = target.getUUID();
         long nextAllowed = this.touchCooldowns.getOrDefault(targetId, Long.MIN_VALUE);
         if (gameTime < nextAllowed) {
            continue;
         }

         Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
         double radius = TOUCH_RADIUS * SWORD_SIZE_SCALE + Math.max(target.getBbWidth(), target.getBbHeight()) * 0.35D;
         if (distanceToSegmentSqr(targetCenter, previous, next) > radius * radius) {
            continue;
         }

         target.hurt(this.level().damageSources().indirectMagic(this, owner), TOUCH_DAMAGE);
         this.touchCooldowns.put(targetId, gameTime + TOUCH_DAMAGE_INTERVAL);
      }
   }

   private void impact() {
      ServerPlayer owner = resolveOwner();
      if (owner == null) {
         return;
      }

      float panelDamage = readPanelDamage(owner);
      AABB impactBox = this.getBoundingBox().inflate(IMPACT_RADIUS * SWORD_SIZE_SCALE, 1.4D * SWORD_SIZE_SCALE, IMPACT_RADIUS * SWORD_SIZE_SCALE);
      List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, impactBox, target -> SlashBladeTargeting.canAttack(owner, target));
      for (LivingEntity target : targets) {
         float damage = panelDamage * PANEL_DAMAGE_MULTIPLIER + (float)(target.getMaxHealth() * MAX_HEALTH_DAMAGE_RATIO);
         target.hurt(this.level().damageSources().indirectMagic(this, owner), damage);
         Vec3 push = target.position().subtract(this.position()).normalize().scale(0.55D).add(0.0D, 0.28D, 0.0D);
         target.push(push.x, push.y, push.z);
         target.hurtMarked = true;
      }

      if (this.level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 3, 0.35D * SWORD_SIZE_SCALE, 0.15D, 0.35D * SWORD_SIZE_SCALE, 0.0D);
         serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH, this.getX(), this.getY(), this.getZ(), 24, 0.75D * SWORD_SIZE_SCALE, 0.22D, 0.75D * SWORD_SIZE_SCALE, 0.08D);
         NightfallDragonScreenShakeEntity.spawn(serverLevel, this.position(), 18.0F, 0.13F, 2, 10);
      }

      this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.25F, 0.65F);
      this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.55F);
   }

   private float readPanelDamage(Player owner) {
      ItemStack stack = owner.getMainHandItem();
      if (!NightfallDragonItemSupport.isNightfallDragon(stack)) {
         stack = owner.getOffhandItem();
      }

      if (NightfallDragonItemSupport.isNightfallDragon(stack)) {
         return NightfallDragonDefinitions.BASE_ATTACK_DAMAGE;
      }

      return NightfallDragonDefinitions.BASE_ATTACK_DAMAGE;
   }

   private ServerPlayer resolveOwner() {
      if (!(this.level() instanceof ServerLevel serverLevel) || this.ownerId == null) {
         return null;
      }

      Entity entity = serverLevel.getEntity(this.ownerId);
      return entity instanceof ServerPlayer player && player.isAlive() ? player : null;
   }

   private Vec3 radialFromCenter(Vec3 position) {
      Vec3 center = this.lockedCenter != null ? this.lockedCenter : position.subtract(this.getDeltaMovement());
      Vec3 radial = position.subtract(center);
      if (radial.horizontalDistanceSqr() < 1.0E-6D) {
         double angle = baseAngle();
         return new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
      }

      return new Vec3(radial.x, 0.0D, radial.z).normalize();
   }

   private void setRotFromRadial(Vec3 radial) {
      double horizontal = Math.sqrt(radial.x * radial.x + radial.z * radial.z);
      if (horizontal < 1.0E-6D) {
         return;
      }

      float yaw = yawToFace(radial);
      this.setYRot(yaw);
      this.setXRot(0.0F);
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
   }

   private double baseAngle() {
      return (Math.PI * 2.0D) * getSwordIndex() / getSwordCount();
   }

   private static double smooth(double progress) {
      double clamped = Mth.clamp(progress, 0.0D, 1.0D);
      return clamped * clamped * (3.0D - 2.0D * clamped);
   }

   private static double distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
      Vec3 segment = end.subtract(start);
      double lengthSqr = segment.lengthSqr();
      if (lengthSqr < 1.0E-6D) {
         return point.distanceToSqr(start);
      }

      double t = Mth.clamp(point.subtract(start).dot(segment) / lengthSqr, 0.0D, 1.0D);
      return point.distanceToSqr(start.add(segment.scale(t)));
   }

   private static float yawToFace(Vec3 direction) {
      return (float)(Mth.atan2(direction.x, direction.z) * Mth.RAD_TO_DEG);
   }

   private static float pitchToFace(Vec3 direction) {
      double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
      return (float)(-Mth.atan2(direction.y, horizontal) * Mth.RAD_TO_DEG);
   }
}
