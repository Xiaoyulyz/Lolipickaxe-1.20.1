package com.anotherstar.lolipickaxe.network;

import com.anotherstar.lolipickaxe.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

 
public record ForceEntityCleanupPacket(List<Target> targets, boolean allowProtectedLoli) {
    public ForceEntityCleanupPacket {
        targets = targets == null ? List.of() : List.copyOf(targets);
    }

    public static void encode(ForceEntityCleanupPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.allowProtectedLoli);
        buffer.writeVarInt(packet.targets.size());
        for (Target target : packet.targets) {
            buffer.writeVarInt(target.entityId);
            buffer.writeUUID(target.entityUuid);
        }
    }

    public static ForceEntityCleanupPacket decode(FriendlyByteBuf buffer) {
        boolean allowProtectedLoli = buffer.readBoolean();
        int size = Math.max(0, Math.min(buffer.readVarInt(), 4096));
        List<Target> targets = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            targets.add(new Target(buffer.readVarInt(), buffer.readUUID()));
        }
        return new ForceEntityCleanupPacket(targets, allowProtectedLoli);
    }

    public static void handle(ForceEntityCleanupPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.forceEntityCleanup(packet.targets, packet.allowProtectedLoli)));
        context.setPacketHandled(true);
    }

    public record Target(int entityId, UUID entityUuid) {}
}
