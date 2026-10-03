package com.anotherstar.lolipickaxe.event;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.asm.LoliErasureState;
import com.anotherstar.lolipickaxe.asm.LoliStructuralPurge;
import com.anotherstar.lolipickaxe.entity.LoliEntity;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

 
@Mod.EventBusSubscriber(modid = LoliPickaxe.MOD_ID)
public final class LoliAsmEvents {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof LoliEntity loli && loli.lolipickaxe$isRemovalAuthorized()) return;
        if (LoliPickaxeItem.hasLoliProtection(entity)) {
            LoliErasureState.stabilizeProtected(entity);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHeal(LivingHealEvent event) {
        if ((LoliErasureState.isErased(event.getEntity()))
                && !LoliPickaxeItem.hasLoliProtection(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (LoliErasureState.shouldRejectJoin(event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof LoliEntity) return;
        if (event.getEntity() instanceof LivingEntity living) {
            LoliErasureState.registerIfErased(living);
            if (living instanceof ServerPlayer player) LoliErasureState.applyPersistentPunishments(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.getEntity().level().isClientSide) {
            LoliErasureState.copyPlayerPunishments(event.getOriginal(), event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LoliErasureState.applyPersistentPunishments(player);
            if (LoliPickaxeItem.hasLoliProtection(player)) LoliErasureState.stabilizeProtected(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LoliErasureState.applyPersistentPunishments(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            LoliErasureState.tick(server);
            LoliStructuralPurge.tickClientCleanup(server);
        }
    }

    private LoliAsmEvents() {}
}
