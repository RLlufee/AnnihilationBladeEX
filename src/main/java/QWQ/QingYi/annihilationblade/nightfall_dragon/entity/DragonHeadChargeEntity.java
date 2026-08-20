package QWQ.QingYi.annihilationblade.nightfall_dragon.entity;

import QWQ.QingYi.annihilationblade.Annihilationblade;
import QWQ.QingYi.annihilationblade.common.SlashBladeTargeting;
import QWQ.QingYi.annihilationblade.registry.ModEntities;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;

public class DragonHeadChargeEntity extends Entity {
   public static final double MAX_DISTANCE = 200.0D;
   public static final double SPEED = 4.0D;
   private static final double HIT_RADIUS = 3.2D;
   private static final double FRONT_OFFSET = 3.8D;
   private static final float DAMAGE = 2000.0F;
   private static final int ROAR_INTERVAL = 18;

   private final Set<UUID> damagedTargets = new HashSet<>();
   private final Set<UUID> carriedTargets = new HashSet<>();
   private UUID ownerId;
   private double travelled;

   public DragonHeadChargeEntity(EntityType<? extends DragonHeadChargeEntity> entityType, Level level) {
      super(entityType, level);
      this.noCulling = true;
   }

   public static DragonHeadChargeEntity create(ServerLevel level, ServerPlayer owner, Vec3 direction) {
      Vec3 normalized = normalize(direction);
      DragonHeadChargeEntity head = ModEntities.DRAGON_HEAD_CHARGE.get().create(level);
      if (head == null) {
         return null;
      }

      Vec3 spawn = owner.getEyePosition().add(normalized.scale(3.0D));
      head.ownerId = owner.getUUID();
      head.setPos(spawn.x, spawn.y - 0.2D, spawn.z);
      head.setDeltaMovement(normalized.scale(SPEED));
      head.setRotFromDirection(normalized);
      return head;
   }

   public static DragonHeadChargeEntity createInstance(PlayMessages.SpawnEntity packet, Level level) {
      Annihilationblade.LOGGER.info("Client created dragon_head_charge entity from Forge spawn packet");
      return new DragonHeadChargeEntity(ModEntities.DRAGON_HEAD_CHARGE.get(), level);
   }

   @Override
   public void tick() {
      super.tick();
      Vec3 direction = currentDirection();
      Vec3 previous = this.position();
      Vec3 movement = direction.scale(SPEED);
      Vec3 next = previous.add(movement);

      this.setDeltaMovement(movement);
      this.setPos(next.x, next.y, next.z);
      this.setRotFromDirection(direction);
      this.travelled += movement.length();

      if (!this.level().isClientSide) {
         carryTargets(direction);
         hitTargets(previous, next, direction);
         playTravelRoar();
      }

      if (this.travelled >= MAX_DISTANCE || this.tickCount > 80) {
         this.discard();
      }
   }

   @Override
   protected void defineSynchedData() {
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) {
         this.ownerId = tag.getUUID("Owner");
      }

      this.travelled = tag.getDouble("Travelled");
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }

      tag.putDouble("Travelled", this.travelled);
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return NetworkHooks.getEntitySpawningPacket(this);
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double distance) {
      double viewDistance = 256.0D * getViewScale();
      return distance < viewDistance * viewDistance;
   }

   private void hitTargets(Vec3 previous, Vec3 next, Vec3 direction) {
      PlayerOwner owner = resolveOwner();
      if (owner == null) {
         return;
      }

      AABB sweep = new AABB(previous, next).inflate(HIT_RADIUS);
      List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, sweep, target -> SlashBladeTargeting.canAttack(owner.player(), target));
      for (LivingEntity target : targets) {
         double radius = HIT_RADIUS + Math.max(target.getBbWidth(), target.getBbHeight()) * 0.5D;
         if (distanceToSegmentSqr(target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D), previous, next) > radius * radius) {
            continue;
         }

         UUID targetId = target.getUUID();
         if (this.damagedTargets.add(targetId)) {
            target.hurt(this.level().damageSources().indirectMagic(this, owner.player()), DAMAGE);
         }

         this.carriedTargets.add(targetId);
         moveTargetWithHead(target, direction);
      }
   }

   private void carryTargets(Vec3 direction) {
      PlayerOwner owner = resolveOwner();
      if (owner == null) {
         this.carriedTargets.clear();
         return;
      }

      this.carriedTargets.removeIf(targetId -> {
         Entity entity = ((ServerLevel)this.level()).getEntity(targetId);
         if (!(entity instanceof LivingEntity target) || !target.isAlive() || !SlashBladeTargeting.canAttack(owner.player(), target)) {
            return true;
         }

         moveTargetWithHead(target, direction);
         return false;
      });
   }

   private void moveTargetWithHead(LivingEntity target, Vec3 direction) {
      Vec3 front = this.position().add(direction.scale(FRONT_OFFSET));
      target.setPos(front.x, front.y - target.getBbHeight() * 0.45D, front.z);
      target.setDeltaMovement(direction.scale(SPEED));
      target.hurtMarked = true;
      target.fallDistance = 0.0F;
   }

   private void playTravelRoar() {
      if (this.tickCount == 1 || this.tickCount % ROAR_INTERVAL == 0) {
         this.level()
            .playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0F, 0.75F);
      }
   }

   private PlayerOwner resolveOwner() {
      if (!(this.level() instanceof ServerLevel serverLevel) || this.ownerId == null) {
         return null;
      }

      Entity entity = serverLevel.getEntity(this.ownerId);
      return entity instanceof ServerPlayer player && player.isAlive() ? new PlayerOwner(player) : null;
   }

   private Vec3 currentDirection() {
      return normalize(this.getDeltaMovement());
   }

   private void setRotFromDirection(Vec3 direction) {
      double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
      this.setYRot((float)(Mth.atan2(direction.x, direction.z) * Mth.RAD_TO_DEG));
      this.setXRot((float)(-Mth.atan2(direction.y, horizontal) * Mth.RAD_TO_DEG));
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
   }

   private static Vec3 normalize(Vec3 direction) {
      if (direction.lengthSqr() < 1.0E-6D) {
         return new Vec3(0.0D, 0.0D, 1.0D);
      }

      return direction.normalize();
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

   private record PlayerOwner(ServerPlayer player) {
   }
}
