package com.anotherstar.lolipickaxe.network;

import com.anotherstar.lolipickaxe.menu.PasswordWorkbenchMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UpdatePasswordPacket(String password) {
    public static void encode(UpdatePasswordPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.password == null ? "" : packet.password);
    }

    public static UpdatePasswordPacket decode(FriendlyByteBuf buffer) {
        return new UpdatePasswordPacket(buffer.readUtf());
    }

    public static void handle(UpdatePasswordPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null && context.getSender().containerMenu instanceof PasswordWorkbenchMenu menu) {
                menu.setPassword(packet.password);
            }
        });
        context.setPacketHandled(true);
    }
}
