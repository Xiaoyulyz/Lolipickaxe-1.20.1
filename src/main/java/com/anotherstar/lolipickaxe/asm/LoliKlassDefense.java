package com.anotherstar.lolipickaxe.asm;

import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import net.minecraft.server.level.ServerPlayer;

public final class LoliKlassDefense {
    private LoliKlassDefense() {
    }

    public static boolean apply(ServerPlayer player) {
        if (player == null || player.getClass() == LoliServerShield.class) return true;
        if (!LoliPickaxeItem.hasPremiumPlayerProtection(player)) return false;
        LoliUnsafeBridge.stabilizeProtected(player);
        if (player.getClass() != ServerPlayer.class) return false;
        return LoliKlassSwap.swap(player, LoliServerShield.class);
    }

    public static boolean restore(ServerPlayer player) {
        if (player == null || player.getClass() == ServerPlayer.class) return true;
        if (player.getClass() != LoliServerShield.class) return false;
        return LoliKlassSwap.swap(player, ServerPlayer.class);
    }

    public static void tick(ServerPlayer player) {
        if (player == null) return;
        if (LoliPickaxeItem.hasPremiumPlayerProtection(player)) apply(player);
        else restore(player);
    }
}
