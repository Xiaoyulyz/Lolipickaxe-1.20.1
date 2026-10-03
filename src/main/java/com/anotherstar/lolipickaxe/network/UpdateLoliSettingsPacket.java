package com.anotherstar.lolipickaxe.network;

import com.anotherstar.lolipickaxe.config.LegacyGuiConfig;
import com.anotherstar.lolipickaxe.config.LegacyLoliSettings;
import com.anotherstar.lolipickaxe.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UpdateLoliSettingsPacket(CompoundTag settings) {
    public static void encode(UpdateLoliSettingsPacket msg, FriendlyByteBuf buf) { buf.writeNbt(msg.settings); }
    public static UpdateLoliSettingsPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new UpdateLoliSettingsPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(UpdateLoliSettingsPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack stack = player.getMainHandItem();
            if (!stack.is(ModItems.LOLI_PICKAXE.get()) && !stack.is(ModItems.SMALL_LOLI_PICKAXE.get())) return;
            CompoundTag sanitized = LegacyGuiConfig.sanitize(msg.settings);
            stack.getOrCreateTag().put(LegacyLoliSettings.ROOT, sanitized.copy());

             
             
             
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            if (player.inventoryMenu != player.containerMenu) player.inventoryMenu.broadcastChanges();

        });
        ctx.setPacketHandled(true);
    }
}
