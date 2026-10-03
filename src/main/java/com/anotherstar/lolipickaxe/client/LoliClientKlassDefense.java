package com.anotherstar.lolipickaxe.client;

import com.anotherstar.lolipickaxe.asm.LoliKlassSwap;
import com.anotherstar.lolipickaxe.item.LoliPickaxeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class LoliClientKlassDefense {
    private LoliClientKlassDefense() {
    }

    public static boolean apply(LocalPlayer player) {
        if (player == null || player.getClass() == LoliClientShield.class) return true;
        if (!LoliPickaxeItem.hasPremiumPlayerProtection(player)) return false;
        if (player.getClass() != LocalPlayer.class) return false;
        return LoliKlassSwap.swap(player, LoliClientShield.class);
    }

    public static boolean restore(LocalPlayer player) {
        if (player == null || player.getClass() == LocalPlayer.class) return true;
        if (player.getClass() != LoliClientShield.class) return false;
        return LoliKlassSwap.swap(player, LocalPlayer.class);
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (LoliPickaxeItem.hasPremiumPlayerProtection(minecraft.player)) apply(minecraft.player);
        else restore(minecraft.player);
    }
}
