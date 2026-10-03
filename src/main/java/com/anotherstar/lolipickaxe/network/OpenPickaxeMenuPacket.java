package com.anotherstar.lolipickaxe.network;

import com.anotherstar.lolipickaxe.inventory.PagedPickaxeStorage;
import com.anotherstar.lolipickaxe.menu.LoliPickaxeMenu;
import com.anotherstar.lolipickaxe.registry.ModItems;
import com.anotherstar.lolipickaxe.util.LegacyTextComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public record OpenPickaxeMenuPacket(boolean dropAll) {
    public static void encode(OpenPickaxeMenuPacket msg, FriendlyByteBuf buf) { buf.writeBoolean(msg.dropAll); }
    public static OpenPickaxeMenuPacket decode(FriendlyByteBuf buf) { return new OpenPickaxeMenuPacket(buf.readBoolean()); }

    public static void handle(OpenPickaxeMenuPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            InteractionHand hand = findHand(player);
            if (hand == null) return;
            ItemStack stack = player.getItemInHand(hand);
            if (msg.dropAll) {
                PagedPickaxeStorage.dropAll(player, stack);
                player.displayClientMessage(LegacyTextComponents.translatable("message.lolipickaxe.drop_all"), true);
                return;
            }
            InteractionHand selectedHand = hand;
            NetworkHooks.openScreen(player, new SimpleMenuProvider(
                    (id, inventory, ignored) -> new LoliPickaxeMenu(id, inventory, selectedHand),
                    Component.translatable(stack.is(ModItems.SMALL_LOLI_PICKAXE.get()) ? "container.smallLoliPickaxe" : "container.loliPickaxe")),
                    buf -> buf.writeEnum(selectedHand));
        });
        ctx.setPacketHandled(true);
    }

    private static InteractionHand findHand(ServerPlayer player) {
        if (isPickaxe(player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        if (isPickaxe(player.getOffhandItem())) return InteractionHand.OFF_HAND;
        return null;
    }

    private static boolean isPickaxe(ItemStack stack) {
        return stack.is(ModItems.LOLI_PICKAXE.get()) || stack.is(ModItems.SMALL_LOLI_PICKAXE.get());
    }
}
