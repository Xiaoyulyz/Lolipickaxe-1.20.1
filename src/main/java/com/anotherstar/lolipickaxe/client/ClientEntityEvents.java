package com.anotherstar.lolipickaxe.client;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.client.model.LoliModel;
import com.anotherstar.lolipickaxe.client.renderer.LoliRenderer;
import com.anotherstar.lolipickaxe.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LoliPickaxe.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEntityEvents {
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LoliModel.LAYER, LoliModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LOLI.get(), LoliRenderer::new);
    }

    private ClientEntityEvents() {}
}
