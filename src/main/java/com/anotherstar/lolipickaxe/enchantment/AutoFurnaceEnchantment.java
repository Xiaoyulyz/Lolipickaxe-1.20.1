package com.anotherstar.lolipickaxe.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public final class AutoFurnaceEnchantment extends Enchantment {
    public AutoFurnaceEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentCategory.DIGGER, new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
    }
    @Override public int getMinCost(int level) { return 15; }
    @Override public int getMaxCost(int level) { return super.getMinCost(level) + 50; }
    @Override public int getMaxLevel() { return 1; }
}
