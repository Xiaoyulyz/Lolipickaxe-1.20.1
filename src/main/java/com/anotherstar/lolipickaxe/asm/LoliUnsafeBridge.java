package com.anotherstar.lolipickaxe.asm;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;


public final class LoliUnsafeBridge {
    private static final Unsafe UNSAFE = findUnsafe();
    private static final MethodHandles.Lookup TRUSTED_LOOKUP = findTrustedLookup();

    private static volatile EntityDataAccessor<Float> healthAccessor;
    private static volatile long entityDataOffset = Long.MIN_VALUE;
    private static volatile long trackedOwnerOffset = Long.MIN_VALUE;
    private static volatile long trackedItemsOffset = Long.MIN_VALUE;
    private static volatile long trackedDirtyOffset = Long.MIN_VALUE;
    private static volatile Class<?> dataItemClass;
    private static volatile long dataItemValueOffset = Long.MIN_VALUE;
    private static volatile long dataItemDirtyOffset = Long.MIN_VALUE;
    private static volatile long removalReasonOffset = Long.MIN_VALUE;
    private static volatile long addedToWorldOffset = Long.MIN_VALUE;
    private static volatile long attributeBaseValueOffset = Long.MIN_VALUE;

    private static volatile MethodHandle vanillaSetHealth;
    private static volatile MethodHandle vanillaActuallyHurt;
    private static volatile MethodHandle vanillaDie;
    private static volatile MethodHandle vanillaRemove;
    private static volatile MethodHandle vanillaSetRemoved;
    private static volatile MethodHandle vanillaIsDeadOrDying;
    private static volatile Field vanillaDeadField;

    public static Object filterSynchedValue(SynchedEntityData data, EntityDataAccessor<?> accessor, Object requested) {
        if (!isHealthAccessor(accessor)) return requested;
        LivingEntity owner = livingOwner(data);
        if (owner == null) return requested;
        if (isProtected(owner)) return Float.valueOf(LoliDefenseHooks.PROTECTED_HEALTH);
        if (isErased(owner)) return Float.valueOf(0.0F);
        return requested;
    }

    public static boolean shouldOverrideSynchedValue(SynchedEntityData data, EntityDataAccessor<?> accessor) {
        if (!isHealthAccessor(accessor)) return false;
        LivingEntity owner = livingOwner(data);
        return owner != null && (isProtected(owner) || isErased(owner));
    }

    public static Object forcedSynchedValue(SynchedEntityData data, EntityDataAccessor<?> accessor) {
        LivingEntity owner = livingOwner(data);
        return Float.valueOf(owner != null && isProtected(owner) ? LoliDefenseHooks.PROTECTED_HEALTH : 0.0F);
    }

