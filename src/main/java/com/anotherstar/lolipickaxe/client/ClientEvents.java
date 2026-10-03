package com.anotherstar.lolipickaxe.client;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.client.screen.LoliPickaxeScreen;
import com.anotherstar.lolipickaxe.client.screen.LoliBlacklistScreen;
import com.anotherstar.lolipickaxe.client.screen.PasswordWorkbenchScreen;
import com.anotherstar.lolipickaxe.client.screen.LegacyLoliConfigScreen;
import com.anotherstar.lolipickaxe.client.screen.LegacyEnchantmentScreen;
import com.anotherstar.lolipickaxe.client.screen.LegacyPotionScreen;
import com.anotherstar.lolipickaxe.client.screen.LegacySpaceFoldingScreen;
import com.anotherstar.lolipickaxe.item.AddonItem;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import com.anotherstar.lolipickaxe.asm.LoliDefenseHooks;
import com.anotherstar.lolipickaxe.network.ModNetwork;
import com.anotherstar.lolipickaxe.network.OpenPickaxeMenuPacket;
import com.anotherstar.lolipickaxe.network.OpenBlacklistMenuPacket;
import com.anotherstar.lolipickaxe.registry.ModItems;
import com.anotherstar.lolipickaxe.registry.ModMenus;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class ClientEvents {
    public static final String CATEGORY = "key.category.lolipickaxe";
    public static final KeyMapping LOLI_CONFIG = key("key.lolipickaxe.loli_config", GLFW.GLFW_KEY_N);
    public static final KeyMapping LOLI_ENCHANTMENT = key("key.lolipickaxe.loli_enchantment", GLFW.GLFW_KEY_M);
    public static final KeyMapping LOLI_POTION = key("key.lolipickaxe.loli_potion", GLFW.GLFW_KEY_P);
    public static final KeyMapping LOLI_SPACE_FOLDING = key("key.lolipickaxe.loli_space_folding", GLFW.GLFW_KEY_K);
    public static final KeyMapping LOLI_CONTAINER = key("key.lolipickaxe.loli_container", GLFW.GLFW_KEY_B);
    public static final KeyMapping LOLI_BLACKLIST = key("key.lolipickaxe.loli_container_blacklist", GLFW.GLFW_KEY_U);

    private static KeyMapping key(String name, int key) {
        return new KeyMapping(name, InputConstants.Type.KEYSYM, key, CATEGORY);
    }

    @Mod.EventBusSubscriber(modid = LoliPickaxe.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(LOLI_CONFIG);
            event.register(LOLI_ENCHANTMENT);
            event.register(LOLI_POTION);
            event.register(LOLI_SPACE_FOLDING);
            event.register(LOLI_CONTAINER);
            event.register(LOLI_BLACKLIST);
        }

        @SubscribeEvent
        public static void registerScreens(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(ModMenus.LOLI_PICKAXE.get(), LoliPickaxeScreen::new);
                MenuScreens.register(ModMenus.LOLI_BLACKLIST.get(), LoliBlacklistScreen::new);
                MenuScreens.register(ModMenus.PASSWORD_WORKBENCH.get(), PasswordWorkbenchScreen::new);
                ItemProperties.register(ModItems.LOLI_ENTITY_SOUL_ADDON.get(),
                        new ResourceLocation(LoliPickaxe.MOD_ID, "end"),
                        (stack, level, entity, seed) -> stack.getItem() instanceof AddonItem addon
                                && addon.level(stack) >= addon.type().maxLevel() ? 1.0F : 0.0F);
            });
        }
    }

    @Mod.EventBusSubscriber(modid = LoliPickaxe.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            ClientPacketHandlers.tickBugEntityCleanup();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            LoliClientKlassDefense.tick();

             
             
             
             
            if (minecraft.screen instanceof DeathScreen
                    && LoliPickaxeItem.hasLoliProtection(minecraft.player)) {
                try {
                    LoliDefenseHooks.maintain(minecraft.player);
                } catch (Throwable ignored) {
                }
                minecraft.setScreen(null);
            }
            while (LOLI_CONFIG.consumeClick()) {
                net.minecraft.world.item.ItemStack held = Minecraft.getInstance().player.getMainHandItem();
                if (held.is(ModItems.LOLI_PICKAXE.get()) || held.is(ModItems.SMALL_LOLI_PICKAXE.get())) {
                    Minecraft.getInstance().setScreen(new LegacyLoliConfigScreen(held));
                }
            }
            while (LOLI_ENCHANTMENT.consumeClick()) {
                net.minecraft.world.item.ItemStack held = Minecraft.getInstance().player.getMainHandItem();
                if (held.is(ModItems.LOLI_PICKAXE.get()) || held.is(ModItems.SMALL_LOLI_PICKAXE.get())) {
                    Minecraft.getInstance().setScreen(new LegacyEnchantmentScreen(held));
                }
            }
            while (LOLI_POTION.consumeClick()) {
                net.minecraft.world.item.ItemStack held = Minecraft.getInstance().player.getMainHandItem();
                if (held.is(ModItems.LOLI_PICKAXE.get()) || held.is(ModItems.SMALL_LOLI_PICKAXE.get())) {
                    Minecraft.getInstance().setScreen(new LegacyPotionScreen(held));
                }
            }
            while (LOLI_SPACE_FOLDING.consumeClick()) {
                net.minecraft.world.item.ItemStack held = Minecraft.getInstance().player.getMainHandItem();
                if (com.anotherstar.lolipickaxe.config.LoliConfig.SPACE_FOLDING.get()
                        && (held.is(ModItems.LOLI_PICKAXE.get()) || held.is(ModItems.SMALL_LOLI_PICKAXE.get()))) {
                    Minecraft.getInstance().setScreen(new LegacySpaceFoldingScreen());
                }
            }
            while (LOLI_CONTAINER.consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(new OpenPickaxeMenuPacket(net.minecraft.client.gui.screens.Screen.hasShiftDown()));
            }
            while (LOLI_BLACKLIST.consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(new OpenBlacklistMenuPacket());
            }
        }
    }

    private ClientEvents() {}
}
