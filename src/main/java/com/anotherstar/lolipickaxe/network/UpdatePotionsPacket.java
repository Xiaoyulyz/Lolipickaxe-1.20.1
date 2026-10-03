package com.anotherstar.lolipickaxe.network;

import com.anotherstar.lolipickaxe.client.screen.LegacyRegistryEditorScreen;
import com.anotherstar.lolipickaxe.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record UpdatePotionsPacket(ListTag potions) {
    public static UpdatePotionsPacket fromEntries(List<LegacyRegistryEditorScreen.Entry> entries) {
         
        Map<ResourceLocation, Integer> map = new LinkedHashMap<>();
        for (LegacyRegistryEditorScreen.Entry entry : entries) map.put(entry.id(), entry.level());
        ListTag list = new ListTag();
        for (Map.Entry<ResourceLocation, Integer> entry : map.entrySet()) {
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(entry.getKey());
            if (effect == null) continue;
            CompoundTag tag = new CompoundTag();
            tag.putShort("id", (short) BuiltInRegistries.MOB_EFFECT.getId(effect));
            tag.putByte("lvl", (byte) Math.max(0, Math.min(32, entry.getValue())));
            tag.putString("name", entry.getKey().toString());
            list.add(tag);
        }
        return new UpdatePotionsPacket(list);
    }

    public static void encode(UpdatePotionsPacket msg, FriendlyByteBuf buf) {
        CompoundTag root = new CompoundTag(); root.put("LoliPotion", msg.potions); buf.writeNbt(root);
    }
    public static UpdatePotionsPacket decode(FriendlyByteBuf buf) {
        CompoundTag root = buf.readNbt();
        return new UpdatePotionsPacket(root == null ? new ListTag() : root.getList("LoliPotion", Tag.TAG_COMPOUND));
    }

    public static void handle(UpdatePotionsPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack stack = player.getMainHandItem();
            if (!stack.is(ModItems.LOLI_PICKAXE.get()) && !stack.is(ModItems.SMALL_LOLI_PICKAXE.get())) return;
            ListTag sanitized = new ListTag();
            Map<ResourceLocation, Integer> ordered = new LinkedHashMap<>();
            for (int i = 0; i < msg.potions.size(); i++) {
                CompoundTag input = msg.potions.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(input.getString("name"));
                MobEffect effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id);
                if (effect == null) effect = BuiltInRegistries.MOB_EFFECT.byId(input.getShort("id"));
                if (effect != null) ordered.put(BuiltInRegistries.MOB_EFFECT.getKey(effect), Math.max(0, Math.min(32, input.getByte("lvl"))));
            }
            for (Map.Entry<ResourceLocation, Integer> entry : ordered.entrySet()) {
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(entry.getKey());
                if (effect == null) continue;
                CompoundTag output = new CompoundTag();
                output.putShort("id", (short) BuiltInRegistries.MOB_EFFECT.getId(effect));
                output.putByte("lvl", entry.getValue().byteValue());
                output.putString("name", entry.getKey().toString());
                sanitized.add(output);
            }
            if (sanitized.isEmpty()) {
                if (stack.hasTag()) stack.getTag().remove("LoliPotion");
            } else stack.getOrCreateTag().put("LoliPotion", sanitized);
        });
        ctx.setPacketHandled(true);
    }
}