    public static void forceRawHealth(LivingEntity entity, float health) {
        if (entity == null) return;
        float forced = isProtected(entity) ? LoliDefenseHooks.PROTECTED_HEALTH : (isErased(entity) ? 0.0F : health);
        SynchedEntityData data = rawEntityData(entity);
        EntityDataAccessor<Float> accessor = healthAccessor();
        if (data == null || accessor == null) return;

         
        try {
            data.set(accessor, forced);
        } catch (Throwable ignored) {
        }

         
         
        try {
            Object item = dataItem(data, accessor.getId());
            if (item == null) return;
            long valueOffset = dataItemValueOffset(item.getClass());
            if (valueOffset >= 0L) UNSAFE.putObjectVolatile(item, valueOffset, Float.valueOf(forced));
            long itemDirty = dataItemDirtyOffset(item.getClass());
            if (itemDirty >= 0L) UNSAFE.putBooleanVolatile(item, itemDirty, true);
            long trackerDirty = trackedDirtyOffset();
            if (trackerDirty >= 0L) UNSAFE.putBooleanVolatile(data, trackerDirty, true);
        } catch (Throwable error) {
            LoliPickaxe.LOGGER.debug("Unable to write raw tracked health for {}", entity.getUUID(), error);
        }
    }

     
    public static Object readFieldValue(Object owner, Field field) {
        if (owner == null || field == null || Modifier.isStatic(field.getModifiers())) return null;
        try {
            if (UNSAFE == null) {
                field.setAccessible(true);
                return field.get(owner);
            }
            long offset = UNSAFE.objectFieldOffset(field);
            Class<?> type = field.getType();
            if (type == boolean.class) return UNSAFE.getBooleanVolatile(owner, offset);
            if (type == byte.class) return UNSAFE.getByteVolatile(owner, offset);
            if (type == short.class) return UNSAFE.getShortVolatile(owner, offset);
            if (type == char.class) return UNSAFE.getCharVolatile(owner, offset);
            if (type == int.class) return UNSAFE.getIntVolatile(owner, offset);
            if (type == long.class) return UNSAFE.getLongVolatile(owner, offset);
            if (type == float.class) return UNSAFE.getFloatVolatile(owner, offset);
            if (type == double.class) return UNSAFE.getDoubleVolatile(owner, offset);
            return UNSAFE.getObjectVolatile(owner, offset);
        } catch (Throwable ignored) {
            try {
                field.setAccessible(true);
                return field.get(owner);
            } catch (Throwable ignoredAgain) {
                return null;
            }
        }
    }

     
    public static boolean writeFieldValue(Object owner, Field field, Object value) {
        if (owner == null || field == null || Modifier.isStatic(field.getModifiers())) return false;
        try {
            if (UNSAFE == null) {
                field.setAccessible(true);
                field.set(owner, value);
                return true;
            }
            long offset = UNSAFE.objectFieldOffset(field);
            Class<?> type = field.getType();
            if (type == boolean.class && value instanceof Boolean v) UNSAFE.putBooleanVolatile(owner, offset, v);
            else if (type == byte.class && value instanceof Number v) UNSAFE.putByteVolatile(owner, offset, v.byteValue());
            else if (type == short.class && value instanceof Number v) UNSAFE.putShortVolatile(owner, offset, v.shortValue());
            else if (type == char.class && value instanceof Character v) UNSAFE.putCharVolatile(owner, offset, v);
            else if (type == int.class && value instanceof Number v) UNSAFE.putIntVolatile(owner, offset, v.intValue());
            else if (type == long.class && value instanceof Number v) UNSAFE.putLongVolatile(owner, offset, v.longValue());
            else if (type == float.class && value instanceof Number v) UNSAFE.putFloatVolatile(owner, offset, v.floatValue());
            else if (type == double.class && value instanceof Number v) UNSAFE.putDoubleVolatile(owner, offset, v.doubleValue());
            else UNSAFE.putObjectVolatile(owner, offset, value);
            return true;
        } catch (Throwable ignored) {
            try {
                field.setAccessible(true);
                field.set(owner, value);
                return true;
            } catch (Throwable ignoredAgain) {
                return false;
            }
        }
    }

     
    public static Object numericZero(Class<?> type) {
        if (type == byte.class || type == Byte.class) return (byte) 0;
        if (type == short.class || type == Short.class) return (short) 0;
        if (type == int.class || type == Integer.class) return 0;
        if (type == long.class || type == Long.class) return 0L;
        if (type == float.class || type == Float.class) return 0.0F;
        if (type == double.class || type == Double.class) return 0.0D;
        return null;
    }

     
    public static Number readNumericArrayElement(Object array, int index) {
        if (array == null || !array.getClass().isArray() || index < 0
                || index >= Array.getLength(array)) return null;
        Class<?> component = array.getClass().getComponentType();
        if (numericZero(component) == null) return null;
        try {
            if (UNSAFE == null) {
                Object value = Array.get(array, index);
                return value instanceof Number number ? number : null;
            }
            long offset = (long) UNSAFE.arrayBaseOffset(array.getClass())
                    + (long) index * UNSAFE.arrayIndexScale(array.getClass());
            if (component == byte.class) return UNSAFE.getByteVolatile(array, offset);
            if (component == short.class) return UNSAFE.getShortVolatile(array, offset);
            if (component == int.class) return UNSAFE.getIntVolatile(array, offset);
            if (component == long.class) return UNSAFE.getLongVolatile(array, offset);
            if (component == float.class) return UNSAFE.getFloatVolatile(array, offset);
            if (component == double.class) return UNSAFE.getDoubleVolatile(array, offset);
        } catch (Throwable ignored) {
            try {
                Object value = Array.get(array, index);
                return value instanceof Number number ? number : null;
            } catch (Throwable ignoredAgain) {
            }
        }
        return null;
    }

     
    public static boolean writeNumericArrayElement(Object array, int index, Number value) {
        if (array == null || value == null || !array.getClass().isArray() || index < 0
                || index >= Array.getLength(array)) return false;
        Class<?> component = array.getClass().getComponentType();
        if (numericZero(component) == null) return false;
        try {
            if (UNSAFE == null) {
                Array.set(array, index, numericArrayValue(component, value));
                return true;
            }
            long offset = (long) UNSAFE.arrayBaseOffset(array.getClass())
                    + (long) index * UNSAFE.arrayIndexScale(array.getClass());
            if (component == byte.class) UNSAFE.putByteVolatile(array, offset, value.byteValue());
            else if (component == short.class) UNSAFE.putShortVolatile(array, offset, value.shortValue());
            else if (component == int.class) UNSAFE.putIntVolatile(array, offset, value.intValue());
            else if (component == long.class) UNSAFE.putLongVolatile(array, offset, value.longValue());
            else if (component == float.class) UNSAFE.putFloatVolatile(array, offset, value.floatValue());
            else if (component == double.class) UNSAFE.putDoubleVolatile(array, offset, value.doubleValue());
            else return false;
            return true;
        } catch (Throwable ignored) {
            try {
                Array.set(array, index, numericArrayValue(component, value));
                return true;
            } catch (Throwable ignoredAgain) {
                return false;
            }
        }
    }

