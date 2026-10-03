package com.anotherstar.lolipickaxe.asm;

import com.anotherstar.lolipickaxe.entity.LoliEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

 
public final class LoliEntityDefense {
    public static final String DISPERSAL_AUTHORIZED_TAG = "LoliDispersalAuthorized";
    public static boolean isAuthorized(LoliEntity entity) {
        return entity.getPersistentData().getBoolean(DISPERSAL_AUTHORIZED_TAG);
    }
    public static void authorizeRemoval(LoliEntity entity) {
        entity.getPersistentData().putBoolean(DISPERSAL_AUTHORIZED_TAG, true);
    }
     
    public static boolean isReplacementCleanup(LoliEntity entity) { return false; }
    public static LoliEntity findNearestAnchored(ServerLevel level, Player player, double range, double width) {
        return null;  
    }
    public static void restoreSafePosition(LoliEntity entity) {
        entity.moveTo(entity.getX(), Math.max(64.0D, entity.level().getMinBuildHeight() + 2.0D),
                entity.getZ(), entity.getYRot(), entity.getXRot());
        entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }
    private LoliEntityDefense() {}
}
