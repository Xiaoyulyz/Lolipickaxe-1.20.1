package com.anotherstar.lolipickaxe.client.screen;

import com.anotherstar.lolipickaxe.network.ModNetwork;
import com.anotherstar.lolipickaxe.network.UpdatePotionsPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class LegacyPotionScreen extends LegacyRegistryEditorScreen {
    public LegacyPotionScreen(ItemStack stack) { super(stack, Component.empty()); }

    @Override protected List<ResourceLocation> allRegistryIds() { return new ArrayList<>(BuiltInRegistries.MOB_EFFECT.keySet()); }

    @Override
    protected List<Entry> readSelected(ItemStack stack) {
        List<Entry> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("LoliPotion", Tag.TAG_LIST)) return result;
        ListTag list = tag.getList("LoliPotion", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag element = list.getCompound(i);
            MobEffect effect = null;
            if (element.contains("name")) {
                ResourceLocation id = ResourceLocation.tryParse(element.getString("name"));
                if (id != null) effect = BuiltInRegistries.MOB_EFFECT.get(id);
            }
            if (effect == null) effect = BuiltInRegistries.MOB_EFFECT.byId(element.getShort("id"));
            ResourceLocation id = effect == null ? null : BuiltInRegistries.MOB_EFFECT.getKey(effect);
            if (id != null) result.add(new Entry(id, element.getByte("lvl")));
        }
        return result;
    }

    @Override
    protected Component display(ResourceLocation id, int level, boolean selectedList) {
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
        if (effect == null) return Component.literal(id.toString());
        Component name = effect.getDisplayName();
        return selectedList ? name.copy().append(" ").append(Component.translatable("enchantment.level." + (level + 1))) : name;
    }

    @Override protected int defaultLevel() { return 0; }
    @Override protected void save(List<Entry> entries) { ModNetwork.CHANNEL.sendToServer(UpdatePotionsPacket.fromEntries(entries)); }
    @Override protected Component editorLabel() { return Component.translatable("gui.loliPotion"); }
}
