package com.anotherstar.lolipickaxe.asm;

import com.anotherstar.lolipickaxe.damage.ModDamageTypes;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class LoliErasureState {
    private static final String DEAD = "LoliDead";
    private static final String COOL = "LoliCool";
    private static final String BEYOND = "LoliBeyondRedemption";
    private static final String REINCARNATION = "LoliReincarnation";
    private static final Map<LivingEntity, Integer> DELAYED = new WeakHashMap<>();
    private static final Set<LivingEntity> IN_DEATH = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<LivingEntity> COMPLETED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<LivingEntity> NEEDS_BASE_DIE = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<LivingEntity, DamageSource> DEATH_SOURCES = new WeakHashMap<>();
    private static final Map<Class<? extends Entity>, Integer> ANTI_ENTITY = new HashMap<>();

    public static boolean erase(LivingEntity target, DamageSource source, boolean compulsory) {
        if (target == null || source == null || target.level().isClientSide
                || LoliPickaxeItem.hasLoliProtection(target) || target.isRemoved()) return false;
        if (isErased(target) || IN_DEATH.contains(target)) {
            registerIfErased(target);
            return true;
        }
        float raw = LoliUnsafeBridge.readRawHealth(target);
        if (COMPLETED.contains(target) && !Float.isNaN(raw) && raw <= 0.0F) return true;
        if (Float.isNaN(raw) || raw > 0.0F) {
            COMPLETED.remove(target);
            NEEDS_BASE_DIE.remove(target);
            DEATH_SOURCES.remove(target);
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof Player player) target.setLastHurtByPlayer(player);
        if (attacker instanceof LivingEntity living) target.setLastHurtByMob(living);
        target.lastHurtByPlayerTime = 60;
        target.getCombatTracker().recordDamage(source, Float.MAX_VALUE);
        LoliBossBarControl.zero(target);

        IN_DEATH.add(target);
        Class<? extends Entity> deathClass = target instanceof Player ? null : target.getClass();
        if (deathClass != null) beginAntiEntity(deathClass);
        try {
            try {
                target.setHealth(0.0F);
            } catch (Throwable ignored) {
            }
            LoliUnsafeBridge.invokeVanillaSetHealth(target, 0.0F);
            LoliUnsafeBridge.forceRawHealth(target, 0.0F);

            try {
                target.die(source);
            } catch (Throwable ignored) {
            }
            if (compulsory && !LoliUnsafeBridge.isVanillaDeathCommitted(target)) NEEDS_BASE_DIE.add(target);
            else NEEDS_BASE_DIE.remove(target);
            LoliUnsafeBridge.forceRawHealth(target, 0.0F);
            LoliBossBarControl.zero(target);
            COMPLETED.add(target);
        } finally {
            if (deathClass != null) endAntiEntity(deathClass);
            IN_DEATH.remove(target);
        }

        if (compulsory && !target.isRemoved()) {
            target.getPersistentData().putBoolean(DEAD, true);
            target.getPersistentData().putBoolean(COOL, false);
            target.getPersistentData().putInt("LoliDeathTime", 0);
            DELAYED.put(target, delayTicks(target));
            DEATH_SOURCES.put(target, source);
            LoliUnsafeBridge.forceRawHealth(target, 0.0F);
        }
        return true;
    }

    public static boolean eraseNonLiving(Entity target) {
        if (target == null || target.level().isClientSide || target instanceof Player || target.isRemoved()) return false;
        target.discard();
        return true;
    }

    public static boolean isErased(LivingEntity entity) {
        return entity != null && (entity.getPersistentData().getBoolean(DEAD)
                || entity instanceof Player player && isBeyondRedemption(player)
                && !LoliPickaxeItem.hasLoliProtection(player));
    }

    public static boolean shouldRejectJoin(Entity entity) {
        if (entity == null || ANTI_ENTITY.isEmpty()) return false;
        for (Map.Entry<Class<? extends Entity>, Integer> entry : ANTI_ENTITY.entrySet()) {
            if (entry.getValue() > 0 && entry.getKey().isInstance(entity)) return true;
        }
        return false;
    }

    public static void registerIfErased(LivingEntity entity) {
        if (entity != null && entity.getPersistentData().getBoolean(DEAD) && !entity.isRemoved()) {
            int total = delayTicks(entity);
            int elapsed = Math.max(0, entity.getPersistentData().getInt("LoliDeathTime"));
            DELAYED.putIfAbsent(entity, Math.max(1, total - elapsed));
            LoliUnsafeBridge.forceRawHealth(entity, 0.0F);
            LoliBossBarControl.zero(entity);
        }
    }

    public static void stabilizeProtected(LivingEntity entity) {
        LoliUnsafeBridge.stabilizeProtected(entity);
    }

    public static void protect(LivingEntity entity) {
        clearDeathFlags(entity);
        if (!entity.level().isClientSide) {
            DELAYED.remove(entity);
            COMPLETED.remove(entity);
            NEEDS_BASE_DIE.remove(entity);
            DEATH_SOURCES.remove(entity);
        }
    }

    public static void tick(MinecraftServer server) {
        var iterator = DELAYED.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            LivingEntity target = entry.getKey();
            if (target == null || target.isRemoved() || LoliPickaxeItem.hasLoliProtection(target)) {
                if (target != null && !target.isRemoved()) clearDeathFlags(target);
                if (target != null) {
                    LoliBossBarControl.release(target.getUUID());
                    NEEDS_BASE_DIE.remove(target);
                    DEATH_SOURCES.remove(target);
                }
                iterator.remove();
                continue;
            }

            LoliUnsafeBridge.forceRawHealth(target, 0.0F);
            LoliBossBarControl.zero(target);
            int remaining = entry.getValue() - 1;
            int total = delayTicks(target);
            target.getPersistentData().putInt("LoliDeathTime", Math.max(0, total - remaining));
            if (remaining > 0) {
                entry.setValue(remaining);
                continue;
            }

            iterator.remove();
            target.getPersistentData().putBoolean(COOL, true);
            DamageSource source = DEATH_SOURCES.remove(target);
            boolean needsBaseDie = NEEDS_BASE_DIE.remove(target);
            if (target instanceof Player) continue;

            if (target.level() instanceof ServerLevel level) {
                if (needsBaseDie && LoliStructuralPurge.isEntityRegistered(level, target)) {
                    DamageSource fallback = source != null ? source : ModDamageTypes.loli(level, null);
                    LoliUnsafeBridge.invokeVanillaDie(target, fallback);
                    LoliUnsafeBridge.forceRawHealth(target, 0.0F);
                }
                if (LoliStructuralPurge.isEntityRegistered(level, target)) {
                    LoliUnsafeBridge.invokeVanillaRemoval(target, Entity.RemovalReason.KILLED);
                }
                if (LoliStructuralPurge.isEntityRegistered(level, target)) {
                    LoliStructuralPurge.purge(level, target, java.util.List.of());
                } else {
                    LoliBossBarControl.detach(target);
                }
            } else {
                if (needsBaseDie && source != null) LoliUnsafeBridge.invokeVanillaDie(target, source);
                LoliUnsafeBridge.invokeVanillaRemoval(target, Entity.RemovalReason.KILLED);
                LoliBossBarControl.detach(target);
            }
        }
    }

    public static int deathTime(LivingEntity entity) {
        return entity == null ? 0 : Math.max(0, entity.getPersistentData().getInt("LoliDeathTime"));
    }

    public static boolean isCool(LivingEntity entity) {
        return entity != null && entity.getPersistentData().getBoolean(COOL);
    }

    public static void markBeyondRedemption(Player player) { player.getPersistentData().putBoolean(BEYOND, true); }
    public static void markReincarnation(Player player) { player.getPersistentData().putBoolean(REINCARNATION, true); }
    public static boolean isBeyondRedemption(Player player) { return player.getPersistentData().getBoolean(BEYOND); }
    public static boolean isReincarnation(Player player) { return player.getPersistentData().getBoolean(REINCARNATION); }

    public static void copyPlayerPunishments(Player oldPlayer, Player newPlayer) {
        if (isBeyondRedemption(oldPlayer)) markBeyondRedemption(newPlayer);
        if (isReincarnation(oldPlayer)) markReincarnation(newPlayer);
        clearDeathFlags(newPlayer);
        DELAYED.remove(oldPlayer);
        NEEDS_BASE_DIE.remove(oldPlayer);
        DEATH_SOURCES.remove(oldPlayer);
        if (isReincarnation(newPlayer)) clearPlayerData(newPlayer);
    }

    public static void applyPersistentPunishments(ServerPlayer player) {
        if (isReincarnation(player)) clearPlayerData(player);
        if (isBeyondRedemption(player) && !LoliPickaxeItem.hasLoliProtection(player) && !player.dead) {
            if (!COMPLETED.contains(player)) {
                DamageSource source = ModDamageTypes.loli(player.level(), null);
                player.getCombatTracker().recordDamage(source, Float.MAX_VALUE);
                LoliUnsafeBridge.invokeVanillaSetHealth(player, 0.0F);
                LoliUnsafeBridge.forceRawHealth(player, 0.0F);
                player.die(source);
                COMPLETED.add(player);
            }
        }
    }

    private static int delayTicks(LivingEntity entity) {
        return entity instanceof EnderDragon ? 201 : 21;
    }

    private static void beginAntiEntity(Class<? extends Entity> type) {
        ANTI_ENTITY.merge(type, 1, Integer::sum);
    }

    private static void endAntiEntity(Class<? extends Entity> type) {
        Integer count = ANTI_ENTITY.get(type);
        if (count == null || count <= 1) ANTI_ENTITY.remove(type);
        else ANTI_ENTITY.put(type, count - 1);
    }

    private static void clearDeathFlags(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        for (String key : new String[]{DEAD, COOL, "LoliDeathTime", "LoliCompulsoryRemove",
                "LoliDeathProcessed", "LoliDeathAnnounced"}) data.remove(key);
    }

    private static void clearPlayerData(Player player) {
        player.getInventory().clearContent();
        player.getEnderChestInventory().clearContent();
    }

    public static void clear() {
        DELAYED.clear();
        IN_DEATH.clear();
        COMPLETED.clear();
        NEEDS_BASE_DIE.clear();
        DEATH_SOURCES.clear();
        ANTI_ENTITY.clear();
        LoliBossBarControl.clear();
    }

    private LoliErasureState() {}
}
