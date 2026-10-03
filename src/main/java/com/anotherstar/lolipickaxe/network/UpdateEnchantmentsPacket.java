package com.anotherstar.lolipickaxe.network;

import com.anotherstar.lolipickaxe.client.screen.LegacyRegistryEditorScreen;
import com.anotherstar.lolipickaxe.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record UpdateEnchantmentsPacket(ListTag enchantments) {
    public static UpdateEnchantmentsPacket fromEntries(List<LegacyRegistryEditorScreen.Entry> entries) {
        ListTag list = new ListTag();
        for (LegacyRegistryEditorScreen.Entry entry : entries) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Name", entry.id().toString());
            tag.putInt("Level", entry.level());
            list.add(tag);
        }
        return new UpdateEnchantmentsPacket(list);
    }

    public static void encode(UpdateEnchantmentsPacket msg, FriendlyByteBuf buf) {
        CompoundTag root = new CompoundTag(); root.put("ench", msg.enchantments); buf.writeNbt(root);
    }
    public static UpdateEnchantmentsPacket decode(FriendlyByteBuf buf) {
        CompoundTag root = buf.readNbt();
        return new UpdateEnchantmentsPacket(root == null ? new ListTag() : root.getList("ench", Tag.TAG_COMPOUND));
    }

    public static void handle(UpdateEnchantmentsPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack stack = player.getMainHandItem();
            if (!stack.is(ModItems.LOLI_PICKAXE.get()) && !stack.is(ModItems.SMALL_LOLI_PICKAXE.get())) return;
            Map<Enchantment, Integer> map = new LinkedHashMap<>();
            for (int i = 0; i < msg.enchantments.size(); i++) {
                CompoundTag entry = msg.enchantments.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(entry.getString("Name"));
                if (id == null) continue;
                Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
                if (enchantment != null) map.put(enchantment, Math.max(0, Math.min(32, entry.getInt("Level"))));
            }
            EnchantmentHelper.setEnchantments(map, stack);
        });
        ctx.setPacketHandled(true);
    }
}
