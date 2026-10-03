package com.anotherstar.lolipickaxe.recipe;

import com.anotherstar.lolipickaxe.item.AddonItem;
import com.anotherstar.lolipickaxe.item.UpgradeType;
import com.anotherstar.lolipickaxe.item.SmallLoliPickaxeItem;
import com.anotherstar.lolipickaxe.registry.ModItems;
import com.anotherstar.lolipickaxe.registry.ModRecipeSerializers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.EnumMap;
import java.util.Map;

public final class SmallLoliUpgradeRecipe extends CustomRecipe {
    public SmallLoliUpgradeRecipe(ResourceLocation id, CraftingBookCategory category) {
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
        ItemStack pickaxe = ItemStack.EMPTY;
        Map<UpgradeType, Integer> supplied = new EnumMap<>(UpgradeType.class);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(ModItems.SMALL_LOLI_PICKAXE.get()) && pickaxe.isEmpty()) {
                pickaxe = stack.copy();
                pickaxe.setCount(1);
            } else if (stack.getItem() instanceof AddonItem addon && addon.type() != UpgradeType.ENTITY_SOUL) {
                if (supplied.put(addon.type(), addon.level(stack)) != null) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (pickaxe.isEmpty() || supplied.isEmpty()) return ItemStack.EMPTY;
        for (Map.Entry<UpgradeType, Integer> entry : supplied.entrySet()) {
            int current = pickaxe.getOrCreateTag().contains(entry.getKey().nbtKey())
                    ? pickaxe.getOrCreateTag().getInt(entry.getKey().nbtKey()) : -1;
            if (current != entry.getValue() - 1) return ItemStack.EMPTY;
        }
        for (Map.Entry<UpgradeType, Integer> entry : supplied.entrySet()) {
            pickaxe.getOrCreateTag().putInt(entry.getKey().nbtKey(), entry.getValue());
        }
        SmallLoliPickaxeItem.updateEnchantment(pickaxe);
        return pickaxe;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.SMALL_LOLI_UPGRADE.get();
    }
}
