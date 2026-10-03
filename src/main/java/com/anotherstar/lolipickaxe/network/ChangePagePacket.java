package com.anotherstar.lolipickaxe.network;

import com.anotherstar.lolipickaxe.menu.LoliPickaxeMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ChangePagePacket(int delta) {
    public static void encode(ChangePagePacket msg, FriendlyByteBuf buf) { buf.writeByte(msg.delta); }
    public static ChangePagePacket decode(FriendlyByteBuf buf) { return new ChangePagePacket(buf.readByte()); }
    public static void handle(ChangePagePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null && player.containerMenu instanceof LoliPickaxeMenu menu) menu.changePage(msg.delta);
        });
        ctx.setPacketHandled(true);
    }
}
