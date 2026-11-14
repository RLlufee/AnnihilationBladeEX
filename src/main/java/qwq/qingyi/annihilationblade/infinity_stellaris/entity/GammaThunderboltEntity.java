package qwq.qingyi.annihilationblade.infinity_stellaris.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 伽马雷暴专属落雷视觉实体，支持自定义主色调并在客户端触发天空闪光与雷鸣音效。
 */
public class GammaThunderboltEntity extends Entity {
    private static final EntityDataAccessor<Integer> FIXED_COLOR =
            SynchedEntityData.defineId(GammaThunderboltEntity.class, EntityDataSerializers.INT);

    public long seed;
    private int life;
    private int flashes;

    public GammaThunderboltEntity(EntityType<? extends GammaThunderboltEntity> entityType, Level level) {
        super(entityType, level);
        this.life = 2;
        this.seed = this.random.nextLong();
        this.flashes = this.random.nextInt(3) + 1;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.WEATHER;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.life == 2 && this.level().isClientSide()) {
            this.level().playLocalSound(
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.WEATHER,
                    10000.0F,
                    0.8F + this.random.nextFloat() * 0.2F,
                    false
            );
            this.level().playLocalSound(
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT,
                    SoundSource.WEATHER,
                    2.0F,
                    0.5F + this.random.nextFloat() * 0.2F,
                    false
            );
        }

        this.life--;
        if (this.life < 0) {
            if (this.flashes == 0) {
                this.discard();
            } else if (this.life < -this.random.nextInt(10)) {
                this.flashes--;
                this.life = 1;
                this.seed = this.random.nextLong();
            }
        }

        if (this.life >= 0 && this.level().isClientSide()) {
            this.level().setSkyFlashTime(2);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double viewDistance = 64.0D * getViewScale();
        return distance < viewDistance * viewDistance;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    public int getFixedColor() {
        return this.entityData.get(FIXED_COLOR);
    }

    public void setFixedColor(int color) {
        this.entityData.set(FIXED_COLOR, color);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FIXED_COLOR, -1);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setFixedColor(input.getIntOr("FixedColor", -1));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("FixedColor", this.getFixedColor());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity trackerEntry) {
        return super.getAddEntityPacket(trackerEntry);
    }
}
