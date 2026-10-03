package com.anotherstar.lolipickaxe.client;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.config.LegacyLoliSettings;
import com.anotherstar.lolipickaxe.config.LoliConfig;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.IdentityHashMap;
import java.util.Map;

 
@Mod.EventBusSubscriber(modid = LoliPickaxe.MOD_ID, value = Dist.CLIENT)
public final class ClientLegacyVisualEvents {
    private static final Map<LivingEntity, Boolean> FORCED_VISIBLE = new IdentityHashMap<>();

     
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hidePremiumPlayer(RenderPlayerEvent.Pre event) {
        ItemStack loli = LoliPickaxeItem.findOwnedPickaxe(event.getEntity());
        if (!loli.isEmpty() && LegacyLoliSettings.bool(
                loli, "loliPickaxeInvisible", LoliConfig.INVISIBLE.get())) {
            event.setCanceled(true);
        }
    }

    



    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void revealInvisibleLiving(RenderLivingEvent.Pre<?, ?> event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player viewer = minecraft.player;
        LivingEntity rendered = event.getEntity();
        if (viewer == null || rendered == null || !rendered.isInvisible()) return;
        ItemStack loli = LoliPickaxeItem.findOwnedPickaxe(viewer);
        if (loli.isEmpty() || !LegacyLoliSettings.bool(
                loli, "loliPickaxeShowInvisible", LoliConfig.SHOW_INVISIBLE.get())) return;

        FORCED_VISIBLE.putIfAbsent(rendered, Boolean.TRUE);
        rendered.setInvisible(false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void restoreInvisibleLiving(RenderLivingEvent.Post<?, ?> event) {
        restore(event.getEntity());
    }

    



    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) return;

        ItemStack loli = LoliPickaxeItem.findOwnedPickaxe(player);
        if (loli.isEmpty() || !LegacyLoliSettings.bool(
                loli, "loliPickaxeStopOnLiquid", LoliConfig.STOP_ON_LIQUID.get())) return;
        HitResult current = minecraft.hitResult;
        if (current != null && current.getType() == HitResult.Type.ENTITY) return;

        double configured = LegacyLoliSettings.decimal(
                loli, "loliPickaxeBlockReachDistance", LoliConfig.BLOCK_REACH_DISTANCE.get());
        double attributeReach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        double reach = Math.max(5.0D, Math.max(configured, attributeReach));
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = start.add(player.getViewVector(1.0F).normalize().scale(reach));
        BlockHitResult liquidHit = minecraft.level.clip(new ClipContext(
                start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
        if (liquidHit.getType() == HitResult.Type.MISS
                || minecraft.level.getFluidState(liquidHit.getBlockPos()).isEmpty()) return;

        double liquidDistance = start.distanceToSqr(liquidHit.getLocation());
        double currentDistance = current == null || current.getType() == HitResult.Type.MISS
                ? Double.POSITIVE_INFINITY : start.distanceToSqr(current.getLocation());
        if (liquidDistance <= currentDistance + 1.0E-6D) minecraft.hitResult = liquidHit;
    }

     
    @SubscribeEvent
    public static void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || FORCED_VISIBLE.isEmpty()) return;
        for (LivingEntity entity : java.util.List.copyOf(FORCED_VISIBLE.keySet())) restore(entity);
    }

    private static void restore(Entity entity) {
        if (!(entity instanceof LivingEntity living) || FORCED_VISIBLE.remove(living) == null) return;
        living.setInvisible(true);
    }

    private ClientLegacyVisualEvents() {}
}
