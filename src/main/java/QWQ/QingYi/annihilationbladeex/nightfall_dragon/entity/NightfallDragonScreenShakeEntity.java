package QWQ.QingYi.annihilationbladeex.nightfall_dragon.entity;

import QWQ.QingYi.annihilationbladeex.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class NightfallDragonScreenShakeEntity extends Entity {
   private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(NightfallDragonScreenShakeEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> MAGNITUDE = SynchedEntityData.defineId(NightfallDragonScreenShakeEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(NightfallDragonScreenShakeEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> FADE_DURATION = SynchedEntityData.defineId(NightfallDragonScreenShakeEntity.class, EntityDataSerializers.INT);

   public NightfallDragonScreenShakeEntity(EntityType<? extends NightfallDragonScreenShakeEntity> type, Level level) {
      super(type, level);
   }

   public static void spawn(ServerLevel level, Vec3 position, float radius, float magnitude, int duration, int fadeDuration) {
      NightfallDragonScreenShakeEntity shake = ModEntities.NIGHTFALL_SCREEN_SHAKE.get().create(level);
      if (shake == null) {
         return;
      }

      shake.setRadius(radius);
      shake.setMagnitude(magnitude);
      shake.setDuration(duration);
      shake.setFadeDuration(fadeDuration);
      shake.setPos(position.x, position.y, position.z);
      level.addFreshEntity(shake);
   }

   @OnlyIn(Dist.CLIENT)
   public float getShakeAmount(Player player, float partialTick) {
      float age = this.tickCount + partialTick;
      float fade = 1.0F - (age - getDuration()) / (getFadeDuration() + 1.0F);
      float base = age < getDuration() ? getMagnitude() : fade * fade * getMagnitude();
      Vec3 eye = player.getEyePosition(partialTick);
      float distanceScale = (float)(1.0D - Mth.clamp(this.position().distanceTo(eye) / getRadius(), 0.0D, 1.0D));
      return base * distanceScale * distanceScale;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.tickCount > getDuration() + getFadeDuration()) {
         this.discard();
      }
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(RADIUS, 10.0F);
      builder.define(MAGNITUDE, 0.08F);
      builder.define(DURATION, 0);
      builder.define(FADE_DURATION, 8);
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      setRadius(tag.getFloat("Radius"));
      setMagnitude(tag.getFloat("Magnitude"));
      setDuration(tag.getInt("Duration"));
      setFadeDuration(tag.getInt("FadeDuration"));
      this.tickCount = tag.getInt("Age");
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putFloat("Radius", getRadius());
      tag.putFloat("Magnitude", getMagnitude());
      tag.putInt("Duration", getDuration());
      tag.putInt("FadeDuration", getFadeDuration());
      tag.putInt("Age", this.tickCount);
   }

   @Override
   public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity trackerEntry) {
      return super.getAddEntityPacket(trackerEntry);
   }

   public float getRadius() {
      return this.entityData.get(RADIUS);
   }

   public void setRadius(float radius) {
      this.entityData.set(RADIUS, Math.max(1.0F, radius));
   }

   public float getMagnitude() {
      return this.entityData.get(MAGNITUDE);
   }

   public void setMagnitude(float magnitude) {
      this.entityData.set(MAGNITUDE, Math.max(0.0F, magnitude));
   }

   public int getDuration() {
      return this.entityData.get(DURATION);
   }

   public void setDuration(int duration) {
      this.entityData.set(DURATION, Math.max(0, duration));
   }

   public int getFadeDuration() {
      return this.entityData.get(FADE_DURATION);
   }

   public void setFadeDuration(int fadeDuration) {
      this.entityData.set(FADE_DURATION, Math.max(1, fadeDuration));
   }
}
