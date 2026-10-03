package com.anotherstar.lolipickaxe;

import com.anotherstar.lolipickaxe.config.LoliConfig;
import com.anotherstar.lolipickaxe.network.ModNetwork;
import com.anotherstar.lolipickaxe.registry.ModBlocks;
import com.anotherstar.lolipickaxe.registry.ModCreativeTabs;
import com.anotherstar.lolipickaxe.registry.ModEnchantments;
import com.anotherstar.lolipickaxe.registry.ModEntities;
import com.anotherstar.lolipickaxe.registry.ModItems;
import com.anotherstar.lolipickaxe.registry.ModMenus;
import com.anotherstar.lolipickaxe.registry.ModRecipeSerializers;
import com.anotherstar.lolipickaxe.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(LoliPickaxe.MOD_ID)
public final class LoliPickaxe {
    public static final String MOD_ID = "lolipickaxe";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LoliPickaxe() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModEnchantments.ENCHANTMENTS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModRecipeSerializers.SERIALIZERS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, LoliConfig.SPEC);
        ModNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
    }

}
