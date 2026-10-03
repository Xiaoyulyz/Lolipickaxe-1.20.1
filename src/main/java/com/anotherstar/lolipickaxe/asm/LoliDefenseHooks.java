package com.anotherstar.lolipickaxe.asm;

import com.anotherstar.lolipickaxe.config.LoliConfig;
import com.anotherstar.lolipickaxe.damage.ModDamageTypes;
import com.anotherstar.lolipickaxe.entity.LoliEntity;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

 
public final class LoliDefenseHooks {
    public static final float PROTECTED_HEALTH = 20.0F;

    public static boolean isProtectedEntity(Entity entity) {
        return entity instanceof LivingEntity living && LoliPickaxeItem.hasLoliProtection(living);
    }

    public static float filterSetHealth(LivingEntity entity, float requested) {
        if (isProtectedEntity(entity)) return PROTECTED_HEALTH;
        return LoliErasureState.isErased(entity) ? 0.0F : requested;
    }

    public static boolean shouldOverrideHealth(LivingEntity entity) {
        return isProtectedEntity(entity) || LoliErasureState.isErased(entity);
    }

    public static float forcedHealth(LivingEntity entity) {
        return isProtectedEntity(entity) ? PROTECTED_HEALTH : 0.0F;
    }

    public static boolean shouldOverrideMaxHealth(LivingEntity entity) {
        return isProtectedEntity(entity) || LoliErasureState.isErased(entity);
    }

    public static float forcedMaxHealth(LivingEntity entity) {
        return isProtectedEntity(entity) ? PROTECTED_HEALTH : 0.0F;
    }

     
    public static void maintain(LivingEntity entity) {
        if (!isProtectedEntity(entity)) return;
        LoliErasureState.protect(entity);
        var max = entity.getAttribute(Attributes.MAX_HEALTH);
        if (max != null && max.getBaseValue() != PROTECTED_HEALTH) max.setBaseValue(PROTECTED_HEALTH);
         
        if (entity.getEntityData().get(LivingEntity.DATA_HEALTH_ID) != PROTECTED_HEALTH) {
            entity.setHealth(PROTECTED_HEALTH);
        }
        entity.dead = false;
        entity.deathTime = 0;
        entity.hurtTime = 0;
        entity.fallDistance = 0.0F;
        entity.clearFire();
        if (entity instanceof LoliEntity && !entity.getActiveEffects().isEmpty()) entity.removeAllEffects();
    }

    public static boolean shouldBlockDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!isProtectedEntity(entity)) return false;
        LoliPickaxeItem.retaliate(entity, source);
        return true;
    }

    public static boolean shouldBlockDeath(LivingEntity entity, DamageSource source) {
        if (!isProtectedEntity(entity)) return false;
        maintain(entity);
        LoliPickaxeItem.retaliate(entity, source);
        return true;
    }

    public static boolean shouldCancelLivingTick(LivingEntity entity) {
        if (entity == null) return false;
        if (isProtectedEntity(entity)) {
            LoliUnsafeBridge.stabilizeProtected(entity);
            return false;
        }
        if (!LoliErasureState.isErased(entity)) return false;
        LoliUnsafeBridge.forceRawHealth(entity, 0.0F);
        entity.deathTime = Math.max(entity.deathTime, LoliErasureState.deathTime(entity));
        if (LoliErasureState.isCool(entity)) {
            entity.dead = true;
            return true;
        }
        if (LoliConfig.FORBID_ON_LIVING_UPDATE.get()) {
            entity.dead = true;
            return true;
        }
        return false;
    }

    public static void afterLivingTick(LivingEntity entity) {
        if (entity == null) return;
        if (isProtectedEntity(entity)) {
            LoliUnsafeBridge.stabilizeProtected(entity);
            return;
        }
        if (LoliErasureState.isErased(entity)) {
            LoliUnsafeBridge.forceRawHealth(entity, 0.0F);
            entity.deathTime = Math.max(entity.deathTime, LoliErasureState.deathTime(entity));
        }
    }

     
    public static boolean shouldBypassDeathEvent(LivingEntity entity, DamageSource source) {
        return !isProtectedEntity(entity) && source.is(ModDamageTypes.LOLI);
    }

    public static boolean shouldCancelProtectedKill(Entity entity) {
        return isProtectedEntity(entity);
    }

     
    public static boolean shouldBlockRemoval(Entity entity, Entity.RemovalReason reason) {
        if (entity instanceof LoliEntity loli && !reason.shouldDestroy()) {
            loli.lolipickaxe$setLifecycleRemoval(true);
        }
        return !entity.level().isClientSide && reason.shouldDestroy() && isProtectedEntity(entity);
    }

    public static boolean shouldCancelLoliRemoval(Entity entity) {
        return !entity.level().isClientSide && isProtectedEntity(entity);
    }

    public static boolean shouldBlockDisconnect(ServerGamePacketListenerImpl listener) {
        return listener != null && listener.player != null && isProtectedEntity(listener.player);
    }

    private LoliDefenseHooks() {}
}
