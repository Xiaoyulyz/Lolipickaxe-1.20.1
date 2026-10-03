package com.anotherstar.lolipickaxe.item;

import com.anotherstar.lolipickaxe.asm.LoliEntityDefense;
import com.anotherstar.lolipickaxe.client.ClientPacketHandlers;
import com.anotherstar.lolipickaxe.entity.LoliEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.Comparator;

 
public final class LoliDispersalItem extends Item {
    private static final double SEARCH_RANGE = 192.0D;
    private static final double RAY_WIDTH = 3.0D;

    public interface DispersibleLoli {
        void lolipickaxe$setDispersal(boolean dispersal);
    }

    public LoliDispersalItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (!(target instanceof DispersibleLoli loli)) return InteractionResult.PASS;
        if (player.level().isClientSide) {
            player.swing(hand, true);
             
             
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientPacketHandlers.forceLocalLoliCleanup(target));
        } else {
            loli.lolipickaxe$setDispersal(true);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    



    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            player.swing(hand, true);
             
             
             
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientPacketHandlers.clearLookedAtLoliGhost(player, SEARCH_RANGE, RAY_WIDTH));
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResultHolder.pass(stack);

        LoliEntity target = findLookTarget(serverLevel, player);
        if (target == null) {
            target = LoliEntityDefense.findNearestAnchored(serverLevel, player, SEARCH_RANGE, RAY_WIDTH);
        }
        if (target == null) return InteractionResultHolder.pass(stack);

        target.lolipickaxe$setDispersal(true);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private static LoliEntity findLookTarget(ServerLevel level, Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 end = start.add(look.scale(SEARCH_RANGE));
        AABB search = player.getBoundingBox().expandTowards(look.scale(SEARCH_RANGE)).inflate(RAY_WIDTH);

        return level.getEntitiesOfClass(LoliEntity.class, search,
                        loli -> !loli.lolipickaxe$isDispersalRequested())
                .stream()
                .filter(loli -> rayDistanceSqr(start, end, loli.getBoundingBox().inflate(RAY_WIDTH)) >= 0.0D)
                .min(Comparator.comparingDouble(loli -> start.distanceToSqr(loli.position())))
                .orElse(null);
    }

     
    public static double rayDistanceSqr(Vec3 start, Vec3 end, AABB box) {
        return box.clip(start, end).map(start::distanceToSqr).orElse(-1.0D);
    }
}
