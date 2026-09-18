package com.gai.mod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * AI 本体（HIM 造型分身）：静止、不可推动、高血量、免疫火焰。
 */
public class GaiAvatarEntity extends Mob {

    public GaiAvatarEntity(EntityType<? extends GaiAvatarEntity> type, Level level) {
        super(type, level);
        this.setNoAi(false);
        this.setPersistenceRequired();
        this.fireImmune();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void checkDespawn() {
        // 永不消失
    }

    @Override
    protected void registerGoals() {
        // 无目标，AI 本体保持站立
    }
}
