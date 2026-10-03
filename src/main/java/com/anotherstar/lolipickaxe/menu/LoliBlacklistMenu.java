package com.anotherstar.lolipickaxe.menu;

import com.anotherstar.lolipickaxe.registry.ModItems;
import com.anotherstar.lolipickaxe.registry.ModMenus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

 
public final class LoliBlacklistMenu extends AbstractContainerMenu {
    private static final int BLACKLIST_SLOTS = 81;
    private final Inventory inventory;
    private final InteractionHand hand;
    private final ItemStack owner;
    private final ItemStackHandler entries = new ItemStackHandler(BLACKLIST_SLOTS) {
        @Override public int getSlotLimit(int slot) { return 1; }
    };

    public static LoliBlacklistMenu fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        return new LoliBlacklistMenu(containerId, inventory, buf.readEnum(InteractionHand.class));
    }

    public LoliBlacklistMenu(int containerId, Inventory inventory, InteractionHand hand) {
        super(ModMenus.LOLI_BLACKLIST.get(), containerId);
        this.inventory = inventory;
        this.hand = hand;
        this.owner = inventory.player.getItemInHand(hand);
        load();
        addBlacklistSlots();
        addPlayerSlots();
    }

    private void addBlacklistSlots() {
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new SlotItemHandler(entries, column + row * 9, 8 + column * 18, 8 + row * 18) {
                    @Override public boolean mayPlace(ItemStack stack) { return false; }
                    @Override public boolean mayPickup(Player player) { return false; }
                });
            }
        }
    }

    private void addPlayerSlots() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 174 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 232));
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < BLACKLIST_SLOTS) {
            if (clickType == ClickType.PICKUP) {
                ItemStack copy = getCarried().copy();
                if (!copy.isEmpty()) copy.setCount(1);
                entries.setStackInSlot(slotId, copy);
                broadcastChanges();
            }
            return;
        }
        if (hand == InteractionHand.MAIN_HAND && slotId == 108 + inventory.selected) return;
        super.clicked(slotId, button, clickType, player);
    }

    private void load() {
        CompoundTag tag = owner.getTag();
        if (tag == null || !tag.contains("Blacklist", Tag.TAG_LIST)) return;
        ListTag list = tag.getList("Blacklist", Tag.TAG_COMPOUND);
        if (list.size() > BLACKLIST_SLOTS) return;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.contains("Slot") || !entry.contains("Name") || !entry.contains("Damage")) continue;
            int slot = entry.getInt("Slot");
            if (slot < 0 || slot >= BLACKLIST_SLOTS) continue;
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("Name"));
            if (id == null) continue;
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item == null || item == net.minecraft.world.item.Items.AIR) continue;
            ItemStack display = new ItemStack(item);
            if (display.isDamageableItem()) display.setDamageValue(Math.max(0, entry.getInt("Damage")));
            entries.setStackInSlot(slot, display);
        }
    }

    private void save() {
        ListTag list = new ListTag();
        for (int slot = 0; slot < BLACKLIST_SLOTS; slot++) {
            ItemStack stack = entries.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", slot);
            entry.putString("Name", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            entry.putInt("Damage", stack.getDamageValue());
            list.add(entry);
        }
        owner.getOrCreateTag().put("Blacklist", list);
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack held = player.getItemInHand(hand);
        return held.is(ModItems.LOLI_PICKAXE.get()) || held.is(ModItems.SMALL_LOLI_PICKAXE.get());
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public void removed(Player player) {
        if (!player.level().isClientSide) save();
        super.removed(player);
    }
}
