package com.anotherstar.lolipickaxe.asm;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import com.anotherstar.lolipickaxe.entity.LoliEntity;
import com.anotherstar.lolipickaxe.network.ForceEntityCleanupPacket;
import com.anotherstar.lolipickaxe.network.ModNetwork;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;


public final class LoliStructuralPurge {
    private static final int MAX_GRAPH_OBJECTS = 64;
    private static final int MAX_GRAPH_DEPTH = 3;
    private static final int STRUCTURAL_PASSES = 3;
    private static final Unsafe UNSAFE = findUnsafe();
    private static final int CLIENT_CLEANUP_RETRIES = 40;
    private static final ConcurrentLinkedQueue<PendingClientCleanup> PENDING_CLIENT_CLEANUP =
            new ConcurrentLinkedQueue<>();

    private static final String[] ENTITY_MANAGER_FIELDS = {
            "entityManager", "f_143244_"
    };
    private static final String[] ENTITY_TICK_LIST_FIELDS = {
            "entityTickList", "f_143243_"
    };
    private static final String[] NAVIGATING_MOBS_FIELDS = {
            "navigatingMobs", "f_143246_"
    };
    private static final String[] MULTIPART_FIELDS = {
            "dragonParts", "multipartEntities", "partEntities", "f_143247_"
    };

    public static void purge(ServerLevel level, LivingEntity primary, Collection<?> prelinked) {
        if (level == null || primary == null || LoliPickaxeItem.hasLoliProtection(primary)) return;
        LoliBossBarControl.suppress(primary);

         
         
         
        LoliExternalLifecycleBridge.retireTargetGraph(primary, prelinked);

        Set<Integer> allIds = new LinkedHashSet<>();
        Set<CleanupTarget> allCleanupTargets = new LinkedHashSet<>();
        for (int pass = 0; pass < STRUCTURAL_PASSES; pass++) {
            LinkedHashSet<Entity> targets = resolveEntityGraph(primary, prelinked);
            targets.removeIf(entity -> entity == null || entity instanceof Player
                    || (entity instanceof LivingEntity living && LoliPickaxeItem.hasLoliProtection(living)));
            if (targets.isEmpty()) break;

            Set<Integer> ids = new LinkedHashSet<>();
            Set<UUID> uuids = new LinkedHashSet<>();
            for (Entity entity : targets) {
                ids.add(entity.getId());
                allIds.add(entity.getId());
                uuids.add(entity.getUUID());
                allCleanupTargets.add(new CleanupTarget(entity.getId(), entity.getUUID()));
                try { entity.stopRiding(); } catch (Throwable ignored) {}
                try { entity.ejectPassengers(); } catch (Throwable ignored) {}
                if (entity instanceof LivingEntity living) {
                    LoliBossBarControl.suppress(living);
                    LoliUnsafeBridge.forceRawHealth(living, 0.0F);
                }
                detachDynamicListeners(level, entity);
            }
            purgeEntityTickList(readField(level, ENTITY_TICK_LIST_FIELDS), targets, ids, uuids);
            purgeEntityManager(readField(level, ENTITY_MANAGER_FIELDS), targets, ids, uuids);
            purgeContainer(readField(level, NAVIGATING_MOBS_FIELDS), targets, ids, uuids, 0, 3);
            purgeContainer(readField(level, MULTIPART_FIELDS), targets, ids, uuids, 0, 4);
            purgeChunkMap(level, targets, ids, uuids);

             
             
            for (Entity entity : targets) {
                LoliUnsafeBridge.forceRemovalState(entity, Entity.RemovalReason.KILLED);
                 
                 
                 
                try { entity.onRemovedFromWorld(); } catch (Throwable ignored) {}
                try { entity.invalidateCaps(); } catch (Throwable ignored) {}
            }
        }

        sendRemovePacket(level, allIds);
        scheduleClientCleanup(level, allCleanupTargets, false);
        if (isEntityRegistered(level, primary)) {
            LoliPickaxe.LOGGER.warn("Loli structural purge could not detach {} ({}) from every level container",
                    primary.getType(), primary.getUUID());
        }
    }

    




