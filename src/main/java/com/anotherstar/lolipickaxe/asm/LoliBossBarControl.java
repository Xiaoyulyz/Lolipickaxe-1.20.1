package com.anotherstar.lolipickaxe.asm;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.ref.WeakReference;
import java.security.CodeSource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

 
public final class LoliBossBarControl {
    private static final int MAX_OBJECTS = 48;
    private static final int MAX_DEPTH = 2;
    private static final int EMPTY_CACHE_RESCAN_TICKS = 20;
    private static final Map<UUID, CacheEntry> CACHE = new ConcurrentHashMap<>();

     
    public static List<UUID> zero(Entity target) {
        return control(target, false);
    }

     
    public static List<UUID> detach(Entity target) {
        return control(target, true);
    }

     
    public static List<UUID> suppress(Entity target) {
        return detach(target);
    }

    private static List<UUID> control(Entity target, boolean detach) {
        if (target == null) return List.of();
        UUID targetId = target.getUUID();
        List<BossEvent> events = cachedEvents(target);
        Set<UUID> ids = new LinkedHashSet<>();
        for (BossEvent event : events) {
            invoke(event, new String[]{"setProgress", "m_142711_"},
                    new Class<?>[]{float.class}, 0.0F);
            Object id = invoke(event, new String[]{"getId", "m_18891_"}, new Class<?>[0]);
            if (id instanceof UUID uuid) ids.add(uuid);
            if (detach && event instanceof ServerBossEvent) {
                invoke(event, new String[]{"setVisible", "m_28984_"},
                        new Class<?>[]{boolean.class}, false);
                invoke(event, new String[]{"removeAllPlayers", "m_6546_"}, new Class<?>[0]);
            }
        }
        if (detach) CACHE.remove(targetId);
        return List.copyOf(ids);
    }

    private static List<BossEvent> cachedEvents(Entity target) {
        UUID uuid = target.getUUID();
        int tick = target.tickCount;
        CacheEntry cached = CACHE.get(uuid);
        List<BossEvent> live = cached == null ? List.of() : cached.liveEvents();
        boolean rescan = cached == null || (live.isEmpty()
                && tick - cached.lastScanTick >= EMPTY_CACHE_RESCAN_TICKS);
        if (!rescan) return live;

        List<BossEvent> discovered = new ArrayList<>();
        for (Object object : discover(target)) {
            if (object instanceof BossEvent event) discovered.add(event);
        }
        CACHE.put(uuid, CacheEntry.of(discovered, tick));
        return discovered;
    }

    public static void release(UUID uuid) {
        if (uuid != null) CACHE.remove(uuid);
    }

    public static void clear() {
        CACHE.clear();
    }

    private static Object invoke(Object owner, String[] names, Class<?>[] parameters, Object... args) {
        if (owner == null) return null;
        for (Class<?> type = owner.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (String name : names) {
                try {
                    var method = type.getDeclaredMethod(name, parameters);
                    method.setAccessible(true);
                    return method.invoke(owner, args);
                } catch (NoSuchMethodException ignored) {
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static List<Object> discover(Entity target) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Object> result = new ArrayList<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        visited.add(target);
        queue.add(new Node(target, 0));

        while (!queue.isEmpty() && visited.size() < MAX_OBJECTS) {
            Node node = queue.removeFirst();
            Object owner = node.value;
            if (owner instanceof BossEvent) result.add(owner);
            if (node.depth >= MAX_DEPTH) continue;

            for (Class<?> type = owner.getClass(); type != null && type != Object.class;
                 type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                    try {
                        field.setAccessible(true);
                        enqueue(target, field.get(owner), node.depth + 1, visited, result, queue);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return result;
    }

    private static void enqueue(Entity target, Object value, int depth, Set<Object> visited,
                                List<Object> result, ArrayDeque<Node> queue) {
        if (value == null || value == target || value instanceof Number || value instanceof CharSequence
                || value instanceof Class<?> || value.getClass().isEnum()) return;
        if (value instanceof BossEvent) {
            if (visited.add(value)) result.add(value);
            return;
        }
        if (value instanceof Collection<?> collection) {
            int count = 0;
            for (Object element : collection) {
                if (count++ >= 24) break;
                enqueue(target, element, depth, visited, result, queue);
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            int count = 0;
            for (Object element : map.values()) {
                if (count++ >= 24) break;
                enqueue(target, element, depth, visited, result, queue);
            }
            return;
        }
        if (value.getClass().isArray() && !value.getClass().getComponentType().isPrimitive()) {
            int length = Math.min(Array.getLength(value), 24);
            for (int i = 0; i < length; i++) {
                enqueue(target, Array.get(value, i), depth, visited, result, queue);
            }
            return;
        }
        if (value instanceof Entity || !sameCodeSource(target.getClass(), value.getClass())) return;
        if (visited.add(value)) queue.addLast(new Node(value, depth));
    }

    private static boolean sameCodeSource(Class<?> target, Class<?> candidate) {
        if (target == null || candidate == null || target.getClassLoader() != candidate.getClassLoader()) {
            return false;
        }
        String name = candidate.getName();
        if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.")
                || name.startsWith("net.minecraft.") || name.startsWith("net.minecraftforge.")) {
            return false;
        }
        try {
            CodeSource left = target.getProtectionDomain().getCodeSource();
            CodeSource right = candidate.getProtectionDomain().getCodeSource();
            return left != null && right != null && left.getLocation().equals(right.getLocation());
        } catch (Throwable ignored) {
            return false;
        }
    }


    private static final class CacheEntry {
        private final List<WeakReference<BossEvent>> events;
        private final int lastScanTick;

        private CacheEntry(List<WeakReference<BossEvent>> events, int lastScanTick) {
            this.events = events;
            this.lastScanTick = lastScanTick;
        }

        private static CacheEntry of(List<BossEvent> events, int tick) {
            List<WeakReference<BossEvent>> references = new ArrayList<>();
            for (BossEvent event : events) references.add(new WeakReference<>(event));
            return new CacheEntry(List.copyOf(references), tick);
        }

        private List<BossEvent> liveEvents() {
            List<BossEvent> live = new ArrayList<>();
            for (WeakReference<BossEvent> reference : events) {
                BossEvent event = reference.get();
                if (event != null) live.add(event);
            }
            return live;
        }
    }

    private record Node(Object value, int depth) {
    }

    private LoliBossBarControl() {
    }
}
