package com.anotherstar.lolipickaxe.network;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "8";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(LoliPickaxe.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private static int id;

    public static void register() {
        CHANNEL.registerMessage(id++, OpenPickaxeMenuPacket.class,
                OpenPickaxeMenuPacket::encode, OpenPickaxeMenuPacket::decode, OpenPickaxeMenuPacket::handle);
        CHANNEL.registerMessage(id++, OpenBlacklistMenuPacket.class,
                OpenBlacklistMenuPacket::encode, OpenBlacklistMenuPacket::decode, OpenBlacklistMenuPacket::handle);
        CHANNEL.registerMessage(id++, ChangePagePacket.class,
                ChangePagePacket::encode, ChangePagePacket::decode, ChangePagePacket::handle);
        CHANNEL.registerMessage(id++, UpdateLoliSettingsPacket.class,
                UpdateLoliSettingsPacket::encode, UpdateLoliSettingsPacket::decode, UpdateLoliSettingsPacket::handle);
        CHANNEL.registerMessage(id++, UpdateEnchantmentsPacket.class,
                UpdateEnchantmentsPacket::encode, UpdateEnchantmentsPacket::decode, UpdateEnchantmentsPacket::handle);
        CHANNEL.registerMessage(id++, UpdatePotionsPacket.class,
                UpdatePotionsPacket::encode, UpdatePotionsPacket::decode, UpdatePotionsPacket::handle);
        CHANNEL.registerMessage(id++, SpaceFoldingPacket.class,
                SpaceFoldingPacket::encode, SpaceFoldingPacket::decode, SpaceFoldingPacket::handle);
        CHANNEL.registerMessage(id++, SafeAttackEffectPacket.class,
                SafeAttackEffectPacket::encode, SafeAttackEffectPacket::decode, SafeAttackEffectPacket::handle);
        CHANNEL.registerMessage(id++, UpdateCardUrlPacket.class,
                UpdateCardUrlPacket::encode, UpdateCardUrlPacket::decode, UpdateCardUrlPacket::handle);
        CHANNEL.registerMessage(id++, UpdatePasswordPacket.class,
                UpdatePasswordPacket::encode, UpdatePasswordPacket::decode, UpdatePasswordPacket::handle);
        CHANNEL.registerMessage(id++, KillFacingPacket.class,
                KillFacingPacket::encode, KillFacingPacket::decode, KillFacingPacket::handle);
        CHANNEL.registerMessage(id++, RefreshHeldItemPacket.class,
                RefreshHeldItemPacket::encode, RefreshHeldItemPacket::decode, RefreshHeldItemPacket::handle);
        CHANNEL.registerMessage(id++, ForceEntityCleanupPacket.class,
                ForceEntityCleanupPacket::encode, ForceEntityCleanupPacket::decode, ForceEntityCleanupPacket::handle);
    }

    private ModNetwork() {}
}