    public static boolean isFirstStageResolved(LivingEntity target) {
        if (target == null) return true;
        if (target instanceof Player) return LoliUnsafeBridge.isVanillaDeathCommitted(target);
        if (!(target.level() instanceof ServerLevel level)) return target.isRemoved();
        try {
            var key = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
            boolean vanilla = key != null && "minecraft".equals(key.getNamespace());
            if (vanilla) return LoliUnsafeBridge.isVanillaDeathCommitted(target);
        } catch (Throwable ignored) {
        }
        if (isEntityRegistered(level, target)) return false;
         
         
        try {
            return target.isRemoved() || target.getRemovalReason() != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

     
    public static boolean isEntityRegistered(ServerLevel level, Entity entity) {
        if (level == null || entity == null) return false;
        try {
            if (level.getEntity(entity.getUUID()) == entity) return true;
        } catch (Throwable ignored) {
        }
        Set<Entity> targets = Collections.newSetFromMap(new IdentityHashMap<>());
        targets.add(entity);
        Set<Integer> ids = Set.of(entity.getId());
        Set<UUID> uuids = Set.of(entity.getUUID());
        Object manager = readField(level, ENTITY_MANAGER_FIELDS);
        if (manager != null) {
            Object visible = readField(manager, "visibleEntityStorage", "f_157494_");
            if (containsTarget(visible, targets, ids, uuids, 0, 3)) return true;
            Object adapter = readField(manager, "entityGetter", "f_157496_");
            if (containsTarget(adapter, targets, ids, uuids, 0, 4)) return true;
            Object sections = readField(manager, "sectionStorage", "f_157495_");
            if (containsTarget(sections, targets, ids, uuids, 0, 4)) return true;
        }
        if (containsTarget(readField(level, ENTITY_TICK_LIST_FIELDS), targets, ids, uuids, 0, 3)) return true;
        Object chunkMap = readField(level.getChunkSource(), "chunkMap", "f_8325_");
        return containsTarget(readField(chunkMap, "entityMap", "trackedEntities", "f_140150_"),
                targets, ids, uuids, 0, 3);
    }

    





    public static void purgeDispersedLoli(ServerLevel level, LoliEntity loli) {
        if (level == null || loli == null || !loli.lolipickaxe$isRemovalAuthorized()) return;

        LinkedHashSet<Entity> targets = new LinkedHashSet<>();
        targets.add(loli);
        Set<Integer> ids = new LinkedHashSet<>();
        ids.add(loli.getId());
        Set<UUID> uuids = new LinkedHashSet<>();
        uuids.add(loli.getUUID());

        detachDynamicListeners(level, loli);
        purgeEntityTickList(readField(level, ENTITY_TICK_LIST_FIELDS), targets, ids, uuids);
        purgeEntityManager(readField(level, ENTITY_MANAGER_FIELDS), targets, ids, uuids);
        purgeContainer(readField(level, NAVIGATING_MOBS_FIELDS), targets, ids, uuids, 0, 2);
        purgeContainer(readField(level, MULTIPART_FIELDS), targets, ids, uuids, 0, 3);
        purgeChunkMap(level, targets, ids, uuids);

        try {
            loli.setRemoved(Entity.RemovalReason.DISCARDED);
        } catch (Throwable ignored) {
        }
         
         
        LoliUnsafeBridge.forceRemovalState(loli, Entity.RemovalReason.DISCARDED);
        try {
            loli.onRemovedFromWorld();
        } catch (Throwable ignored) {
        }
        try {
            loli.invalidateCaps();
        } catch (Throwable ignored) {
        }
        sendRemovePacket(level, ids);
        scheduleClientCleanup(level, List.of(new CleanupTarget(loli.getId(), loli.getUUID())), true);
    }

     
    public static void purgeStaleLoliForReplacement(ServerLevel level, LoliEntity loli) {
        if (level == null || loli == null || !LoliEntityDefense.isReplacementCleanup(loli)) return;

        LinkedHashSet<Entity> targets = new LinkedHashSet<>();
        targets.add(loli);
        Set<Integer> ids = new LinkedHashSet<>();
        ids.add(loli.getId());
        Set<UUID> uuids = new LinkedHashSet<>();
        uuids.add(loli.getUUID());

        detachDynamicListeners(level, loli);
        purgeEntityTickList(readField(level, ENTITY_TICK_LIST_FIELDS), targets, ids, uuids);
        purgeEntityManager(readField(level, ENTITY_MANAGER_FIELDS), targets, ids, uuids);
        purgeContainer(readField(level, NAVIGATING_MOBS_FIELDS), targets, ids, uuids, 0, 2);
        purgeContainer(readField(level, MULTIPART_FIELDS), targets, ids, uuids, 0, 3);
        purgeChunkMap(level, targets, ids, uuids);
        LoliUnsafeBridge.forceRemovalState(loli, Entity.RemovalReason.DISCARDED);
        try { loli.onRemovedFromWorld(); } catch (Throwable ignored) {}
        try { loli.invalidateCaps(); } catch (Throwable ignored) {}
        sendRemovePacket(level, ids);
        scheduleClientCleanup(level, List.of(new CleanupTarget(loli.getId(), loli.getUUID())), true);
    }

     
    public static void tickClientCleanup(MinecraftServer server) {
        if (server == null || PENDING_CLIENT_CLEANUP.isEmpty()) return;
        for (PendingClientCleanup pending : List.copyOf(PENDING_CLIENT_CLEANUP)) {
            ServerLevel level = server.getLevel(pending.dimension);
            if (level == null) {
                PENDING_CLIENT_CLEANUP.remove(pending);
                continue;
            }
            sendClientCleanup(level, pending.targets, pending.allowProtectedLoli);
            if (--pending.remaining <= 0) PENDING_CLIENT_CLEANUP.remove(pending);
        }
    }

    public static void clearClientCleanup() {
        PENDING_CLIENT_CLEANUP.clear();
    }

    public static void scheduleClientCleanup(ServerLevel level, Entity entity, boolean allowProtectedLoli) {
        if (level == null || entity == null) return;
        scheduleClientCleanup(level, List.of(new CleanupTarget(entity.getId(), entity.getUUID())), allowProtectedLoli);
    }

    private static void scheduleClientCleanup(ServerLevel level, Collection<CleanupTarget> targets,
                                              boolean allowProtectedLoli) {
        if (level == null || targets == null || targets.isEmpty()) return;
        List<CleanupTarget> stable = targets.stream().filter(target -> target != null).distinct().toList();
        if (stable.isEmpty()) return;
        sendClientCleanup(level, stable, allowProtectedLoli);
        PENDING_CLIENT_CLEANUP.add(new PendingClientCleanup(level.dimension(), stable,
                allowProtectedLoli, CLIENT_CLEANUP_RETRIES - 1));
    }

    private static void sendClientCleanup(ServerLevel level, Collection<CleanupTarget> targets,
                                          boolean allowProtectedLoli) {
        if (level == null || targets == null || targets.isEmpty()) return;
        List<ForceEntityCleanupPacket.Target> packetTargets = new ArrayList<>();
        for (CleanupTarget target : targets) {
            if (target == null) continue;
             
             
             
            packetTargets.add(new ForceEntityCleanupPacket.Target(target.id, target.uuid));
        }
        if (packetTargets.isEmpty()) return;
        ForceEntityCleanupPacket packet = new ForceEntityCleanupPacket(packetTargets, allowProtectedLoli);
        for (ServerPlayer player : level.players()) {
            try {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            } catch (Throwable error) {
                LoliPickaxe.LOGGER.debug("Unable to send reliable Loli cleanup packet", error);
            }
        }
    }

     
    private static LinkedHashSet<Entity> resolveEntityGraph(Entity primary, Collection<?> prelinked) {
        LinkedHashSet<Entity> entities = new LinkedHashSet<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<Node> queue = new ArrayDeque<>();
        queue.add(new Node(primary, 0));
        if (prelinked != null) {
            for (Object linked : prelinked) if (linked != null) queue.add(new Node(linked, 0));
        }

        while (!queue.isEmpty() && visited.size() < MAX_GRAPH_OBJECTS) {
            Node node = queue.removeFirst();
            Object object = node.object;
            if (object == null || !visited.add(object)) continue;
            if (object instanceof Entity entity) entities.add(entity);
            if (node.depth >= MAX_GRAPH_DEPTH) continue;

            for (Method method : allMethods(object.getClass())) {
                if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) continue;
                if (method.getReturnType() == void.class || method.getReturnType().isPrimitive()) continue;
                String name = method.getName().toLowerCase(Locale.ROOT);
                if (!isLinkName(name)) continue;
                try {
                    method.setAccessible(true);
                    Object linked = method.invoke(object);
                    enqueueLinked(queue, linked, node.depth + 1);
                } catch (Throwable ignored) {
                }
            }

            for (Field field : allFields(object.getClass())) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                String name = field.getName().toLowerCase(Locale.ROOT);
                if (!isLinkName(name)) continue;
                try {
                    field.setAccessible(true);
                    enqueueLinked(queue, field.get(object), node.depth + 1);
                } catch (Throwable ignored) {
                }
            }
        }
        return entities;
    }

