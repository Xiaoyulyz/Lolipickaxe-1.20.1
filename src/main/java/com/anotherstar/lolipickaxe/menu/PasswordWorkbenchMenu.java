package com.anotherstar.lolipickaxe.menu;

import com.anotherstar.lolipickaxe.registry.ModBlocks;
import com.anotherstar.lolipickaxe.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

 
public final class PasswordWorkbenchMenu extends AbstractContainerMenu {
    public final TransientCraftingContainer craftMatrix = new TransientCraftingContainer(this, 3, 3);
    public final ResultContainer craftResult = new ResultContainer();
    private final ContainerLevelAccess access;
    private String password = "";

    public static PasswordWorkbenchMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        return new PasswordWorkbenchMenu(id, inventory, inventory.player.level(), pos);
    }

    public PasswordWorkbenchMenu(int id, Inventory inventory, Level level, BlockPos pos) {
        super(ModMenus.PASSWORD_WORKBENCH.get(), id);
        this.access = ContainerLevelAccess.create(level, pos);

        addSlot(new ResultSlot(inventory.player, craftMatrix, craftResult, 0, 124, 65));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(craftMatrix, col + row * 3, 30 + col * 18, 47 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 114 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 172));
        }
    }

    public void setPassword(String password) {
        this.password = password == null ? "" : password;
        slotsChanged(craftMatrix);
    }

    public String getPassword() {
        return password;
    }

    @Override
    public void slotsChanged(Container container) {
         
         
        craftResult.setItem(0, ItemStack.EMPTY);
        broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, craftMatrix));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.PASSWORD_WORK_BENCH.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;
        ItemStack source = slot.getItem();
        result = source.copy();
        if (index == 0) {
            if (!moveItemStackTo(source, 10, 46, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(source, result);
        } else if (index >= 10 && index < 37) {
            if (!moveItemStackTo(source, 37, 46, false)) return ItemStack.EMPTY;
        } else if (index >= 37 && index < 46) {
            if (!moveItemStackTo(source, 10, 37, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(source, 10, 46, false)) {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (source.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, source);
        return result;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != craftResult && super.canTakeItemForPickAll(stack, slot);
    }
}
