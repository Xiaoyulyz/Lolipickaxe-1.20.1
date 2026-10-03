package com.anotherstar.lolipickaxe.gametest;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.client.screen.LegacyLoliConfigScreen;
import com.anotherstar.lolipickaxe.client.render.LoliUiShaders;
import com.anotherstar.lolipickaxe.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

 
@Mod.EventBusSubscriber(modid = LoliPickaxe.MOD_ID, value = Dist.CLIENT)
public final class LoliUiSmokeTest {
    private static int frames;
    private static boolean opened;
    private static boolean captured;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("lolipickaxe.uiSmokeTest") || event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        if (client.getOverlay() != null) return;
        if (!opened) {
            if (!LoliUiShaders.isReady()) throw new IllegalStateException("Pure-white UI shader did not load");
            client.setScreen(new LegacyLoliConfigScreen(new ItemStack(ModItems.LOLI_PICKAXE.get())));
            opened = true;
        }
        if (!captured && ++frames >= 60) {
            captured = true;
            Screenshot.grab(client.gameDirectory, "n-key-pure-white.png", client.getMainRenderTarget(), message -> {
                LoliPickaxe.LOGGER.info("Pure-white UI smoke screenshot: {}", message.getString());
                client.execute(client::stop);
            });
        }
    }

    private LoliUiSmokeTest() {}
}
