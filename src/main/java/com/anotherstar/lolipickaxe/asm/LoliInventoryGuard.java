package com.anotherstar.lolipickaxe.asm;

import com.anotherstar.lolipickaxe.config.LoliConfig;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import com.anotherstar.lolipickaxe.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public final class LoliInventoryGuard {
    private static final Map<UUID, GuardState> STATES = new HashMap<>();

     
    public static boolean shouldBlockInventoryMutation(Player player) {
        if (player == null || player.level().isClientSide) return false;
        return inventoryHasOwnedPickaxe(player) || hasTemporaryProtection(player);
    }

    public static boolean hasTemporaryProtection(Player player) {
        GuardState state = STATES.get(player.getUUID());
        return state != null && state.remainingTicks > 0;
    }

     
    public static ItemStack rememberedPickaxe(Player player) {
        GuardState state = STATES.get(player.getUUID());
        if (state == null || state.remainingTicks <= 0 || state.pickaxe.isEmpty()) return ItemStack.EMPTY;
        return state.pickaxe.copy();
    }

     
    public static void markIntentionalDrop(Player player) {
        GuardState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new GuardState());
        state.intentionalDrop = true;
        state.restoreSuppressed = false;
        state.intentionalDropTicks = 0;
        state.remainingTicks = Math.max(state.remainingTicks, LoliConfig.EFFECT_DURATION.get());
    }

    public static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        LocatedPickaxe located = locateOwnedPickaxe(player);
        GuardState state = STATES.get(id);

        if (located != null) {
            if (state == null) {
                state = new GuardState();
                STATES.put(id, state);
            }
            state.pickaxe = located.stack.copy();
            state.preferredMainSlot = located.mainSlot;
            state.remainingTicks = LoliConfig.EFFECT_DURATION.get();
            state.intentionalDrop = false;
            state.restoreSuppressed = false;
            state.intentionalDropTicks = 0;
            return;
        }

        if (state == null) return;
        if (state.remainingTicks > 0) state.remainingTicks--;

        if (!state.intentionalDrop && player.getAbilities().instabuild) {
             
             
             
            state.restoreSuppressed = true;
        }

        if (state.intentionalDrop) {
            state.intentionalDropTicks++;
             
             
             
            if (hasOwnedDroppedPickaxe(player, state.pickaxe)) {
                if (state.remainingTicks <= 0) STATES.remove(id);
                return;
            }
            if (state.intentionalDropTicks <= 5) return;
            state.intentionalDrop = false;
        }

        if (state.remainingTicks > 0 && !state.restoreSuppressed && !state.pickaxe.isEmpty()) {
            restorePickaxeOnly(player, state);
        }

        if (state.remainingTicks <= 0) STATES.remove(id);
    }

    public static void forget(Player player) {
        STATES.remove(player.getUUID());
    }

    private static void restorePickaxeOnly(ServerPlayer player, GuardState state) {
        if (inventoryHasOwnedPickaxe(player)) return;

        ItemStack restored = state.pickaxe.copy();
        int preferred = state.preferredMainSlot;
        if (preferred >= 0 && preferred < player.getInventory().items.size()
                && player.getInventory().items.get(preferred).isEmpty()) {
            player.getInventory().items.set(preferred, restored);
            sync(player);
            return;
        }

        for (int i = 0; i < player.getInventory().items.size(); i++) {
            if (player.getInventory().items.get(i).isEmpty()) {
                player.getInventory().items.set(i, restored);
                state.preferredMainSlot = i;
                sync(player);
                return;
            }
        }

        if (player.getInventory().offhand.get(0).isEmpty()) {
            player.getInventory().offhand.set(0, restored);
            sync(player);
            return;
        }

        if (player.containerMenu != null && player.containerMenu.getCarried().isEmpty()) {
            player.containerMenu.setCarried(restored);
            sync(player);
            return;
        }

         
         
         
        ItemEntity entity = player.drop(restored, false, false);
        if (entity != null) {
            entity.setPos(player.getX(), player.getEyeY() - 0.3D, player.getZ());
            entity.setInvulnerable(true);
            entity.setPickUpDelay(0);
        }
    }

    private static void sync(ServerPlayer player) {
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != null && player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }
    }

    private static boolean inventoryHasOwnedPickaxe(Player player) {
        return locateOwnedPickaxe(player) != null;
    }

    private static LocatedPickaxe locateOwnedPickaxe(Player player) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (isOwnedPremium(stack, player)) return new LocatedPickaxe(stack, i);
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (isOwnedPremium(stack, player)) return new LocatedPickaxe(stack, -1);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (isOwnedPremium(stack, player)) return new LocatedPickaxe(stack, -1);
        }
        if (player.containerMenu != null) {
            ItemStack carried = player.containerMenu.getCarried();
            if (isOwnedPremium(carried, player)) return new LocatedPickaxe(carried, -1);
        }
        return null;
    }

    private static boolean hasOwnedDroppedPickaxe(ServerPlayer player, ItemStack remembered) {
        if (!(player.level() instanceof ServerLevel level) || remembered.isEmpty()) return false;
        int range = Math.max(2, LoliConfig.FIND_OWNER_RANGE.get());
        AABB area = player.getBoundingBox().inflate(range);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            ItemStack stack = entity.getItem();
            if (stack.is(ModItems.LOLI_PICKAXE.get())
                    && (!LoliPickaxeItem.hasOwner(stack) || LoliPickaxeItem.isOwner(stack, player))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOwnedPremium(ItemStack stack, Player player) {
        return stack.is(ModItems.LOLI_PICKAXE.get())
                && (!LoliPickaxeItem.hasOwner(stack) || LoliPickaxeItem.isOwner(stack, player));
    }

    private static final class GuardState {
        private ItemStack pickaxe = ItemStack.EMPTY;
        private int preferredMainSlot = -1;
        private int remainingTicks;
        private boolean intentionalDrop;
        private boolean restoreSuppressed;
        private int intentionalDropTicks;
    }

    private record LocatedPickaxe(ItemStack stack, int mainSlot) {}

    private LoliInventoryGuard() {}
}
