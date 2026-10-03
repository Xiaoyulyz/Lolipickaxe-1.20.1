package com.anotherstar.lolipickaxe.event;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.asm.LoliErasureState;
import com.anotherstar.lolipickaxe.asm.LoliStructuralPurge;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

 
@Mod.EventBusSubscriber(modid = LoliPickaxe.MOD_ID)
public final class LoliEntityDefenseEvents {
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LoliErasureState.clear();
        LoliStructuralPurge.clearClientCleanup();
    }
    private LoliEntityDefenseEvents() {}
}
