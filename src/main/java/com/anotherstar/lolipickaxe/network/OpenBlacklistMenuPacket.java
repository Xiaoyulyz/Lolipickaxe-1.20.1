package com.anotherstar.lolipickaxe.network;

import com.anotherstar.lolipickaxe.menu.LoliBlacklistMenu;
import com.anotherstar.lolipickaxe.registry.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public record OpenBlacklistMenuPacket() {
    public static void encode(OpenBlacklistMenuPacket msg, FriendlyByteBuf buf) {}
    public static OpenBlacklistMenuPacket decode(FriendlyByteBuf buf) { return new OpenBlacklistMenuPacket(); }

    public static void handle(OpenBlacklistMenuPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            InteractionHand hand = findHand(player);
            if (hand == null) return;
            NetworkHooks.openScreen(player, new SimpleMenuProvider(
                    (id, inventory, ignored) -> new LoliBlacklistMenu(id, inventory, hand),
                    Component.empty()), buf -> buf.writeEnum(hand));
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
