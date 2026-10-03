package com.anotherstar.lolipickaxe.menu;

import com.anotherstar.lolipickaxe.inventory.PagedPickaxeStorage;
import com.anotherstar.lolipickaxe.inventory.LegacyPageItemHandler;
import com.anotherstar.lolipickaxe.registry.ModItems;
import com.anotherstar.lolipickaxe.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class LoliPickaxeMenu extends AbstractContainerMenu {
    private final Inventory playerInventory;
    private final InteractionHand hand;
    private final ItemStack ownerStack;
    private final LegacyPageItemHandler pageHandler;
    private int page;

    public static LoliPickaxeMenu fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        return new LoliPickaxeMenu(containerId, inventory, buf.readEnum(InteractionHand.class));
    }

    public LoliPickaxeMenu(int containerId, Inventory inventory, InteractionHand hand) {
        super(ModMenus.LOLI_PICKAXE.get(), containerId);
        this.playerInventory = inventory;
        this.hand = hand;
        this.ownerStack = inventory.player.getItemInHand(hand);
        this.page = PagedPickaxeStorage.getCurrentPage(ownerStack);
        this.pageHandler = new LegacyPageItemHandler(ownerStack) {
            @Override
            protected void onContentsChanged(int slot) {
                if (!ownerStack.isEmpty()) PagedPickaxeStorage.savePage(ownerStack, page, this);
            }
        };
        PagedPickaxeStorage.loadPage(ownerStack, page, pageHandler);
        addDataSlot(new DataSlot() {
            @Override public int get() { return page; }
            @Override public void set(int value) {
                if (value != page) {
                    page = PagedPickaxeStorage.clampPage(ownerStack, value);
                    PagedPickaxeStorage.loadPage(ownerStack, page, pageHandler);
                }
            }
        });
        addStorageSlots();
        addPlayerSlots();
    }

    private void addStorageSlots() {
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new SlotItemHandler(pageHandler, column + row * 9, 8 + column * 18, 8 + row * 18));
            }
        }
    }

    private void addPlayerSlots() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 174 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 232));
        }
    }

    public void changePage(int delta) {
        if (delta == 0) return;
        PagedPickaxeStorage.savePage(ownerStack, page, pageHandler);
        page = PagedPickaxeStorage.clampPage(ownerStack, page + Integer.signum(delta));
        PagedPickaxeStorage.setCurrentPage(ownerStack, page);
        PagedPickaxeStorage.loadPage(ownerStack, page, pageHandler);
        broadcastChanges();
    }

    public int getPage() { return page; }

    @Override
    public boolean stillValid(Player player) {
        ItemStack held = player.getItemInHand(hand);
        return held.is(ModItems.LOLI_PICKAXE.get()) || held.is(ModItems.SMALL_LOLI_PICKAXE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < PagedPickaxeStorage.SLOTS_PER_PAGE) {
            if (!moveItemStackTo(original, PagedPickaxeStorage.SLOTS_PER_PAGE, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, PagedPickaxeStorage.SLOTS_PER_PAGE, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player player) {
        PagedPickaxeStorage.savePage(ownerStack, page, pageHandler);
        super.removed(player);
    }
}
