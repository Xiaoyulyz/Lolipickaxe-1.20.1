package com.anotherstar.lolipickaxe.client;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.config.LegacyLoliSettings;
import com.anotherstar.lolipickaxe.config.LoliConfig;
import com.anotherstar.lolipickaxe.network.KillFacingPacket;
import com.anotherstar.lolipickaxe.network.ModNetwork;
import com.anotherstar.lolipickaxe.registry.ModItems;
import com.anotherstar.lolipickaxe.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;


@Mod.EventBusSubscriber(modid = LoliPickaxe.MOD_ID, value = Dist.CLIENT)
public final class ClientInteractionEvents {
    private static int lastAttackPacketTick = Integer.MIN_VALUE;
    




    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.player.isSpectator()) return;
        ItemStack stack = minecraft.player.getMainHandItem();
        if (!stack.is(ModItems.LOLI_PICKAXE.get())) return;

         
         
        minecraft.player.playSound(ModSounds.LOLI_SUCCESS.get(), 1.0F, 1.0F);

        HitResult hit = minecraft.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            sendPremiumAttack(minecraft);
            return;
        }
        if (!hasLongRangeCombatCandidate(minecraft, stack)) return;
        sendPremiumAttack(minecraft);

         
         
        event.setCanceled(true);
        event.setSwingHand(true);
        minecraft.player.resetAttackStrengthTicker();
    }

     
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (!event.getLevel().isClientSide || event.getEntity() != Minecraft.getInstance().player) return;
        if (!event.getItemStack().is(ModItems.LOLI_PICKAXE.get())) return;
        sendPremiumAttack(Minecraft.getInstance());
    }

    private static void sendPremiumAttack(Minecraft minecraft) {
        if (minecraft.player == null) return;
        int tick = minecraft.player.tickCount;
        if (lastAttackPacketTick == tick) return;
        lastAttackPacketTick = tick;

        int preferredId = -1;
        if (minecraft.hitResult instanceof EntityHitResult entityHit) {
            Entity preferred = entityHit.getEntity();
            if (preferred instanceof PartEntity<?> part && part.getParent() != null) {
                preferred = part.getParent();
            }
            preferredId = preferred.getId();
        }
        Vec3 look = minecraft.player.getLookAngle().normalize();
        ModNetwork.CHANNEL.sendToServer(new KillFacingPacket(preferredId, look.x, look.y, look.z));
    }

    private static boolean hasLongRangeCombatCandidate(Minecraft minecraft, ItemStack stack) {
        if (minecraft.player == null || minecraft.level == null) return false;
        double facingRange = LegacyLoliSettings.integer(stack, "loliPickaxeKillFacingRange",
                LoliConfig.KILL_FACING_RANGE.get());
        double blockReach = LegacyLoliSettings.decimal(stack, "loliPickaxeBlockReachDistance",
                LoliConfig.BLOCK_REACH_DISTANCE.get());
        double range = Math.max(5.0D, Math.max(facingRange, blockReach));

        Vec3 start = minecraft.player.getEyePosition();
        Vec3 direction = minecraft.player.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(range));
        double broadMargin = Math.max(4.0D, Math.min(12.0D, range * 0.08D));
        AABB search = minecraft.player.getBoundingBox()
                .expandTowards(direction.scale(range)).inflate(broadMargin);
        boolean all = LegacyLoliSettings.bool(stack, "loliPickaxeValidToAllEntity",
                LoliConfig.VALID_TO_ALL_ENTITY.get());

        java.util.LinkedHashSet<Entity> roots = new java.util.LinkedHashSet<>();
        for (Entity raw : minecraft.level.getEntities(minecraft.player, search,
                entity -> entity != minecraft.player)) {
            Entity candidate = raw;
            if (candidate instanceof PartEntity<?> part && part.getParent() != null) {
                candidate = part.getParent();
            }
            if (!all && !isLivingCandidate(candidate)) continue;
            roots.add(candidate);
        }

        for (Entity candidate : roots) {
            AABB rawBounds = candidate.getBoundingBox();
            AABB bounds = rawBounds.inflate(Math.max(0.5D, candidate.getPickRadius()));
            if (bounds.contains(start) || bounds.clip(start, end).isPresent()) return true;

            Vec3 center = rawBounds.getCenter();
            double projection = center.subtract(start).dot(direction);
            if (projection < 0.0D || projection > range) continue;
            double perpendicularSqr = center.distanceToSqr(start.add(direction.scale(projection)));
            double width = Math.max(0.1D, rawBounds.getXsize());
            double height = Math.max(0.1D, rawBounds.getYsize());
            double depth = Math.max(0.1D, rawBounds.getZsize());
            double modelRadius = Math.max(1.0D,
                    Math.max(candidate.getPickRadius() + 0.5D,
                            Math.max(Math.max(width, depth) * 0.75D, height * 0.35D)));
            double distanceAssist = Math.min(3.5D, 0.5D + projection * 0.02D);
            double allowed = modelRadius + distanceAssist;
            if (perpendicularSqr <= allowed * allowed) return true;
        }
        return false;
    }

    private static boolean isLivingCandidate(Entity entity) {
        if (entity instanceof LivingEntity) return true;
        return entity instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity;
    }

    private ClientInteractionEvents() {}
}