    private static Object numericArrayValue(Class<?> component, Number value) {
        if (component == byte.class) return value.byteValue();
        if (component == short.class) return value.shortValue();
        if (component == int.class) return value.intValue();
        if (component == long.class) return value.longValue();
        if (component == float.class) return value.floatValue();
        if (component == double.class) return value.doubleValue();
        return value;
    }

    



    public static void forceRemovalState(Entity entity, Entity.RemovalReason reason) {
        if (entity == null || reason == null || UNSAFE == null) return;
        try {
            long offset = removalReasonOffset;
            if (offset == Long.MIN_VALUE) {
                offset = findInstanceFieldOffset(Entity.class,
                        field -> field.getType() == Entity.RemovalReason.class);
                removalReasonOffset = offset;
            }
            if (offset >= 0L) UNSAFE.putObjectVolatile(entity, offset, reason);
        } catch (Throwable ignored) {
        }
        try {
            long offset = addedToWorldOffset;
            if (offset == Long.MIN_VALUE) {
                offset = findInstanceFieldOffset(Entity.class, field -> field.getType() == boolean.class
                        && field.getName().toLowerCase().contains("addedtoworld"));
                addedToWorldOffset = offset;
            }
            if (offset >= 0L) UNSAFE.putBooleanVolatile(entity, offset, false);
        } catch (Throwable ignored) {
        }
    }

    public static void stabilizeProtected(LivingEntity entity) {
        if (entity == null || !isProtected(entity)) return;
        AttributeInstance max = entity.getAttribute(Attributes.MAX_HEALTH);
        if (max != null && Double.compare(max.getBaseValue(), LoliDefenseHooks.PROTECTED_HEALTH) != 0) {
            max.setBaseValue(LoliDefenseHooks.PROTECTED_HEALTH);
        }
        LoliDefenseHooks.maintain(entity);
        forceRawHealth(entity, LoliDefenseHooks.PROTECTED_HEALTH);
         
        entity.deathTime = 0;
        entity.invulnerableTime = Math.max(entity.invulnerableTime, 20);
    }


    public static boolean forceMaxHealthBase(LivingEntity target, double value) {
        if (target == null) return false;
        AttributeInstance attribute;
        try {
            attribute = target.getAttribute(Attributes.MAX_HEALTH);
        } catch (Throwable ignored) {
            return false;
        }
        if (attribute == null) return false;
        boolean changed = false;
        try {
            attribute.setBaseValue(value);
            changed = true;
        } catch (Throwable ignored) {
        }
        if (UNSAFE == null) return changed;
        try {
            long offset = attributeBaseValueOffset;
            if (offset == Long.MIN_VALUE) {
                offset = findInstanceFieldOffset(AttributeInstance.class, field ->
                        field.getType() == double.class
                                && (field.getName().toLowerCase().contains("base")
                                || field.getName().toLowerCase().contains("value")));
                if (offset < 0L) {
                    offset = findInstanceFieldOffset(AttributeInstance.class,
                            field -> field.getType() == double.class);
                }
                attributeBaseValueOffset = offset;
            }
            if (offset >= 0L) {
                UNSAFE.putDoubleVolatile(attribute, offset, value);
                changed = true;
            }
        } catch (Throwable ignored) {
        }
        return changed;
    }


