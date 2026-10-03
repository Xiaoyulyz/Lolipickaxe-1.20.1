package com.anotherstar.lolipickaxe.client;

import com.anotherstar.lolipickaxe.asm.LoliEntityDefense;
import com.anotherstar.lolipickaxe.asm.LoliUnsafeBridge;
import com.anotherstar.lolipickaxe.asm.LoliDetachedOwnerGraph;
import com.anotherstar.lolipickaxe.client.screen.SafeAttackSimulationScreen;
import com.anotherstar.lolipickaxe.entity.LoliEntity;
import com.anotherstar.lolipickaxe.network.ForceEntityCleanupPacket;
import com.anotherstar.lolipickaxe.network.SafeAttackEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ClientPacketHandlers {
    private static final int BUG_ENTITY_CLEANUP_TICKS = 60;
    private static final Map<UUID, PendingBugCleanup> PENDING_BUG_CLEANUPS = new java.util.LinkedHashMap<>();
    private static ClientLevel bugCleanupLevel;
    public static void showSafeAttack(SafeAttackEffect effect) {
        Minecraft.getInstance().setScreen(new SafeAttackSimulationScreen(effect));
    }

    public static void refreshHeldItem(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.itemInHandRenderer.itemUsed(hand);
    }

    public static void forceEntityCleanup(List<ForceEntityCleanupPacket.Target> targets,
                                          boolean allowProtectedLoli) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || targets == null) return;
        for (ForceEntityCleanupPacket.Target target : targets) {
            if (target == null || target.entityUuid() == null) continue;
            Set<Entity> matches = Collections.newSetFromMap(new IdentityHashMap<>());

            Entity byId = level.getEntity(target.entityId());
            if (matchesTarget(byId, target)) matches.add(byId);

             
             
            try {
                for (Entity entity : level.entitiesForRendering()) {
                    if (matchesTarget(entity, target)) matches.add(entity);
                }
            } catch (Throwable ignored) {
            }

            for (Entity entity : matches) {
                if (entity instanceof LoliEntity && !allowProtectedLoli) continue;
                try { LoliDetachedOwnerGraph.detach(entity, List.of()); } catch (Throwable ignored) {}
                forceRemoveClientEntity(level, entity);
            }

             
             
            purgeClientEntityStorage(level, target, allowProtectedLoli);
        }
    }

    public static void suppressBugEntity(Entity entity) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || entity == null) return;
        if (bugCleanupLevel != level) {
            PENDING_BUG_CLEANUPS.clear();
            bugCleanupLevel = level;
        }
        ForceEntityCleanupPacket.Target target =
                new ForceEntityCleanupPacket.Target(entity.getId(), entity.getUUID());
        PENDING_BUG_CLEANUPS.put(entity.getUUID(), new PendingBugCleanup(target, BUG_ENTITY_CLEANUP_TICKS));
        forceEntityCleanup(List.of(target), true);
    }

     
    public static void tickBugEntityCleanup() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || bugCleanupLevel != level) {
            PENDING_BUG_CLEANUPS.clear();
            bugCleanupLevel = level;
            return;
        }
        Iterator<Map.Entry<UUID, PendingBugCleanup>> iterator = PENDING_BUG_CLEANUPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingBugCleanup> entry = iterator.next();
            PendingBugCleanup pending = entry.getValue();
            forceEntityCleanup(List.of(pending.target), true);
            if (--pending.remaining <= 0) iterator.remove();
        }
    }

    public static void forceLocalLoliCleanup(Entity entity) {
        if (!(entity instanceof LoliEntity)) return;
        forceEntityCleanup(List.of(new ForceEntityCleanupPacket.Target(entity.getId(), entity.getUUID())), true);
    }

    public static void clearLookedAtLoliGhost(Player player, double range, double width) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || player == null) return;
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 end = start.add(look.scale(range));
        LoliEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        try {
            for (Entity entity : level.entitiesForRendering()) {
                if (!(entity instanceof LoliEntity loli)) continue;
                AABB bounds = loli.getBoundingBox().inflate(width);
                double distance = bounds.contains(start) ? 0.0D
                        : bounds.clip(start, end).map(start::distanceToSqr).orElse(-1.0D);
                if (distance >= 0.0D && distance < nearestDistance) {
                    nearest = loli;
                    nearestDistance = distance;
                }
            }
        } catch (Throwable ignored) {
        }
        if (nearest != null) forceLocalLoliCleanup(nearest);
    }

    private static boolean matchesTarget(Entity entity, ForceEntityCleanupPacket.Target target) {
        return entity != null && target.entityUuid().equals(entity.getUUID());
    }

    private static void forceRemoveClientEntity(ClientLevel level, Entity entity) {
        if (entity instanceof LoliEntity loli) {
            loli.getPersistentData().putBoolean(LoliEntityDefense.DISPERSAL_AUTHORIZED_TAG, true);
            loli.lolipickaxe$setLifecycleRemoval(true);
        }
        Entity.RemovalReason reason = entity instanceof LoliEntity
                ? Entity.RemovalReason.DISCARDED : Entity.RemovalReason.KILLED;
        try { entity.setInvisible(true); } catch (Throwable ignored) {}
        try { entity.stopRiding(); } catch (Throwable ignored) {}
        try { entity.ejectPassengers(); } catch (Throwable ignored) {}
        try {
            if (level.getEntity(entity.getId()) == entity) level.removeEntity(entity.getId(), reason);
        } catch (Throwable ignored) {
        }
        try { entity.remove(reason); } catch (Throwable ignored) {}

         
         
         
        try { LoliUnsafeBridge.forceRemovalState(entity, reason); } catch (Throwable ignored) {}
        try { entity.onRemovedFromWorld(); } catch (Throwable ignored) {}
        try { entity.invalidateCaps(); } catch (Throwable ignored) {}
        try {
            if (level.getEntity(entity.getId()) == entity) level.removeEntity(entity.getId(), reason);
        } catch (Throwable ignored) {
        }
    }

    private static void purgeClientEntityStorage(ClientLevel level, ForceEntityCleanupPacket.Target target,
                                                 boolean allowProtectedLoli) {
        Object storage = readField(level, "entityStorage", "f_171631_");
        if (storage == null) return;
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        purgeObjectGraph(storage, target, allowProtectedLoli, visited, 0, 5);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void purgeObjectGraph(Object object, ForceEntityCleanupPacket.Target target,
                                         boolean allowProtectedLoli, Set<Object> visited,
                                         int depth, int maxDepth) {
        if (object == null || depth > maxDepth || !visited.add(object)) return;
        if (object instanceof Map map) {
            try {
                Iterator<Map.Entry> iterator = map.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry entry = iterator.next();
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    if (isRemovableTarget(key, target, allowProtectedLoli)
                            || isRemovableTarget(value, target, allowProtectedLoli)) {
                        iterator.remove();
                    } else {
                        purgeObjectGraph(value, target, allowProtectedLoli, visited, depth + 1, maxDepth);
                    }
                }
            } catch (Throwable ignored) {
            }
            return;
        }
        if (object instanceof Collection collection) {
            List<Object> nested = new ArrayList<>();
            try {
                Iterator iterator = collection.iterator();
                while (iterator.hasNext()) {
                    Object value = iterator.next();
                    if (isRemovableTarget(value, target, allowProtectedLoli)) iterator.remove();
                    else nested.add(value);
                }
            } catch (Throwable ignored) {
            }
            for (Object value : nested) {
                purgeObjectGraph(value, target, allowProtectedLoli, visited, depth + 1, maxDepth);
            }
            return;
        }
        Class<?> objectType = object.getClass();
        if (objectType.isArray() && !objectType.getComponentType().isPrimitive()) {
            for (int i = 0; i < Array.getLength(object); i++) {
                Object value = Array.get(object, i);
                if (isRemovableTarget(value, target, allowProtectedLoli)) {
                    try { Array.set(object, i, null); } catch (Throwable ignored) {}
                } else {
                    purgeObjectGraph(value, target, allowProtectedLoli, visited, depth + 1, maxDepth);
                }
            }
            return;
        }
        if (depth >= maxDepth || !isClientEntityContainer(objectType)) return;
        for (Class<?> type = objectType; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(object);
                    if (isRemovableTarget(value, target, allowProtectedLoli)) {
                        if (!Modifier.isFinal(field.getModifiers())) field.set(object, null);
                    } else {
                        purgeObjectGraph(value, target, allowProtectedLoli, visited, depth + 1, maxDepth);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static boolean isRemovableTarget(Object value, ForceEntityCleanupPacket.Target target,
                                             boolean allowProtectedLoli) {
        return value instanceof Entity entity
                && target.entityUuid().equals(entity.getUUID())
                && (allowProtectedLoli || !(entity instanceof LoliEntity));
    }

    private static boolean isClientEntityContainer(Class<?> type) {
        String name = type.getName();
        return name.startsWith("net.minecraft.") || name.startsWith("it.unimi.dsi.fastutil.")
                || Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type);
    }

    private static Object readField(Object owner, String... names) {
        if (owner == null) return null;
        for (Class<?> type = owner.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (String name : names) {
                try {
                    Field field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    return field.get(owner);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static final class PendingBugCleanup {
        private final ForceEntityCleanupPacket.Target target;
        private int remaining;

        private PendingBugCleanup(ForceEntityCleanupPacket.Target target, int remaining) {
            this.target = target;
            this.remaining = remaining;
        }
    }

    private ClientPacketHandlers() {}
}
