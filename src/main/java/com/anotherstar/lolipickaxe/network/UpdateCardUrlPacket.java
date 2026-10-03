package com.anotherstar.lolipickaxe.network;

import com.anotherstar.lolipickaxe.registry.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UpdateCardUrlPacket(InteractionHand hand, String url) {
    public static void encode(UpdateCardUrlPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.hand);
        buf.writeUtf(msg.url, 500);
    }
    public static UpdateCardUrlPacket decode(FriendlyByteBuf buf) {
        return new UpdateCardUrlPacket(buf.readEnum(InteractionHand.class), buf.readUtf(500));
    }
    public static void handle(UpdateCardUrlPacket msg, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(msg.hand);
            if (stack.is(ModItems.LOLI_CARD_ONLINE.get())) stack.getOrCreateTag().putString("ImageUrl", msg.url);
        });
        ctx.setPacketHandled(true);
    }
}
