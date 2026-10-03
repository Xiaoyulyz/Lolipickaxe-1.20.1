package com.anotherstar.lolipickaxe.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class AddonItem extends Item {
    public static final String LEVEL_TAG = "LoliMaterialLevel";
    private final UpgradeType type;

    public AddonItem(UpgradeType type) {
        super(new Item.Properties());
        this.type = type;
    }

    public UpgradeType type() { return type; }

    public int level(ItemStack stack) {
        return Math.max(0, Math.min(stack.getOrCreateTag().getInt(LEVEL_TAG), type.maxLevel()));
    }

    public ItemStack withLevel(int level, int count) {
        ItemStack stack = new ItemStack(this, count);
        stack.getOrCreateTag().putInt(LEVEL_TAG, Math.max(0, Math.min(level, type.maxLevel())));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (type.maxLevel() == 0) return Component.translatable(type.nameKey());
        int level = level(stack);
        Component tier = level >= type.maxLevel()
                ? Component.translatable("item.loliMaterial.end")
                : Component.translatable("item.loliMaterial." + level);
        return Component.translatable("item.loliMaterialFormat", Component.translatable(type.nameKey()), tier);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int current = level(stack);
        if (current < type.maxLevel()) {
            Component from = Component.translatable("item.loliMaterial." + current);
            Component to = current + 1 >= type.maxLevel()
                    ? Component.translatable("item.loliMaterial.end")
                    : Component.translatable("item.loliMaterial." + (current + 1));
            Component end = Component.translatable("item.loliMaterial." + type.maxLevel());
            tooltip.add(Component.translatable("item.loliMaterial.recipe", from, to, end));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
