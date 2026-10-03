package com.anotherstar.lolipickaxe.network;

import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import com.anotherstar.lolipickaxe.registry.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

 
public final class KillFacingPacket {
    private final int preferredEntityId;
    private final double lookX;
    private final double lookY;
    private final double lookZ;

    public KillFacingPacket() {
        this(-1, 0.0D, 0.0D, 0.0D);
    }

    public KillFacingPacket(int preferredEntityId, double lookX, double lookY, double lookZ) {
        this.preferredEntityId = preferredEntityId;
        this.lookX = lookX;
        this.lookY = lookY;
        this.lookZ = lookZ;
    }

    public static void encode(KillFacingPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.preferredEntityId);
        buffer.writeDouble(packet.lookX);
        buffer.writeDouble(packet.lookY);
        buffer.writeDouble(packet.lookZ);
    }

    public static KillFacingPacket decode(FriendlyByteBuf buffer) {
        return new KillFacingPacket(buffer.readVarInt(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(KillFacingPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.isSpectator()) return;
            ItemStack stack = player.getMainHandItem();
            if (!stack.is(ModItems.LOLI_PICKAXE.get())) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            Entity preferred = packet.preferredEntityId < 0 ? null : level.getEntity(packet.preferredEntityId);
            Vec3 look = new Vec3(packet.lookX, packet.lookY, packet.lookZ);
            if (!Double.isFinite(look.x) || !Double.isFinite(look.y) || !Double.isFinite(look.z)
                    || look.lengthSqr() < 1.0E-8D) {
                look = player.getLookAngle();
            }
            LoliPickaxeItem.attackFromPremiumClick(level, player, stack, preferred, look.normalize());
        });
        context.setPacketHandled(true);
    }
}
