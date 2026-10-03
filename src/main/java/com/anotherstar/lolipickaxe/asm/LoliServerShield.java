package com.anotherstar.lolipickaxe.asm;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;

public final class LoliServerShield extends ServerPlayer {
    public LoliServerShield(MinecraftServer server, ServerLevel level, GameProfile profile) {
        super(server, level, profile);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void actuallyHurt(DamageSource source, float amount) {
    }

    @Override
    public void die(DamageSource source) {
    }

    @Override
    public void tickDeath() {
    }

    @Override
    public void kill() {
    }

    @Override
    public void setHealth(float health) {
    }

    @Override
    public float getHealth() {
        return LoliDefenseHooks.PROTECTED_HEALTH;
    }

    @Override
    public boolean isDeadOrDying() {
        return false;
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public void setRemainingFireTicks(int ticks) {
    }

    @Override
    public void setSecondsOnFire(int seconds) {
    }

    @Override
    public boolean isInLava() {
        return false;
    }

    @Override
    public int getAirSupply() {
        return 300;
    }

    @Override
    public void setAirSupply(int air) {
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public int getTicksFrozen() {
        return 0;
    }

    @Override
    public void setTicksFrozen(int ticks) {
    }

    @Override
    public boolean isFullyFrozen() {
        return false;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect().isBeneficial();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) return;
        super.remove(reason);
    }
}