    private static void enqueueLinked(ArrayDeque<Node> queue, Object linked, int depth) {
        if (linked == null || linked instanceof Player || isBossUiObject(linked.getClass())) return;
        if (linked instanceof Entity) {
            queue.addLast(new Node(linked, depth));
            return;
        }
        if (linked instanceof Collection<?> collection) {
            int count = 0;
            for (Object element : collection) {
                if (element instanceof Entity && count++ < 32) queue.addLast(new Node(element, depth));
            }
            return;
        }
        if (linked.getClass().isArray() && !linked.getClass().getComponentType().isPrimitive()) {
            int length = Math.min(Array.getLength(linked), 32);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(linked, i);
                if (element instanceof Entity) queue.addLast(new Node(element, depth));
            }
        }
    }

    private static void purgeEntityManager(Object manager, Set<Entity> targets, Set<Integer> ids,
                                           Set<UUID> uuids) {
        if (manager == null) return;
        purgeContainer(readField(manager, "knownUuids", "f_157491_"), targets, ids, uuids, 0, 2);
        Object visible = readField(manager, "visibleEntityStorage", "f_157494_");
        purgeLookup(visible, targets, ids, uuids);
        Object adapter = readField(manager, "entityGetter", "f_157496_");
        if (adapter != null) {
            purgeLookup(readField(adapter, "visibleEntities", "visibleEntityStorage", "f_156940_"),
                    targets, ids, uuids);
            purgeSections(readField(adapter, "sectionStorage", "f_156941_"), targets, ids, uuids);
            purgeContainer(adapter, targets, ids, uuids, 0, 3);
        }
        purgeSections(readField(manager, "sectionStorage", "f_157495_"), targets, ids, uuids);
        purgeContainer(readField(manager, "loadingInbox", "f_157500_"), targets, ids, uuids, 0, 3);
    }

    private static void purgeLookup(Object lookup, Set<Entity> targets, Set<Integer> ids, Set<UUID> uuids) {
        if (lookup == null) return;
        purgeContainer(readField(lookup, "byId", "f_156807_", "f_156872_"), targets, ids, uuids, 0, 2);
        purgeContainer(readField(lookup, "byUuid", "f_156808_", "f_156873_"), targets, ids, uuids, 0, 2);
        purgeContainer(lookup, targets, ids, uuids, 0, 2);
    }

    private static void purgeSections(Object sectionStorage, Set<Entity> targets, Set<Integer> ids,
                                      Set<UUID> uuids) {
        if (sectionStorage == null) return;
        Object sections = readField(sectionStorage, "sections", "f_156852_");
        if (sections instanceof Map<?, ?> map) {
            for (Object section : List.copyOf(map.values())) {
                if (section == null) continue;
                for (Entity target : targets) invokeRemove(section, target);
                purgeContainer(section, targets, ids, uuids, 0, 3);
            }
        }
        purgeContainer(sectionStorage, targets, ids, uuids, 0, 2);
    }

    private static void purgeEntityTickList(Object tickList, Set<Entity> targets, Set<Integer> ids,
                                            Set<UUID> uuids) {
        if (tickList == null) return;
        purgeContainer(readField(tickList, "active", "entities", "f_156903_"), targets, ids, uuids, 0, 2);
        purgeContainer(readField(tickList, "passive", "temp", "f_156904_"), targets, ids, uuids, 0, 2);
        purgeContainer(readField(tickList, "iterated", "iterating", "f_156905_"), targets, ids, uuids, 0, 2);
        purgeContainer(tickList, targets, ids, uuids, 0, 2);
    }

    private static void purgeChunkMap(ServerLevel level, Set<Entity> targets, Set<Integer> ids,
                                      Set<UUID> uuids) {
        Object chunkSource = level.getChunkSource();
        Object chunkMap = readField(chunkSource, "chunkMap", "f_8325_");
        if (chunkMap == null) return;
        Object tracked = readField(chunkMap, "entityMap", "trackedEntities", "f_140150_");
        if (tracked instanceof Map<?, ?> map) {
            Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();
                Object wrapper = entry.getValue();
                Entity trackedEntity = findEntityReference(wrapper, 0, 2);
                boolean remove = matches(entry.getKey(), targets, ids, uuids)
                        || matches(wrapper, targets, ids, uuids)
                        || (trackedEntity != null && matches(trackedEntity, targets, ids, uuids));
                if (!remove) continue;
                clearSeenBy(wrapper);
                try {
                    iterator.remove();
                } catch (Throwable ignored) {
                }
            }
        }
        purgeContainer(tracked, targets, ids, uuids, 0, 3);
    }

    private static void detachDynamicListeners(ServerLevel level, Entity entity) {
        for (Field field : allFields(entity.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (!field.getType().getName().contains("DynamicGameEventListener")) continue;
            try {
                field.setAccessible(true);
                Object listener = field.get(entity);
                if (listener == null) continue;
                for (Method method : allMethods(listener.getClass())) {
                    if (!method.getName().equals("remove") || method.getParameterCount() != 1) continue;
                    Class<?> parameter = method.getParameterTypes()[0];
                    if (!parameter.isInstance(level) && !parameter.isAssignableFrom(level.getClass())) continue;
                    method.setAccessible(true);
                    method.invoke(listener, level);
                    break;
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void clearSeenBy(Object wrapper) {
        if (wrapper == null) return;
        Object seenBy = readField(wrapper, "seenBy", "f_140475_");
        if (seenBy instanceof Collection<?> collection) {
            try {
                collection.clear();
            } catch (Throwable ignored) {
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void purgeContainer(Object object, Set<Entity> targets, Set<Integer> ids,
                                       Set<UUID> uuids, int depth, int maxDepth) {
        if (object == null || isBossUiObject(object.getClass()) || depth > maxDepth) return;
        if (object instanceof Map map) {
            for (Integer id : ids) { try { map.remove(id); } catch (Throwable ignored) {} }
            for (UUID uuid : uuids) { try { map.remove(uuid); } catch (Throwable ignored) {} }
            try { map.values().removeIf(value -> matches(value, targets, ids, uuids)); }
            catch (Throwable ignored) {}
            Iterator<Map.Entry> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = iterator.next();
                if (matches(entry.getKey(), targets, ids, uuids)
                        || matches(entry.getValue(), targets, ids, uuids)) {
                    try {
                        iterator.remove();
                    } catch (Throwable ignored) {
                    }
                } else if (depth < maxDepth) {
                    purgeContainer(entry.getValue(), targets, ids, uuids, depth + 1, maxDepth);
                }
            }
            return;
        }
        if (object instanceof Collection collection) {
            try { collection.removeIf(value -> matches(value, targets, ids, uuids)); }
            catch (Throwable ignored) {}
            Iterator iterator = collection.iterator();
            while (iterator.hasNext()) {
                Object value = iterator.next();
                if (matches(value, targets, ids, uuids)) {
                    try { iterator.remove(); } catch (Throwable ignored) {}
                }
            }
            forceCompactArrayList(collection, targets, ids, uuids);
            return;
        }
        if (object.getClass().isArray() && !object.getClass().getComponentType().isPrimitive()) {
            for (int i = 0; i < Array.getLength(object); i++) {
                Object value = Array.get(object, i);
                if (matches(value, targets, ids, uuids)) {
                    try {
                        Array.set(object, i, null);
                    } catch (Throwable ignored) {
                    }
                }
            }
            return;
        }
        if (depth >= maxDepth || !isMinecraftContainer(object.getClass())) return;
        for (Field field : allFields(object.getClass())) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
            if (isBossUiName(field.getName()) || isBossUiObject(field.getType())) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(object);
                if (matches(value, targets, ids, uuids)) {
                    if (!Modifier.isFinal(field.getModifiers())) field.set(object, null);
                } else {
                    purgeContainer(value, targets, ids, uuids, depth + 1, maxDepth);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void forceCompactArrayList(Collection collection, Set<Entity> targets,
                                              Set<Integer> ids, Set<UUID> uuids) {
        if (!(collection instanceof java.util.ArrayList) || UNSAFE == null) return;
        try {
            Field elementData = java.util.ArrayList.class.getDeclaredField("elementData");
            Field sizeField = java.util.ArrayList.class.getDeclaredField("size");
            long dataOffset = UNSAFE.objectFieldOffset(elementData);
            long sizeOffset = UNSAFE.objectFieldOffset(sizeField);
            Object[] data = (Object[]) UNSAFE.getObject(collection, dataOffset);
            int size = UNSAFE.getInt(collection, sizeOffset);
            int write = 0;
            for (int read = 0; read < size; read++) {
                Object value = data[read];
                if (!matches(value, targets, ids, uuids)) data[write++] = value;
            }
            for (int i = write; i < size; i++) data[i] = null;
            if (write != size) UNSAFE.putIntVolatile(collection, sizeOffset, write);
        } catch (Throwable ignored) {
        }
    }

    private static boolean containsTarget(Object object, Set<Entity> targets, Set<Integer> ids,
                                          Set<UUID> uuids, int depth, int maxDepth) {
        if (object == null || isBossUiObject(object.getClass()) || depth > maxDepth) return false;
        if (matches(object, targets, ids, uuids)) return true;
        if (object instanceof Map<?, ?> map) {
            for (Object key : map.keySet()) if (matches(key, targets, ids, uuids)) return true;
            for (Object value : map.values()) {
                if (matches(value, targets, ids, uuids)) return true;
                if (depth < maxDepth && containsTarget(value, targets, ids, uuids, depth + 1, maxDepth)) return true;
            }
            return false;
        }
        if (object instanceof Collection<?> collection) {
            for (Object value : collection) if (matches(value, targets, ids, uuids)) return true;
            return false;
        }
        if (object.getClass().isArray() && !object.getClass().getComponentType().isPrimitive()) {
            for (int i = 0; i < Array.getLength(object); i++) {
                if (matches(Array.get(object, i), targets, ids, uuids)) return true;
            }
            return false;
        }
        if (depth >= maxDepth || !isMinecraftContainer(object.getClass())) return false;
        for (Field field : allFields(object.getClass())) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
            if (isBossUiName(field.getName()) || isBossUiObject(field.getType())) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(object);
                if (containsTarget(value, targets, ids, uuids, depth + 1, maxDepth)) return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }

    private static boolean matches(Object value, Set<Entity> targets, Set<Integer> ids, Set<UUID> uuids) {
        if (value == null) return false;
        if (value instanceof Entity entity) {
            return targets.contains(entity) || ids.contains(entity.getId()) || uuids.contains(entity.getUUID());
        }
        if (value instanceof Integer id) return ids.contains(id);
        if (value instanceof UUID uuid) return uuids.contains(uuid);
        return false;
    }

    private static Entity findEntityReference(Object object, int depth, int maxDepth) {
        if (object == null || isBossUiObject(object.getClass()) || depth > maxDepth) return null;
        if (object instanceof Entity entity) return entity;
        for (Field field : allFields(object.getClass())) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
            String name = field.getName().toLowerCase(Locale.ROOT);
            if (!name.contains("entity") && !name.contains("target") && !name.contains("owner")) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(object);
                Entity entity = findEntityReference(value, depth + 1, maxDepth);
                if (entity != null) return entity;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static void invokeRemove(Object owner, Entity entity) {
        if (owner == null || entity == null) return;
        for (Method method : allMethods(owner.getClass())) {
            if (!method.getName().equals("remove") || method.getParameterCount() != 1) continue;
            if (!method.getParameterTypes()[0].isInstance(entity)) continue;
            try {
                method.setAccessible(true);
                method.invoke(owner, entity);
                return;
            } catch (Throwable ignored) {
            }
        }
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

    private static List<Field> allFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            Collections.addAll(fields, current.getDeclaredFields());
        }
        return fields;
    }

    private static List<Method> allMethods(Class<?> type) {
        List<Method> methods = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            Collections.addAll(methods, current.getDeclaredMethods());
        }
        return methods;
    }

    private static boolean isBossUiName(String raw) {
        if (raw == null) return false;
        String name = raw.toLowerCase(Locale.ROOT).replace("_", "").replace("$", "");
        return name.contains("bossbar") || name.contains("bossevent") || name.contains("bossinfo")
                || name.contains("bosshealthoverlay") || name.contains("healthbar")
                || name.contains("progressbar") || name.contains("hudoverlay")
                || name.contains("bossmusic");
    }

    private static boolean isBossUiObject(Class<?> type) {
        if (type == null || Entity.class.isAssignableFrom(type)) return false;
        String name = type.getName().toLowerCase(Locale.ROOT).replace("_", "").replace("$", "");
        return name.equals("net.minecraft.world.bossevent")
                || name.equals("net.minecraft.server.level.serverbossevent")
                || isBossUiName(name);
    }

    private static boolean isLinkName(String name) {
        return name.equals("getparent") || name.equals("getowner") || name.equals("gethost")
                || name.equals("getmaster") || name.equals("getcontroller") || name.equals("getroot")
                || name.equals("gettrueentity") || name.equals("getmainentity") || name.equals("getcore")
                || name.contains("parent") || name.contains("owner") || name.contains("host")
                || name.contains("master") || name.contains("proxy") || name.contains("controller")
                || name.contains("delegate") || name.contains("root") || name.contains("body")
                || name.contains("core") || name.contains("multipart") || name.contains("part");
    }

    private static boolean isMinecraftContainer(Class<?> type) {
        String name = type.getName();
        return name.startsWith("net.minecraft.") || name.startsWith("net.minecraftforge.")
                || name.startsWith("it.unimi.dsi.fastutil.") || Map.class.isAssignableFrom(type)
                || Collection.class.isAssignableFrom(type);
    }

    private static void sendRemovePacket(ServerLevel level, Set<Integer> ids) {
        if (ids.isEmpty()) return;
        int[] array = ids.stream().mapToInt(Integer::intValue).toArray();
        ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(array);
        for (ServerPlayer player : level.players()) {
            try {
                player.connection.send(packet);
            } catch (Throwable error) {
                LoliPickaxe.LOGGER.debug("Unable to send Loli structural removal packet", error);
            }
        }
    }

    private static Unsafe findUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private record CleanupTarget(int id, UUID uuid) {}

    private static final class PendingClientCleanup {
        private final ResourceKey<Level> dimension;
        private final List<CleanupTarget> targets;
        private final boolean allowProtectedLoli;
        private int remaining;

        private PendingClientCleanup(ResourceKey<Level> dimension, List<CleanupTarget> targets,
                                     boolean allowProtectedLoli, int remaining) {
            this.dimension = dimension;
            this.targets = targets;
            this.allowProtectedLoli = allowProtectedLoli;
            this.remaining = remaining;
        }
    }

    private record Node(Object object, int depth) {}

    private LoliStructuralPurge() {}
}
