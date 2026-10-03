package com.anotherstar.lolipickaxe.entity;

import com.anotherstar.lolipickaxe.asm.LoliEntityDefense;
import com.anotherstar.lolipickaxe.asm.LoliDefenseHooks;
import com.anotherstar.lolipickaxe.asm.LoliStructuralPurge;
import com.anotherstar.lolipickaxe.config.LoliConfig;
import com.anotherstar.lolipickaxe.item.LoliDispersalItem;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;

 
public final class LoliEntity extends PathfinderMob implements LoliDispersalItem.DispersibleLoli {
    private static final EntityDataAccessor<Boolean> DISPERSAL_SYNC =
            SynchedEntityData.defineId(LoliEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double BASE_MOVEMENT_SPEED = 0.30D;

    private boolean dispersal;
    private boolean dimensionChanging;
    private boolean lifecycleRemoval;

    public LoliEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DISPERSAL_SYNC, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, BASE_MOVEMENT_SPEED)
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LoliMeleeGoal(this));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false, this::isValidAttackTarget));
    }

    private boolean isValidAttackTarget(@Nullable LivingEntity target) {
        if (!LoliConfig.LOLI_ATTACK.get() || target == null || target == this
                || target instanceof LoliEntity || !(target instanceof Mob || target instanceof Player)) return false;
        if (!target.isAlive() || target.isRemoved() || target.deathTime > 0) return false;
        if (target instanceof Player player && (player.isSpectator() || player.isCreative())) return false;
        double range = getAttributeValue(Attributes.FOLLOW_RANGE);
        return distanceToSqr(target) <= range * range && !LoliPickaxeItem.hasLoliProtection(target);
    }

     
    private static final class LoliMeleeGoal extends MeleeAttackGoal {
        private final LoliEntity loli;
        private Vec3 lastPosition;
        private int stalledTicks;
        private int teleportCooldown;

        private LoliMeleeGoal(LoliEntity loli) {
            super(loli, 1.0D, true);
            this.loli = loli;
        }

        @Override
        public boolean canUse() {
            return loli.isValidAttackTarget(loli.getTarget()) && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return loli.isValidAttackTarget(loli.getTarget()) && super.canContinueToUse();
        }

        @Override
        public void start() {
            super.start();
            lastPosition = loli.position();
            stalledTicks = 0;
        }

        @Override
        public void stop() {
            super.stop();
            if (!loli.isValidAttackTarget(loli.getTarget())) loli.setTarget(null);
            stalledTicks = 0;
            lastPosition = null;
        }

        @Override
        public void tick() {
            super.tick();
            if (teleportCooldown > 0) teleportCooldown--;
            LivingEntity target = loli.getTarget();
            if (target == null) return;
            Vec3 position = loli.position();
            boolean stationary = lastPosition != null && position.distanceToSqr(lastPosition) < 0.0004D;
            stalledTicks = stationary && loli.distanceToSqr(target) > getAttackReachSqr(target)
                    ? stalledTicks + 1 : 0;
            lastPosition = position;
            if (LoliConfig.LOLI_TELEPORT.get() && stalledTicks >= 60 && teleportCooldown == 0) {
                teleportCooldown = 100;
                stalledTicks = 0;
                tryUnstuckTeleport(target);
            }
        }

        private void tryUnstuckTeleport(LivingEntity target) {
             
             
            if (!loli.getSensing().hasLineOfSight(target)) return;
            for (int attempt = 0; attempt < 8; attempt++) {
                double angle = attempt * Math.PI / 4.0D;
                double x = target.getX() + Math.cos(angle) * 2.5D;
                double z = target.getZ() + Math.sin(angle) * 2.5D;
                if (!loli.level().hasChunkAt(BlockPos.containing(x, target.getY(), z))) continue;
                if (loli.randomTeleport(x, target.getY() + 1.0D, z, true)) {
                    loli.getNavigation().stop();
                    loli.getNavigation().moveTo(target, 1.0D);
                    lastPosition = loli.position();
                    return;
                }
            }
        }
    }

    @Override
    public void aiStep() {
         
         
        if (!level().isClientSide && !lolipickaxe$isRemovalAuthorized()) {
            double speed = BASE_MOVEMENT_SPEED * Mth.clamp(LoliConfig.LOLI_SPEED.get(), 0.25D, 2.0D);
            var attribute = getAttribute(Attributes.MOVEMENT_SPEED);
            if (attribute != null && Double.compare(attribute.getBaseValue(), speed) != 0) {
                attribute.setBaseValue(speed);
            }
            if (!isValidAttackTarget(getTarget())) setTarget(null);
            if (getY() < level().getMinBuildHeight() - 8.0D) LoliEntityDefense.restoreSafePosition(this);
            LoliDefenseHooks.maintain(this);
        }
        super.aiStep();
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return target instanceof LivingEntity living && isValidAttackTarget(living)
                && LoliPickaxeItem.attackFromLoli(this, target);
    }

    



    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (lolipickaxe$isRemovalAuthorized()) return super.hurt(source, amount);
        try { LoliPickaxeItem.retaliate(this, source); } catch (Throwable ignored) {}
        return false;
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        if (lolipickaxe$isRemovalAuthorized()) {
            super.actuallyHurt(source, amount);
            return;
        }
        try { LoliPickaxeItem.retaliate(this, source); } catch (Throwable ignored) {}
        LoliDefenseHooks.maintain(this);
    }

    @Override
    public void die(DamageSource source) {
        if (lolipickaxe$isRemovalAuthorized()) {
            super.die(source);
            return;
        }
        try { LoliPickaxeItem.retaliate(this, source); } catch (Throwable ignored) {}
        LoliDefenseHooks.maintain(this);
    }

    @Override
    protected void tickDeath() {
        if (lolipickaxe$isRemovalAuthorized()) {
            super.tickDeath();
            return;
        }
        LoliDefenseHooks.maintain(this);
    }

    @Override
    public float getHealth() {
        if (lolipickaxe$isRemovalAuthorized()) return super.getHealth();
        return LoliDefenseHooks.PROTECTED_HEALTH;
    }

    @Override
    public void setHealth(float health) {
        if (lolipickaxe$isRemovalAuthorized()) {
            super.setHealth(health);
            return;
        }
        super.setHealth(LoliDefenseHooks.PROTECTED_HEALTH);
    }

    @Override
    public boolean isDeadOrDying() {
        if (lolipickaxe$isRemovalAuthorized()) return super.isDeadOrDying();
        return false;
    }

    @Override
    public boolean isAlive() {
        if (lolipickaxe$isRemovalAuthorized()) return super.isAlive();
        return !super.isRemoved();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !lolipickaxe$isRemovalAuthorized() || super.isInvulnerableTo(source);
    }

    @Override
    public void kill() {
        if (lolipickaxe$isRemovalAuthorized()) {
            super.kill();
            return;
        }
        LoliDefenseHooks.maintain(this);
    }

    @Override
    @Nullable
    public RemovalReason getRemovalReason() {
        return lolipickaxe$isRemovalAuthorized() ? super.getRemovalReason() : null;
    }

    @Override
    public boolean canUpdate() {
        return !lolipickaxe$isRemovalAuthorized() || super.canUpdate();
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        lifecycleRemoval = false;
        dimensionChanging = false;
        if (!level().isClientSide) LoliDefenseHooks.maintain(this);
    }

    public void lolipickaxe$setLifecycleRemoval(boolean value) {
        lifecycleRemoval = value;
    }

    @Override
    @Nullable
    public Entity changeDimension(ServerLevel destination, ITeleporter teleporter) {
        dimensionChanging = true;
        return super.changeDimension(destination, teleporter);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level().isClientSide) lifecycleRemoval = true;
        boolean normalLifecycle = !reason.shouldDestroy();
        if (level().isClientSide || lolipickaxe$isRemovalAuthorized() || normalLifecycle) {
            super.remove(reason);
            return;
        }
        LoliDefenseHooks.maintain(this);
    }

    @Override
    public void onRemovedFromWorld() {
        if (level().isClientSide || lolipickaxe$isRemovalAuthorized() || dimensionChanging || lifecycleRemoval) {
            super.onRemovedFromWorld();
            return;
        }
        LoliDefenseHooks.maintain(this);
    }

    public boolean lolipickaxe$isRemovalAuthorized() {
        boolean syncedDispersal = false;
        try {
            syncedDispersal = entityData.get(DISPERSAL_SYNC);
        } catch (Throwable ignored) {
        }
        return dispersal || syncedDispersal || dimensionChanging || lifecycleRemoval
                || LoliEntityDefense.isAuthorized(this);
    }

    public boolean lolipickaxe$isDispersalRequested() {
        boolean synced = false;
        try {
            synced = entityData.get(DISPERSAL_SYNC);
        } catch (Throwable ignored) {
        }
        return dispersal || synced
                || getPersistentData().getBoolean(LoliEntityDefense.DISPERSAL_AUTHORIZED_TAG);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Dispersal", dispersal || entityData.get(DISPERSAL_SYNC));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        dispersal = tag.getBoolean("Dispersal");
        entityData.set(DISPERSAL_SYNC, dispersal);
        if (dispersal) getPersistentData().putBoolean(LoliEntityDefense.DISPERSAL_AUTHORIZED_TAG, true);
    }

    @Override
    public void lolipickaxe$setDispersal(boolean value) {
        dispersal = value;
        entityData.set(DISPERSAL_SYNC, value, true);
        if (value && !level().isClientSide) {
            LoliEntityDefense.authorizeRemoval(this);

             
             
            if (level() instanceof ServerLevel serverLevel) {
                LoliStructuralPurge.purgeDispersedLoli(serverLevel, this);
            } else {
                discard();
            }
        }
    }
}