    public static boolean invokeVanillaSetHealth(LivingEntity target, float health) {
        if (target == null) return false;
        MethodHandle handle = vanillaSetHealth;
        if (handle == null) {
            handle = findSpecial(LivingEntity.class, new String[]{"setHealth", "m_21153_"},
                    void.class, float.class);
            vanillaSetHealth = handle;
        }
        if (handle == null) return false;
        try {
            handle.invokeWithArguments(target, health);
            return true;
        } catch (Throwable error) {
            LoliPickaxe.LOGGER.debug("Direct LivingEntity#setHealth invocation failed for {}",
                    target.getUUID(), error);
            return false;
        }
    }

     
    public static boolean invokeVanillaActuallyHurt(LivingEntity target, DamageSource source, float amount) {
        if (target == null || source == null) return false;
        MethodHandle handle = vanillaActuallyHurt;
        if (handle == null) {
            handle = findSpecial(LivingEntity.class, new String[]{"actuallyHurt", "m_6475_"},
                    void.class, DamageSource.class, float.class);
            vanillaActuallyHurt = handle;
        }
        if (handle == null) return false;
        try {
            handle.invokeWithArguments(target, source, amount);
            return true;
        } catch (Throwable error) {
            LoliPickaxe.LOGGER.debug("Direct LivingEntity#actuallyHurt invocation failed for {}",
                    target.getUUID(), error);
            return false;
        }
    }

