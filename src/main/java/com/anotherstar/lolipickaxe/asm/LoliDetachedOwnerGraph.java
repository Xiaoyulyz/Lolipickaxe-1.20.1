package com.anotherstar.lolipickaxe.asm;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import sun.misc.Unsafe;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.CodeSource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public final class LoliDetachedOwnerGraph {
    private static final Unsafe UNSAFE = findUnsafe();
    private static final int MAX_OBJECTS = 48;
    private static final int MAX_DEPTH = 2;
    private static final int MAX_STATIC_REFS = 96;

    public static boolean hasDetachedOwner(Entity proxy, Collection<?> seeds) {
        if (proxy == null) return false;
        for (Object candidate : discover(proxy, seeds)) {
            if (candidate != null && !(candidate instanceof Entity) && ownerScore(proxy, candidate) >= 5) {
                return true;
            }
        }
        return false;
    }

    public static boolean detach(Entity proxy, Collection<?> seeds) {
        if (proxy == null) return false;
        boolean changed = false;
        for (Object owner : discover(proxy, seeds)) {
            if (owner == null || owner instanceof Entity || ownerScore(proxy, owner) < 5) continue;
            changed |= detachBossEvents(owner);
            changed |= markTerminalState(owner);
            changed |= invokeTerminalMethods(owner);
            changed |= removeFromReferencedStatics(owner);
        }
        return changed;
    }

    private static List<Object> discover(Entity proxy, Collection<?> seeds) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Object> result = new ArrayList<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        visited.add(proxy);
        queue.add(new Node(proxy, 0));

        if (seeds != null) {
            for (Object seed : seeds) enqueue(proxy, seed, 0, visited, result, queue);
        }

        while (!queue.isEmpty() && visited.size() < MAX_OBJECTS) {
            Node node = queue.removeFirst();
            if (node.depth >= MAX_DEPTH) continue;
            for (Class<?> type = node.value.getClass(); type != null && type != Object.class;
                 type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                    try {
                        Object value = LoliUnsafeBridge.readFieldValue(node.value, field);
                        enqueue(proxy, value, node.depth + 1, visited, result, queue);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return result;
    }

    private static void enqueue(Entity proxy, Object value, int depth,
                                Set<Object> visited, List<Object> result, ArrayDeque<Node> queue) {
        if (value == null || value == proxy || value instanceof Level || value instanceof Class<?>
                || value instanceof Number || value instanceof CharSequence || value.getClass().isEnum()) return;
        if (value instanceof Collection<?> collection) {
            int count = 0;
            for (Object element : collection) {
                if (count++ >= 24) break;
                enqueue(proxy, element, depth, visited, result, queue);
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            int count = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (count++ >= 24) break;
                enqueue(proxy, entry.getKey(), depth, visited, result, queue);
                enqueue(proxy, entry.getValue(), depth, visited, result, queue);
            }
            return;
        }
        if (value.getClass().isArray() && !value.getClass().getComponentType().isPrimitive()) {
            int length = Math.min(Array.getLength(value), 24);
            for (int i = 0; i < length; i++) enqueue(proxy, Array.get(value, i), depth, visited, result, queue);
            return;
        }
        if (!sameModCode(proxy.getClass(), value.getClass())) return;
        if (visited.add(value)) {
            result.add(value);
            queue.addLast(new Node(value, depth));
        }
    }

    private static int ownerScore(Entity proxy, Object owner) {
        int score = 0;
        boolean referencesProxy = false;
        boolean sameLevel = false;
        boolean positionState = false;
        boolean identityState = false;
        boolean tickLike = false;
        for (Class<?> type = owner.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                Class<?> fieldType = field.getType();
                if (Entity.class.isAssignableFrom(fieldType)) {
                    Object value = LoliUnsafeBridge.readFieldValue(owner, field);
                    if (value == proxy) referencesProxy = true;
                } else if (Level.class.isAssignableFrom(fieldType)) {
                    Object value = LoliUnsafeBridge.readFieldValue(owner, field);
                    if (value == proxy.level()) sameLevel = true;
                } else if (fieldType == Vec3.class || fieldType == AABB.class) {
                    positionState = true;
                } else if (fieldType == java.util.UUID.class) {
                    identityState = true;
                }
            }
            for (Method method : type.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) continue;
                String name = normalize(method.getName());
                if (name.equals("tick") || name.endsWith("tick") || name.contains("update")) tickLike = true;
            }
        }
        if (referencesProxy) score += 5;
        if (sameLevel) score += 2;
        if (positionState) score += 1;
        if (identityState) score += 1;
        if (tickLike) score += 1;
        return score;
    }

    private static boolean detachBossEvents(Object owner) {
        boolean changed = false;
        for (Class<?> type = owner.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                Object value = LoliUnsafeBridge.readFieldValue(owner, field);
                if (!(value instanceof ServerBossEvent event)) continue;
                try { event.setProgress(0.0F); changed = true; } catch (Throwable ignored) {}
                try { event.setVisible(false); changed = true; } catch (Throwable ignored) {}
                try { event.removeAllPlayers(); changed = true; } catch (Throwable ignored) {}
            }
        }
        return changed;
    }

    private static boolean markTerminalState(Object owner) {
        boolean changed = false;
        for (Class<?> type = owner.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType() != boolean.class) continue;
                String name = normalize(field.getName());
                Boolean value = null;
                if (name.equals("removed") || name.equals("dead") || name.equals("destroyed")
                        || name.equals("terminated") || name.equals("invalid") || name.equals("erased")) {
                    value = true;
                } else if (name.equals("alive") || name.equals("active") || name.equals("present")
                        || name.equals("existing") || name.equals("exists")) {
                    value = false;
                }
                if (value != null) changed |= LoliUnsafeBridge.writeFieldValue(owner, field, value);
            }
        }
        return changed;
    }

    private static boolean invokeTerminalMethods(Object owner) {
        boolean changed = false;
        Set<String> names = Set.of("remove", "discard", "kill", "destroy", "terminate",
                "erase", "invalidate", "shutdown", "close", "despawn");
        for (Class<?> type = owner.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0
                        || !names.contains(normalize(method.getName()))) continue;
                try {
                    method.setAccessible(true);
                    method.invoke(owner);
                    changed = true;
                } catch (Throwable ignored) {
                }
            }
        }
        return changed;
    }

    private static boolean removeFromReferencedStatics(Object owner) {
        boolean changed = false;
        Set<FieldRef> refs = referencedStaticFields(owner.getClass());
        for (Class<?> type = owner.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) continue;
                Class<?> fieldType = field.getType();
                if (fieldType.isPrimitive()) continue;
                refs.add(new FieldRef(type.getName(), field.getName()));
            }
        }
        int checked = 0;
        for (FieldRef ref : refs) {
            if (checked++ >= MAX_STATIC_REFS) break;
            try {
                Class<?> holder = Class.forName(ref.owner, false, owner.getClass().getClassLoader());
                if (!sameModCode(owner.getClass(), holder)) continue;
                Field field = findField(holder, ref.name);
                if (field == null || !Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                Object container = readStatic(field);
                if (container instanceof Collection<?> collection && containsIdentity(collection, owner)) {
                    changed |= forceRemoveCollection(collection, owner);
                } else if (container instanceof Map<?, ?> map && containsIdentity(map, owner)) {
                    changed |= forceRemoveMap(map, owner);
                } else if (container != null && container.getClass().isArray()) {
                    changed |= forceRemoveArray(container, owner);
                }
            } catch (Throwable ignored) {
            }
        }
        return changed;
    }

    private static Set<FieldRef> referencedStaticFields(Class<?> type) {
        Set<FieldRef> refs = new HashSet<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            String resource = "/" + current.getName().replace('.', '/') + ".class";
            try (InputStream input = current.getResourceAsStream(resource)) {
                if (input == null) continue;
                ClassNode node = new ClassNode();
                new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                for (MethodNode method : node.methods) {
                    for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                        if (!(insn instanceof FieldInsnNode field) || field.getOpcode() != Opcodes.GETSTATIC) continue;
                        Type fieldType = Type.getType(field.desc);
                        if (fieldType.getSort() != Type.OBJECT && fieldType.getSort() != Type.ARRAY) continue;
                        refs.add(new FieldRef(field.owner.replace('/', '.'), field.name));
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return refs;
    }

    private static boolean containsIdentity(Collection<?> collection, Object target) {
        try {
            for (Object value : collection) if (value == target) return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean containsIdentity(Map<?, ?> map, Object target) {
        try {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == target || entry.getValue() == target) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean forceRemoveCollection(Collection<?> collection, Object target) {
        try {
            if (((Collection) collection).remove(target) && !containsIdentity(collection, target)) return true;
        } catch (Throwable ignored) {
        }
        if (collection instanceof java.util.ArrayList<?>) return forceRemoveArrayList(collection, target);
        for (Class<?> type = collection.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                Object nested = LoliUnsafeBridge.readFieldValue(collection, field);
                if (nested instanceof Collection<?> nestedCollection && nestedCollection != collection
                        && containsIdentity(nestedCollection, target)) {
                    if (forceRemoveCollection(nestedCollection, target)) return true;
                }
            }
        }
        return false;
    }

    private static boolean forceRemoveArrayList(Object list, Object target) {
        if (UNSAFE == null) return false;
        try {
            Field elementDataField = ArrayList.class.getDeclaredField("elementData");
            Field sizeField = ArrayList.class.getDeclaredField("size");
            Object[] data = (Object[]) UNSAFE.getObject(list, UNSAFE.objectFieldOffset(elementDataField));
            int size = UNSAFE.getInt(list, UNSAFE.objectFieldOffset(sizeField));
            if (data == null || size < 0 || size > data.length) return false;
            int index = -1;
            for (int i = 0; i < size; i++) {
                if (data[i] == target) { index = i; break; }
            }
            if (index < 0) return false;
            int moved = size - index - 1;
            if (moved > 0) System.arraycopy(data, index + 1, data, index, moved);
            data[size - 1] = null;
            UNSAFE.putIntVolatile(list, UNSAFE.objectFieldOffset(sizeField), size - 1);
            try {
                Field modCountField = java.util.AbstractList.class.getDeclaredField("modCount");
                long offset = UNSAFE.objectFieldOffset(modCountField);
                UNSAFE.putIntVolatile(list, offset, UNSAFE.getInt(list, offset) + 1);
            } catch (Throwable ignored) {
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean forceRemoveMap(Map<?, ?> map, Object target) {
        boolean changed = false;
        try {
            List<Object> keys = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == target || entry.getValue() == target) keys.add(entry.getKey());
            }
            for (Object key : keys) {
                ((Map) map).remove(key);
                changed = true;
            }
        } catch (Throwable ignored) {
        }
         
         
        return changed && !containsIdentity(map, target);
    }

    private static boolean forceRemoveArray(Object array, Object target) {
        if (array == null || !array.getClass().isArray() || array.getClass().getComponentType().isPrimitive()) return false;
        boolean changed = false;
        for (int i = 0; i < Array.getLength(array); i++) {
            if (Array.get(array, i) == target) {
                Array.set(array, i, null);
                changed = true;
            }
        }
        return changed;
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            try { return current.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    private static Object readStatic(Field field) {
        if (field == null || !Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) return null;
        try {
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean sameModCode(Class<?> a, Class<?> b) {
        if (a == null || b == null || a.getClassLoader() != b.getClassLoader()) return false;
        String name = b.getName();
        if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.")
                || name.startsWith("sun.") || name.startsWith("net.minecraft.")
                || name.startsWith("net.minecraftforge.") || name.startsWith("com.anotherstar.lolipickaxe.")) return false;
        try {
            CodeSource sa = a.getProtectionDomain().getCodeSource();
            CodeSource sb = b.getProtectionDomain().getCodeSource();
            return sa != null && sb != null && sa.getLocation().equals(sb.getLocation());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("_", "").replace("$", "");
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

    private record Node(Object value, int depth) {}
    private record FieldRef(String owner, String name) {}

    private LoliDetachedOwnerGraph() {}
}
