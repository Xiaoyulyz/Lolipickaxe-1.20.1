package com.anotherstar.lolipickaxe.asm;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import sun.misc.Unsafe;

import java.io.File;
import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
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
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public final class LoliExternalLifecycleBridge {
    private static final Unsafe UNSAFE = findUnsafe();
    private static final int MAX_OWNERS = 64;
    private static final int MAX_LINK_DEPTH = 3;
    private static final int MAX_CANDIDATE_CLASSES = 256;
    private static final int MAX_CONTAINER_DEPTH = 6;

    private static final String[] LIFECYCLE_CLASS_TOKENS = {
            "respawn", "reviv", "rebirth", "resurrect", "recreate", "lifecycle",
            "registry", "manager", "storage", "tracker", "container", "level"
    };
    private static final String[] PRIVILEGE_CLASS_TOKENS = {
            "accesschecker", "accesscontrol", "permission", "privilege", "authorization", "guard"
    };
    private static final String[] RETIRE_METHOD_TOKENS = {
            "remove", "unregister", "retire", "invalidate", "delete", "despawn", "destroy", "erase"
    };

    public static void retireTargetGraph(Entity primary, Collection<?> prelinked) {
        if (primary == null || primary instanceof Player) return;
        LinkedHashSet<Object> owners = discoverOwners(primary, prelinked);
        if (owners.isEmpty()) return;

        LinkedHashSet<Class<?>> candidates = new LinkedHashSet<>();
        for (Object owner : owners) collectCandidateClasses(owner.getClass(), candidates);

        Runnable directRetirement = () -> {
            for (Class<?> candidate : candidates) {
                invokeRetirementMethods(candidate, owners);
                purgeStaticFields(candidate, owners);
            }
             
             
            for (Object owner : owners) purgeOwnerTypeStatics(owner, owners);
        };

        runWithDiscoveredPrivilege(candidates, directRetirement);
         
         
         
        directRetirement.run();
    }

    private static LinkedHashSet<Object> discoverOwners(Object primary, Collection<?> prelinked) {
        LinkedHashSet<Object> result = new LinkedHashSet<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<LinkNode> queue = new ArrayDeque<>();
        queue.addLast(new LinkNode(primary, 0));
        if (prelinked != null) {
            for (Object value : prelinked) if (value != null) queue.addLast(new LinkNode(value, 0));
        }

        while (!queue.isEmpty() && visited.size() < MAX_OWNERS) {
            LinkNode node = queue.removeFirst();
            Object value = node.value;
            if (value == null || value instanceof Player || isBossUiObject(value.getClass())
                    || !visited.add(value)) continue;
            result.add(value);
            if (node.depth >= MAX_LINK_DEPTH) continue;

            for (Field field : allFields(value.getClass())) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                if (!isOwnerLinkName(field.getName())) continue;
                Object linked = readFieldValue(value, field);
                enqueueLinked(queue, linked, node.depth + 1);
            }
            for (Method method : allMethods(value.getClass())) {
                if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0
                        || method.getReturnType() == void.class || method.getReturnType().isPrimitive()) continue;
                if (!isOwnerLinkName(method.getName())) continue;
                try {
                    method.setAccessible(true);
                    enqueueLinked(queue, method.invoke(value), node.depth + 1);
                } catch (Throwable ignored) {
                }
            }
        }
        return result;
    }

    private static void enqueueLinked(ArrayDeque<LinkNode> queue, Object linked, int depth) {
        if (linked == null || linked instanceof Player || isBossUiObject(linked.getClass())) return;
        if (linked instanceof Collection<?> collection) {
            int count = 0;
            for (Object value : collection) {
                if (count++ >= 32) break;
                if (value != null && !isBossUiObject(value.getClass())) queue.addLast(new LinkNode(value, depth));
            }
            return;
        }
        if (linked instanceof Map<?, ?> map) {
            int count = 0;
            for (Object value : map.values()) {
                if (count++ >= 32) break;
                if (value != null && !isBossUiObject(value.getClass())) queue.addLast(new LinkNode(value, depth));
            }
            return;
        }
        if (linked.getClass().isArray() && !linked.getClass().getComponentType().isPrimitive()) {
            int length = Math.min(Array.getLength(linked), 32);
            for (int i = 0; i < length; i++) {
                Object value = Array.get(linked, i);
                if (value != null && !isBossUiObject(value.getClass())) queue.addLast(new LinkNode(value, depth));
            }
            return;
        }
        if (isExternalObject(linked)) queue.addLast(new LinkNode(linked, depth));
    }

    private static void collectCandidateClasses(Class<?> origin, Set<Class<?>> output) {
        if (origin == null || !isExternalClass(origin)) return;
        ClassLoader loader = origin.getClassLoader();
        for (Class<?> type = origin; type != null && type != Object.class; type = type.getSuperclass()) {
            output.add(type);
            collectInterfaces(type, output);
            if (type.getEnclosingClass() != null) output.add(type.getEnclosingClass());
            try { Collections.addAll(output, type.getNestMembers()); } catch (Throwable ignored) {}
        }

        CodeSource source = origin.getProtectionDomain() == null ? null : origin.getProtectionDomain().getCodeSource();
        URL location = source == null ? null : source.getLocation();
        if (location == null) return;
        try {
            URI uri = location.toURI();
            File root = new File(uri);
            if (root.isFile()) scanJar(root, loader, output);
            else if (root.isDirectory()) scanDirectory(root, root, loader, output);
        } catch (Throwable error) {
            LoliPickaxe.LOGGER.debug("Unable to scan target lifecycle code source for {}", origin.getName(), error);
        }
    }

    private static void collectInterfaces(Class<?> type, Set<Class<?>> output) {
        for (Class<?> iface : type.getInterfaces()) {
            if (output.add(iface)) collectInterfaces(iface, output);
        }
    }

    private static void scanJar(File file, ClassLoader loader, Set<Class<?>> output) {
        try (JarFile jar = new JarFile(file)) {
            Iterator<JarEntry> iterator = jar.entries().asIterator();
            while (iterator.hasNext() && output.size() < MAX_CANDIDATE_CLASSES) {
                String name = iterator.next().getName();
                if (!name.endsWith(".class") || name.equals("module-info.class")) continue;
                String className = name.substring(0, name.length() - 6).replace('/', '.');
                addScannedCandidate(className, loader, output);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void scanDirectory(File root, File current, ClassLoader loader, Set<Class<?>> output) {
        File[] files = current.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (output.size() >= MAX_CANDIDATE_CLASSES) return;
            if (file.isDirectory()) {
                scanDirectory(root, file, loader, output);
            } else if (file.getName().endsWith(".class")) {
                String relative = root.toPath().relativize(file.toPath()).toString();
                String className = relative.substring(0, relative.length() - 6)
                        .replace(File.separatorChar, '.');
                addScannedCandidate(className, loader, output);
            }
        }
    }

    private static void addScannedCandidate(String className, ClassLoader loader, Set<Class<?>> output) {
        String lower = className.toLowerCase(Locale.ROOT);
        if (lower.contains(".client.") || lower.contains(".render") || lower.contains(".model")
                || lower.contains(".screen") || lower.contains(".network") || lower.contains("mixin")) return;
        if (!containsAny(lower, LIFECYCLE_CLASS_TOKENS) && !containsAny(lower, PRIVILEGE_CLASS_TOKENS)) return;
        try {
            Class<?> type = Class.forName(className, false, loader);
            if (isExternalClass(type)) output.add(type);
        } catch (Throwable ignored) {
        }
    }

    private static void invokeRetirementMethods(Class<?> candidate, Set<Object> owners) {
        if (isBossUiObject(candidate)) return;
        for (Method method : allMethods(candidate)) {
            if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) continue;
            String lower = method.getName().toLowerCase(Locale.ROOT);
            if (!containsAny(lower, RETIRE_METHOD_TOKENS)) continue;
            Class<?> parameter = method.getParameterTypes()[0];
            for (Object owner : owners) {
                if (owner == null || !parameter.isInstance(owner)) continue;
                try {
                    method.setAccessible(true);
                    method.invoke(null, owner);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void purgeStaticFields(Class<?> candidate, Set<Object> owners) {
        if (isBossUiObject(candidate)) return;
        for (Field field : candidate.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
            if (isBossUiName(field.getName()) || isBossUiObject(field.getType())) continue;
            Object container = readStaticFieldValue(field);
            if (container == null) continue;
            removeIdentities(container, owners, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
        }
    }

    private static void purgeOwnerTypeStatics(Object owner, Set<Object> owners) {
        LinkedHashSet<Class<?>> classes = new LinkedHashSet<>();
        for (Class<?> type = owner.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            classes.add(type);
            collectInterfaces(type, classes);
            if (type.getEnclosingClass() != null) classes.add(type.getEnclosingClass());
            try { Collections.addAll(classes, type.getNestMembers()); } catch (Throwable ignored) {}
        }
        for (Class<?> type : classes) purgeStaticFields(type, owners);
    }

    private static void runWithDiscoveredPrivilege(Set<Class<?>> candidates, Runnable action) {
        for (Class<?> candidate : candidates) {
            String className = candidate.getName().toLowerCase(Locale.ROOT);
            if (!containsAny(className, PRIVILEGE_CLASS_TOKENS)) continue;
            for (Method method : candidate.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) continue;
                String name = method.getName().toLowerCase(Locale.ROOT);
                if (!(name.contains("privileg") || name.contains("authoriz") || name.contains("internal"))) continue;
                Class<?> parameter = method.getParameterTypes()[0];
                try {
                    method.setAccessible(true);
                    if (Runnable.class.isAssignableFrom(parameter)) {
                        method.invoke(null, action);
                        return;
                    }
                    if (Supplier.class.isAssignableFrom(parameter)) {
                        method.invoke(null, (Supplier<Object>) () -> { action.run(); return null; });
                        return;
                    }
                    if (Callable.class.isAssignableFrom(parameter)) {
                        method.invoke(null, (Callable<Object>) () -> { action.run(); return null; });
                        return;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        action.run();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean removeIdentities(Object container, Set<Object> owners, Set<Object> visited, int depth) {
        if (container == null || isBossUiObject(container.getClass())
                || depth > MAX_CONTAINER_DEPTH || owners.contains(container) || !visited.add(container)) {
            return owners.contains(container);
        }
        boolean changed = false;

        if (container instanceof Map map) {
            for (Object owner : owners) {
                try { changed |= map.remove(owner) != null; } catch (Throwable ignored) {}
            }
            try {
                Iterator<Map.Entry> iterator = map.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry entry = iterator.next();
                    if (owners.contains(entry.getKey()) || owners.contains(entry.getValue())) {
                        try { iterator.remove(); changed = true; } catch (Throwable ignored) {}
                    } else {
                        changed |= removeIdentities(entry.getValue(), owners, visited, depth + 1);
                    }
                }
            } catch (Throwable ignored) {
            }
            changed |= rawHashTablePrune(container, owners);
        } else if (container instanceof Collection collection) {
            try { changed |= collection.removeIf(owners::contains); } catch (Throwable ignored) {}
            try {
                Iterator iterator = collection.iterator();
                while (iterator.hasNext()) {
                    Object value = iterator.next();
                    if (owners.contains(value)) {
                        try { iterator.remove(); changed = true; } catch (Throwable ignored) {}
                    }
                }
            } catch (Throwable ignored) {
            }
            changed |= rawArrayListPrune(collection, owners);
        } else if (container.getClass().isArray() && !container.getClass().getComponentType().isPrimitive()) {
            for (int i = 0; i < Array.getLength(container); i++) {
                Object value = Array.get(container, i);
                if (owners.contains(value)) {
                    try { Array.set(container, i, null); changed = true; } catch (Throwable ignored) {}
                } else if (value != null && isBackingObject(value.getClass())) {
                    changed |= removeIdentities(value, owners, visited, depth + 1);
                }
            }
        }

        if (depth >= MAX_CONTAINER_DEPTH || !isBackingObject(container.getClass())) return changed;
        for (Field field : allFields(container.getClass())) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
            String name = field.getName().toLowerCase(Locale.ROOT);
            if (isBossUiName(name) || isBossUiObject(field.getType())) continue;
            if (!isBackingFieldName(name) && !(container instanceof Map) && !(container instanceof Collection)) continue;
            Object value = readFieldValue(container, field);
            if (value == null) continue;
            if (owners.contains(value)) {
                if (writeObjectField(container, field, null)) changed = true;
            } else {
                changed |= removeIdentities(value, owners, visited, depth + 1);
            }
        }
        return changed;
    }

    private static boolean rawArrayListPrune(Object list, Set<Object> owners) {
        if (!(list instanceof ArrayList) || UNSAFE == null) return false;
        try {
            Field dataField = findField(ArrayList.class, "elementData");
            Field sizeField = findField(ArrayList.class, "size");
            if (dataField == null || sizeField == null) return false;
            Object[] data = (Object[]) UNSAFE.getObject(list, UNSAFE.objectFieldOffset(dataField));
            long sizeOffset = UNSAFE.objectFieldOffset(sizeField);
            int size = UNSAFE.getInt(list, sizeOffset);
            int write = 0;
            for (int read = 0; read < size; read++) {
                Object value = data[read];
                if (!owners.contains(value)) data[write++] = value;
            }
            for (int i = write; i < size; i++) data[i] = null;
            if (write == size) return false;
            UNSAFE.putIntVolatile(list, sizeOffset, write);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

     
    private static boolean rawHashTablePrune(Object map, Set<Object> owners) {
        if (UNSAFE == null) return false;
        Field tableField = findFieldByName(map.getClass(), "table");
        if (tableField == null || !tableField.getType().isArray()) return false;
        Object table = readFieldValue(map, tableField);
        if (table == null || !table.getClass().isArray()) return false;
        int removed = 0;
        for (int bucket = 0; bucket < Array.getLength(table); bucket++) {
            Object head = Array.get(table, bucket);
            Object previous = null;
            Object node = head;
            int guard = 0;
            while (node != null && guard++ < 100000) {
                Field nextField = findFieldByName(node.getClass(), "next");
                Object next = nextField == null ? null : readFieldValue(node, nextField);
                Object key = node instanceof Reference<?> reference ? reference.get()
                        : readNamedField(node, "key");
                Object value = readNamedField(node, "value");
                boolean remove = owners.contains(key) || owners.contains(value) || owners.contains(node);
                if (remove) {
                    removed++;
                    if (previous == null) Array.set(table, bucket, next);
                    else {
                        Field previousNext = findFieldByName(previous.getClass(), "next");
                        if (previousNext != null) writeObjectField(previous, previousNext, next);
                    }
                } else {
                    previous = node;
                }
                node = next;
            }
        }
        if (removed <= 0) return false;
        Field sizeField = findFieldByName(map.getClass(), "size");
        if (sizeField != null && sizeField.getType() == int.class) {
            try {
                long offset = UNSAFE.objectFieldOffset(sizeField);
                int old = UNSAFE.getInt(map, offset);
                UNSAFE.putIntVolatile(map, offset, Math.max(0, old - removed));
            } catch (Throwable ignored) {}
        }
        return true;
    }

    private static Object readNamedField(Object owner, String name) {
        Field field = findFieldByName(owner.getClass(), name);
        return field == null ? null : readFieldValue(owner, field);
    }

    private static Object readStaticFieldValue(Field field) {
         
         
         
        if (UNSAFE != null) {
            try {
                return UNSAFE.getObject(UNSAFE.staticFieldBase(field), UNSAFE.staticFieldOffset(field));
            } catch (Throwable ignored) {
            }
        }
        try {
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object readFieldValue(Object owner, Field field) {
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (Throwable ignored) {
            if (UNSAFE == null || Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) return null;
            try { return UNSAFE.getObject(owner, UNSAFE.objectFieldOffset(field)); }
            catch (Throwable ignoredAgain) { return null; }
        }
    }

    private static boolean writeObjectField(Object owner, Field field, Object value) {
        if (owner == null || field == null || field.getType().isPrimitive()) return false;
        try {
            field.setAccessible(true);
            field.set(owner, value);
            return true;
        } catch (Throwable ignored) {
            if (UNSAFE == null || Modifier.isStatic(field.getModifiers())) return false;
            try {
                UNSAFE.putObjectVolatile(owner, UNSAFE.objectFieldOffset(field), value);
                return true;
            } catch (Throwable ignoredAgain) {
                return false;
            }
        }
    }

    private static Field findField(Class<?> type, String name) {
        try { return type.getDeclaredField(name); } catch (Throwable ignored) { return null; }
    }

    private static Field findFieldByName(Class<?> type, String name) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            try { return current.getDeclaredField(name); } catch (Throwable ignored) {}
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
                || name.contains("healthbar") || name.contains("progressbar")
                || name.contains("bossoverlay") || name.contains("bossmusic")
                || name.contains("hudoverlay");
    }

    private static boolean isBossUiObject(Class<?> type) {
        if (type == null) return false;
        String name = type.getName().toLowerCase(Locale.ROOT).replace("_", "").replace("$", "");
        return name.equals("net.minecraft.world.bossevent")
                || name.equals("net.minecraft.server.level.serverbossevent")
                || isBossUiName(name);
    }

    private static boolean isOwnerLinkName(String raw) {
        String name = raw.toLowerCase(Locale.ROOT);
        return name.contains("proxy") || name.contains("owner") || name.contains("parent")
                || name.contains("master") || name.contains("controller") || name.contains("root")
                || name.contains("host") || name.contains("body") || name.contains("core")
                || name.contains("trueentity") || name.contains("mainentity") || name.contains("respawn")
                || name.contains("delegate") || name.contains("lifecycle");
    }

    private static boolean isBackingFieldName(String name) {
        return name.contains("backing") || name.contains("delegate") || name.contains("wrapped")
                || name.contains("map") || name.contains("list") || name.contains("table")
                || name.contains("entry") || name.contains("entries") || name.contains("element")
                || name.contains("array") || name.contains("storage") || name.contains("container")
                || name.contains("values") || name.contains("keys") || name.contains("data");
    }

    private static boolean isBackingObject(Class<?> type) {
        if (type == null) return false;
        String name = type.getName();
        return Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type)
                || name.startsWith("java.util.") || name.startsWith("it.unimi.dsi.fastutil.")
                || (!name.startsWith("java.") && !name.startsWith("javax.")
                && !name.startsWith("net.minecraft.") && !name.startsWith("net.minecraftforge."));
    }

    private static boolean isExternalObject(Object value) {
        return value instanceof Entity || isExternalClass(value.getClass());
    }

    private static boolean isExternalClass(Class<?> type) {
        if (type == null || isBossUiObject(type)) return false;
        String name = type.getName();
        return !name.startsWith("java.") && !name.startsWith("javax.")
                && !name.startsWith("jdk.") && !name.startsWith("sun.")
                && !name.startsWith("net.minecraft.") && !name.startsWith("net.minecraftforge.")
                && !name.startsWith("com.anotherstar.lolipickaxe.");
    }

    private static boolean containsAny(String value, String[] tokens) {
        for (String token : tokens) if (value.contains(token)) return true;
        return false;
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

    private record LinkNode(Object value, int depth) {}

    private LoliExternalLifecycleBridge() {}
}
