package com.anotherstar.lolipickaxe.config;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class LegacyLoliSettings {
    public static final String ROOT = "LoliConfig";

    public static boolean bool(ItemStack stack, String key, boolean globalValue) {
        CompoundTag config = itemConfig(stack);
        return config != null && config.contains(key) ? config.getBoolean(key) : globalValue;
    }

    public static int integer(ItemStack stack, String key, int globalValue) {
        CompoundTag config = itemConfig(stack);
        return config != null && config.contains(key) ? config.getInt(key) : globalValue;
    }

    public static double decimal(ItemStack stack, String key, double globalValue) {
        CompoundTag config = itemConfig(stack);
        return config != null && config.contains(key) ? config.getDouble(key) : globalValue;
    }

    public static String string(ItemStack stack, String key, String globalValue) {
        CompoundTag config = itemConfig(stack);
        return config != null && config.contains(key, Tag.TAG_STRING) ? config.getString(key) : globalValue;
    }

    public static CompoundTag itemConfig(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(ROOT, Tag.TAG_COMPOUND) ? tag.getCompound(ROOT) : null;
    }

    private LegacyLoliSettings() {}
}
