package com.anotherstar.lolipickaxe.asm;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class LoliKlassSwap {
    private static final Unsafe U;
    private static final long KLASS_OFFSET;
    private static final boolean NARROW_KLASS;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            U = (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
        KLASS_OFFSET = U.addressSize();
        NARROW_KLASS = U.addressSize() == 4 || U.arrayBaseOffset(Object[].class) == 16;
    }

    private LoliKlassSwap() {
    }

    private static boolean hasNoInstanceFields(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) return false;
        }
        return true;
    }

    public static boolean swap(Object target, Class<?> to) {
        if (target == null || to == null) return false;
        if (target.getClass() == to) return true;
        Class<?> current = target.getClass();
        boolean down = to.getSuperclass() == current && hasNoInstanceFields(to);
        boolean up = current.getSuperclass() == to && hasNoInstanceFields(current);
        if (!down && !up) return false;
        synchronized (LoliKlassSwap.class) {
            try {
                U.ensureClassInitialized(to);
                Object donor = U.allocateInstance(to);
                if (NARROW_KLASS) {
                    U.putIntVolatile(target, KLASS_OFFSET, U.getIntVolatile(donor, KLASS_OFFSET));
                } else {
                    U.putLongVolatile(target, KLASS_OFFSET, U.getLongVolatile(donor, KLASS_OFFSET));
                }
                U.fullFence();
            } catch (Throwable throwable) {
                LoliPickaxe.LOGGER.error("Loli klass defence swap failed", throwable);
                return false;
            }
        }
        return target.getClass() == to;
    }
}
