package com.anotherstar.lolipickaxe.recipe;

import com.anotherstar.lolipickaxe.item.AddonItem;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import com.anotherstar.lolipickaxe.item.UpgradeType;
import com.anotherstar.lolipickaxe.registry.ModItems;
import com.anotherstar.lolipickaxe.registry.ModRecipeSerializers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class LoliPickaxeUpgradeRecipe extends CustomRecipe {
    public LoliPickaxeUpgradeRecipe(ResourceLocation id, CraftingBookCategory category) {
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
        ItemStack small = ItemStack.EMPTY;
        ItemStack soul = ItemStack.EMPTY;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(ModItems.SMALL_LOLI_PICKAXE.get()) && small.isEmpty()) small = stack;
            else if (stack.getItem() instanceof AddonItem addon && addon.type() == UpgradeType.ENTITY_SOUL && soul.isEmpty()) soul = stack;
            else return ItemStack.EMPTY;
        }
        if (small.isEmpty() || soul.isEmpty()) return ItemStack.EMPTY;
        AddonItem soulItem = (AddonItem) soul.getItem();
        if (soulItem.level(soul) != UpgradeType.ENTITY_SOUL.maxLevel()) return ItemStack.EMPTY;
        CompoundTag tag = small.getTag();
        if (tag == null) return ItemStack.EMPTY;
        for (UpgradeType type : UpgradeType.values()) {
            if (type == UpgradeType.ENTITY_SOUL) continue;
            if (!tag.contains(type.nbtKey()) || tag.getInt(type.nbtKey()) != type.maxLevel()) return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(ModItems.LOLI_PICKAXE.get());
        LoliPickaxeItem.applyLegacyDefaults(result);
        CompoundTag resultTag = result.getOrCreateTag();
        if (tag.contains("Pages")) resultTag.put("Pages", tag.getCompound("Pages").copy());
        if (tag.contains("Blacklist", Tag.TAG_LIST)) {
            resultTag.put("Blacklist", tag.getList("Blacklist", Tag.TAG_COMPOUND).copy());
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.LOLI_PICKAXE_UPGRADE.get();
    }
}
