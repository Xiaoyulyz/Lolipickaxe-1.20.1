package com.anotherstar.lolipickaxe.network;

import com.anotherstar.lolipickaxe.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SafeAttackEffectPacket(SafeAttackEffect effect) {
    public static void encode(SafeAttackEffectPacket msg, FriendlyByteBuf buf) { buf.writeEnum(msg.effect); }
    public static SafeAttackEffectPacket decode(FriendlyByteBuf buf) { return new SafeAttackEffectPacket(buf.readEnum(SafeAttackEffect.class)); }
    public static void handle(SafeAttackEffectPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandlers.showSafeAttack(msg.effect)));
        ctx.setPacketHandled(true);
    }
}
