package com.anotherstar.lolipickaxe.inventory;

import com.anotherstar.lolipickaxe.config.LoliConfig;
import com.anotherstar.lolipickaxe.item.SmallLoliPickaxeItem;
import com.anotherstar.lolipickaxe.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

 
public class LegacyPageItemHandler extends ItemStackHandler {
    private final ItemStack owner;

    public LegacyPageItemHandler(ItemStack owner) {
        super(PagedPickaxeStorage.SLOTS_PER_PAGE);
        this.owner = owner;
    }

    @Override
    public int getSlotLimit(int slot) {
        if (owner.is(ModItems.SMALL_LOLI_PICKAXE.get())) return PagedPickaxeStorage.maxPages(owner) * 32;
        return LoliConfig.SLOT_STACK_LIMIT.get();
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        boolean cancel = owner.is(ModItems.SMALL_LOLI_PICKAXE.get()) || LoliConfig.CANCEL_STACK_LIMIT.get();
        return cancel ? getSlotLimit(slot) : Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag out = new CompoundTag();
        ListTag items = new ListTag();
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (stack.isEmpty()) continue;
            CompoundTag item = new CompoundTag();
            item.putByte("Slot", (byte) slot);
            int count = stack.getCount();
            stack.save(item);
            item.putInt("Count", count);
            items.add(item);
        }
        out.put("Items", items);
        return out;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        stacks = NonNullList.withSize(PagedPickaxeStorage.SLOTS_PER_PAGE, ItemStack.EMPTY);
        ListTag items = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag item = items.getCompound(i);
            int slot = item.getByte("Slot") & 255;
            if (slot < 0 || slot >= stacks.size()) continue;
            int count = item.getInt("Count");
            CompoundTag vanilla = item.copy();
            vanilla.putByte("Count", (byte) 1);
            ItemStack stack = ItemStack.of(vanilla);
            stack.setCount(Math.max(0, count));
            stacks.set(slot, stack);
        }
        onLoad();
    }
}
