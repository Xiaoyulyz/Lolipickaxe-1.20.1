package com.anotherstar.lolipickaxe.network;

import com.anotherstar.lolipickaxe.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

 
public record RefreshHeldItemPacket(InteractionHand hand) {
    public static void encode(RefreshHeldItemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
    }

    public static RefreshHeldItemPacket decode(FriendlyByteBuf buffer) {
        return new RefreshHeldItemPacket(buffer.readEnum(InteractionHand.class));
    }

    public static void handle(RefreshHeldItemPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.refreshHeldItem(packet.hand)));
        context.setPacketHandled(true);
    }
}
