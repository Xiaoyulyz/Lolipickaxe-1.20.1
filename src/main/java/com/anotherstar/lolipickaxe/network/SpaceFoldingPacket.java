package com.anotherstar.lolipickaxe.network;

import com.anotherstar.lolipickaxe.config.LoliConfig;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import com.anotherstar.lolipickaxe.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

 
public record SpaceFoldingPacket(ResourceLocation dimension, double x, double y, double z) {
    public static void encode(SpaceFoldingPacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.dimension); buf.writeDouble(msg.x); buf.writeDouble(msg.y); buf.writeDouble(msg.z);
    }
    public static SpaceFoldingPacket decode(FriendlyByteBuf buf) {
        return new SpaceFoldingPacket(buf.readResourceLocation(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(SpaceFoldingPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || !LoliConfig.SPACE_FOLDING.get() || !hasLoli(player)) return;
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, msg.dimension);
            ServerLevel target = player.getServer() == null ? null : player.getServer().getLevel(key);
            if (target == null) return;
            Vec3 offset = new Vec3(msg.x, msg.y, msg.z);
            double max = LoliConfig.MAX_TELEPORT_DISTANCE.get();
            if (max > 0 && offset.length() > max) offset = offset.normalize().scale(max);
            player.stopRiding();
            player.teleportTo(target, player.getX() + offset.x, player.getY() + offset.y, player.getZ() + offset.z,
                    player.getYRot(), player.getXRot());
        });
        ctx.setPacketHandled(true);
    }

    private static boolean hasLoli(ServerPlayer player) {
        if (!LoliPickaxeItem.findOwnedPickaxe(player).isEmpty()) return true;
        for (ItemStack stack : player.getInventory().items) if (stack.is(ModItems.SMALL_LOLI_PICKAXE.get())) return true;
        for (ItemStack stack : player.getInventory().offhand) if (stack.is(ModItems.SMALL_LOLI_PICKAXE.get())) return true;
        return false;
    }
}
