package com.anotherstar.lolipickaxe.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

 
public enum LoliTier implements Tier {
    INSTANCE;
    @Override public int getUses() { return 0; }
    @Override public float getSpeed() { return 0.0F; }
    @Override public float getAttackDamageBonus() { return 0.0F; }
    @Override public int getLevel() { return 32; }
    @Override public int getEnchantmentValue() { return 0; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
}
