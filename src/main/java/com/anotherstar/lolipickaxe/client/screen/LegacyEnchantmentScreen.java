package com.anotherstar.lolipickaxe.client.screen;

import com.anotherstar.lolipickaxe.network.ModNetwork;
import com.anotherstar.lolipickaxe.network.UpdateEnchantmentsPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LegacyEnchantmentScreen extends LegacyRegistryEditorScreen {
    public LegacyEnchantmentScreen(ItemStack stack) { super(stack, Component.empty()); }

    @Override protected List<ResourceLocation> allRegistryIds() { return new ArrayList<>(ForgeRegistries.ENCHANTMENTS.getKeys()); }

    @Override
    protected List<Entry> readSelected(ItemStack stack) {
        List<Entry> result = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : EnchantmentHelper.getEnchantments(stack).entrySet()) {
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey());
            if (id != null) result.add(new Entry(id, entry.getValue()));
        }
        return result;
    }

    @Override
    protected Component display(ResourceLocation id, int level, boolean selectedList) {
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
        if (enchantment == null) return Component.literal(id.toString());
        Component name = Component.translatable(enchantment.getDescriptionId());
        if (enchantment.isCurse()) name = name.copy().withStyle(ChatFormatting.RED);
        if (selectedList && (level != 1 || enchantment.getMaxLevel() != 1)) {
            name = name.copy().append(" ").append(Component.translatable("enchantment.level." + level));
        }
        return name;
    }

    @Override protected int defaultLevel() { return 1; }
    @Override protected void save(List<Entry> entries) { ModNetwork.CHANNEL.sendToServer(UpdateEnchantmentsPacket.fromEntries(entries)); }
    @Override protected Component editorLabel() { return Component.translatable("gui.loliEnch"); }
}
