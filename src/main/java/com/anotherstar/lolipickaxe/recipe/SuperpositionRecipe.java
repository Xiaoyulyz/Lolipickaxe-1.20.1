package com.anotherstar.lolipickaxe.recipe;

import com.anotherstar.lolipickaxe.item.AddonItem;
import com.anotherstar.lolipickaxe.registry.ModRecipeSerializers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class SuperpositionRecipe extends CustomRecipe {
    public SuperpositionRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inventory, Level level) {
        return !assembleInternal(inventory).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, RegistryAccess access) {
        return assembleInternal(inventory);
    }

    private ItemStack assembleInternal(CraftingContainer inventory) {
        AddonItem item = null;
        int materialLevel = -1;
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof AddonItem addon) || addon.type().maxLevel() <= 0) return ItemStack.EMPTY;
            int level = addon.level(stack);
            if (item == null) {
                item = addon;
                materialLevel = level;
            } else if (item != addon || materialLevel != level) {
                return ItemStack.EMPTY;
            }
            count++;
        }
        if (item == null) return ItemStack.EMPTY;
        if (count == 9 && materialLevel < item.type().maxLevel()) return item.withLevel(materialLevel + 1, 1);
        if (count == 1 && materialLevel > 0) return item.withLevel(materialLevel - 1, 9);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return (width == 3 && height == 3) || (width == 1 && height == 1);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.SUPERPOSITION.get();
    }
}
