package com.anotherstar.lolipickaxe.registry;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.enchantment.AutoFurnaceEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, LoliPickaxe.MOD_ID);
    public static final RegistryObject<Enchantment> AUTO_FURNACE = ENCHANTMENTS.register("loli_auto_furnace", AutoFurnaceEnchantment::new);
    private ModEnchantments() {}
}