    public static boolean invokeVanillaDie(LivingEntity target, DamageSource source) {
        if (target == null || source == null) return false;
        MethodHandle handle = vanillaDie;
        if (handle == null) {
            handle = findSpecial(LivingEntity.class, new String[]{"die", "m_6667_"},
                    void.class, DamageSource.class);
            vanillaDie = handle;
        }
        if (handle == null) return false;
        try {
            handle.invokeWithArguments(target, source);
            return true;
        } catch (Throwable error) {
            LoliPickaxe.LOGGER.debug("Direct LivingEntity#die invocation failed for {}",
                    target.getUUID(), error);
            return false;
        }
    }

     
    public static float readRawHealth(LivingEntity entity) {
        if (entity == null) return Float.NaN;
        SynchedEntityData data = rawEntityData(entity);
        EntityDataAccessor<Float> accessor = healthAccessor();
        if (data == null || accessor == null) return Float.NaN;
        try {
            Object item = dataItem(data, accessor.getId());
            if (item != null) {
                long valueOffset = dataItemValueOffset(item.getClass());
                if (valueOffset >= 0L) {
                    Object value = UNSAFE.getObjectVolatile(item, valueOffset);
                    if (value instanceof Number number) return number.floatValue();
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Float value = data.get(accessor);
            return value == null ? Float.NaN : value;
        } catch (Throwable ignored) {
            return Float.NaN;
        }
    }

    private static Field resolveVanillaDeadField() {
        Field field = vanillaDeadField;
        if (field != null) return field;
        for (Class<?> type = LivingEntity.class; type != null && type != Object.class;
             type = type.getSuperclass()) {
            for (Field candidate : type.getDeclaredFields()) {
                if (candidate.getType() != boolean.class || Modifier.isStatic(candidate.getModifiers())) continue;
                String name = candidate.getName();
                if (!name.equals("dead") && !name.equals("f_20890_")
                        && !name.toLowerCase().contains("dead")) continue;
                try {
                    candidate.setAccessible(true);
                    vanillaDeadField = candidate;
                    return candidate;
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    



    public static boolean isVanillaDeathCommitted(LivingEntity target) {
        if (target == null) return true;
        try {
            if (target.isRemoved() || target.getRemovalReason() != null || target.deathTime > 0) return true;
        } catch (Throwable ignored) {
        }

         
         
         
        Field field = resolveVanillaDeadField();
        if (field != null) {
            try {
                return field.getBoolean(target);
            } catch (Throwable ignored) {
            }
        }
        return false;
    }


    public static boolean invokeVanillaRemoval(Entity target, Entity.RemovalReason reason) {
        if (target == null || reason == null) return false;
        MethodHandle remove = vanillaRemove;
        if (remove == null) {
            remove = findSpecial(Entity.class, new String[]{"remove", "m_142687_"},
                    void.class, Entity.RemovalReason.class);
            vanillaRemove = remove;
        }
        MethodHandle setRemoved = vanillaSetRemoved;
        if (setRemoved == null) {
            setRemoved = findSpecial(Entity.class, new String[]{"setRemoved", "m_142467_"},
                    void.class, Entity.RemovalReason.class);
            vanillaSetRemoved = setRemoved;
        }
        try {
             
             
            if (setRemoved != null) setRemoved.invokeWithArguments(target, reason);
            if (target.getRemovalReason() == null && remove != null) {
                remove.invokeWithArguments(target, reason);
            }
            return target.getRemovalReason() != null || target.isRemoved();
        } catch (Throwable error) {
            LoliPickaxe.LOGGER.debug("Direct Entity removal invocation failed for {}",
                    target.getUUID(), error);
            return false;
        }
    }

    private static boolean isProtected(LivingEntity entity) {
        try {
            return LoliPickaxeItem.hasLoliProtection(entity);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isErased(LivingEntity entity) {
        try {
            return LoliErasureState.isErased(entity);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Float> healthAccessor() {
        EntityDataAccessor<Float> cached = healthAccessor;
        if (cached != null) return cached;
        synchronized (LoliUnsafeBridge.class) {
            if (healthAccessor != null) return healthAccessor;
            for (Field field : LivingEntity.class.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())
                        || !EntityDataAccessor.class.isAssignableFrom(field.getType())) continue;
                try {
                    Object value = getStatic(field);
                    if (value instanceof EntityDataAccessor<?> accessor
                            && accessor.getSerializer() == EntityDataSerializers.FLOAT) {
                        healthAccessor = (EntityDataAccessor<Float>) accessor;
                        return healthAccessor;
                    }
                } catch (Throwable ignored) {
                }
            }
            return null;
        }
    }

    private static boolean isHealthAccessor(EntityDataAccessor<?> accessor) {
        if (accessor == null) return false;
        EntityDataAccessor<Float> health = healthAccessor();
        if (health == null) return false;
        if (accessor == health) return true;
        try {
            return accessor.getId() == health.getId()
                    && accessor.getSerializer() == EntityDataSerializers.FLOAT;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static LivingEntity livingOwner(SynchedEntityData data) {
        if (data == null || UNSAFE == null) return null;
        long offset = trackedOwnerOffset;
        if (offset == Long.MIN_VALUE) {
            offset = findInstanceFieldOffset(SynchedEntityData.class,
                    field -> Entity.class.isAssignableFrom(field.getType()));
            trackedOwnerOffset = offset;
        }
        if (offset < 0L) return null;
        try {
            Object owner = UNSAFE.getObject(data, offset);
            return owner instanceof LivingEntity living ? living : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static SynchedEntityData rawEntityData(Entity entity) {
        if (entity == null || UNSAFE == null) return null;
        long offset = entityDataOffset;
        if (offset == Long.MIN_VALUE) {
            offset = findInstanceFieldOffset(Entity.class,
                    field -> SynchedEntityData.class.isAssignableFrom(field.getType()));
            entityDataOffset = offset;
        }
        if (offset >= 0L) {
            try {
                Object value = UNSAFE.getObject(entity, offset);
                if (value instanceof SynchedEntityData data) return data;
            } catch (Throwable ignored) {
            }
        }
        try {
            return entity.getEntityData();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object dataItem(SynchedEntityData data, int id) throws ReflectiveOperationException {
        long offset = trackedItemsOffset;
        if (offset == Long.MIN_VALUE) {
            offset = findInstanceFieldOffset(SynchedEntityData.class, field -> {
                String type = field.getType().getName();
                return type.contains("Int2ObjectMap") || field.getType().isArray();
            });
            trackedItemsOffset = offset;
        }
        if (offset < 0L) return null;
        Object items = UNSAFE.getObject(data, offset);
        if (items == null) return null;
        if (items.getClass().isArray()) {
            return id >= 0 && id < Array.getLength(items) ? Array.get(items, id) : null;
        }
        if (items instanceof Map<?, ?> map) return map.get(id);
        Method get = items.getClass().getMethod("get", int.class);
        return get.invoke(items, id);
    }

    private static long dataItemValueOffset(Class<?> itemClass) {
        if (dataItemClass == itemClass && dataItemValueOffset != Long.MIN_VALUE) return dataItemValueOffset;
        synchronized (LoliUnsafeBridge.class) {
            if (dataItemClass == itemClass && dataItemValueOffset != Long.MIN_VALUE) return dataItemValueOffset;
            long value = findInstanceFieldOffset(itemClass, field -> !Modifier.isFinal(field.getModifiers())
                    && !field.getType().isPrimitive()
                    && !EntityDataAccessor.class.isAssignableFrom(field.getType()));
            dataItemClass = itemClass;
            dataItemValueOffset = value;
            return value;
        }
    }

    private static long dataItemDirtyOffset(Class<?> itemClass) {
        if (dataItemClass == itemClass && dataItemDirtyOffset != Long.MIN_VALUE) return dataItemDirtyOffset;
        synchronized (LoliUnsafeBridge.class) {
            long dirty = findInstanceFieldOffset(itemClass, field -> field.getType() == boolean.class);
            dataItemClass = itemClass;
            dataItemDirtyOffset = dirty;
            return dirty;
        }
    }

    private static long trackedDirtyOffset() {
        long cached = trackedDirtyOffset;
        if (cached != Long.MIN_VALUE) return cached;
        long found = findInstanceFieldOffset(SynchedEntityData.class, field -> {
            if (field.getType() != boolean.class) return false;
            String name = field.getName();
            return name.equals("isDirty") || name.equals("dirty") || name.equals("f_135348_")
                    || name.equals("i") || name.equals("g");
        });
        trackedDirtyOffset = found;
        return found;
    }

    private static MethodHandle findSpecial(Class<?> owner, String[] names, Class<?> returnType,
                                            Class<?>... parameters) {
        if (TRUSTED_LOOKUP == null) return null;
        for (Method method : owner.getDeclaredMethods()) {
            if (method.getReturnType() != returnType) continue;
            if (!java.util.Arrays.equals(method.getParameterTypes(), parameters)) continue;
            boolean named = false;
            for (String name : names) {
                if (method.getName().equals(name)) {
                    named = true;
                    break;
                }
            }
            if (!named) continue;
            try {
                return TRUSTED_LOOKUP.unreflectSpecial(method, owner);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

     
    public static MethodHandle findStaticHandle(Class<?> owner, String name, String descriptor) {
        if (owner == null || name == null || descriptor == null || TRUSTED_LOOKUP == null) return null;
        try {
            MethodType type = MethodType.fromMethodDescriptorString(descriptor, owner.getClassLoader());
            return TRUSTED_LOOKUP.findStatic(owner, name, type);
        } catch (Throwable ignored) {
            return null;
        }
    }

     
    public static Object invokeStaticHandle(MethodHandle handle, Object... arguments) {
        if (handle == null) return null;
        try {
            return handle.invokeWithArguments(arguments == null ? java.util.List.of()
                    : java.util.Arrays.asList(arguments));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static long findInstanceFieldOffset(Class<?> owner, java.util.function.Predicate<Field> predicate) {
        if (UNSAFE == null) return -1L;
        for (Field field : owner.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || !predicate.test(field)) continue;
            try {
                return UNSAFE.objectFieldOffset(field);
            } catch (Throwable ignored) {
            }
        }
        return -1L;
    }

    private static Object getStatic(Field field) {
        if (UNSAFE == null) return null;
        return UNSAFE.getObject(UNSAFE.staticFieldBase(field), UNSAFE.staticFieldOffset(field));
    }

    private static Unsafe findUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Throwable error) {
            return null;
        }
    }

    private static MethodHandles.Lookup findTrustedLookup() {
        if (UNSAFE == null) return null;
        try {
            Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            return (MethodHandles.Lookup) UNSAFE.getObject(
                    UNSAFE.staticFieldBase(field), UNSAFE.staticFieldOffset(field));
        } catch (Throwable error) {
            return null;
        }
    }

    private LoliUnsafeBridge() {
    }
}
