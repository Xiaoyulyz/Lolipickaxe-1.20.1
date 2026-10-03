package com.anotherstar.lolipickaxe.inventory;

import com.anotherstar.lolipickaxe.config.LoliConfig;
import com.anotherstar.lolipickaxe.item.SmallLoliPickaxeItem;
import com.anotherstar.lolipickaxe.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

 
public final class PagedPickaxeStorage {
    public static final int SLOTS_PER_PAGE = 81;
    private static final String ROOT = "Pages";
    private static final String PAGE_LIST = "PageList";
    private static final String CURRENT_PAGE = "CurPage";

    public static int maxPages(ItemStack owner) {
        if (owner.is(ModItems.SMALL_LOLI_PICKAXE.get())) {
            CompoundTag tag = owner.getTag();
            if (tag == null || !tag.contains("LoliBackpackPage")) return 0;
            return SmallLoliPickaxeItem.getTransformValue("LoliBackpackPage", tag.getInt("LoliBackpackPage"));
        }
        return LoliConfig.STORAGE_PAGES.get();
    }

    public static int getCurrentPage(ItemStack owner) {
        return clampPage(owner, owner.getOrCreateTagElement(ROOT).getInt(CURRENT_PAGE));
    }

    public static void setCurrentPage(ItemStack owner, int page) {
        owner.getOrCreateTagElement(ROOT).putInt(CURRENT_PAGE, clampPage(owner, page));
    }

    public static void loadPage(ItemStack owner, int page, ItemStackHandler target) {
        target.deserializeNBT(getPageTag(owner, clampPage(owner, page)));
    }

    public static void savePage(ItemStack owner, int page, ItemStackHandler source) {
        CompoundTag root = owner.getOrCreateTagElement(ROOT);
        ListTag pages = root.getList(PAGE_LIST, Tag.TAG_COMPOUND);
        int clamped = clampPage(owner, page);
        while (pages.size() <= clamped) pages.add(new CompoundTag());
        pages.set(clamped, source.serializeNBT());
        root.put(PAGE_LIST, pages);
        root.putInt(CURRENT_PAGE, clamped);
    }

    private static CompoundTag getPageTag(ItemStack owner, int page) {
        CompoundTag root = owner.getOrCreateTagElement(ROOT);
        ListTag pages = root.getList(PAGE_LIST, Tag.TAG_COMPOUND);
        if (page >= 0 && page < pages.size()) return pages.getCompound(page);
        return new CompoundTag();
    }

    public static int clampPage(ItemStack owner, int page) {
        int max = Math.max(1, maxPages(owner));
        return Math.max(0, Math.min(page, max - 1));
    }


     
    public static ItemStack insert(ItemStack owner, ItemStack incoming) {
        ItemStack remainder = incoming.copy();
        int pages = maxPages(owner);
        for (int page = 0; page < pages && !remainder.isEmpty(); page++) {
            LegacyPageItemHandler handler = new LegacyPageItemHandler(owner);
            loadPage(owner, page, handler);
            for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
                remainder = handler.insertItem(slot, remainder, false);
            }
            savePage(owner, page, handler);
        }
        return remainder;
    }

    public static void dropAll(ServerPlayer player, ItemStack owner) {
        CompoundTag root = owner.getOrCreateTagElement(ROOT);
        ListTag pages = root.getList(PAGE_LIST, Tag.TAG_COMPOUND);
        for (int p = 0; p < pages.size(); p++) {
            LegacyPageItemHandler handler = new LegacyPageItemHandler(owner);
            handler.deserializeNBT(pages.getCompound(p));
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.extractItem(i, Integer.MAX_VALUE, false);
                if (!stack.isEmpty()) player.drop(stack, false, true);
            }
        }
        root.remove(PAGE_LIST);
        root.putInt(CURRENT_PAGE, 0);
    }

    private PagedPickaxeStorage() {}
}
