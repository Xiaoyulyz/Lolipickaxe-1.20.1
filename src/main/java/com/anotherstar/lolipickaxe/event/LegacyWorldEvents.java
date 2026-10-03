package com.anotherstar.lolipickaxe.event;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.config.LoliConfig;
import com.anotherstar.lolipickaxe.registry.ModItems;
import com.anotherstar.lolipickaxe.util.LegacyTextComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

 
@Mod.EventBusSubscriber(modid = LoliPickaxe.MOD_ID)
public final class LegacyWorldEvents {
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var entity = event.getEntity();
        var random = entity.getRandom();
        if (random.nextDouble() < LoliConfig.CARD_DROP_PROBABILITY.get()) {
            add(event, new ItemStack(ModItems.LOLI_CARD.get()));
        }
        if (random.nextDouble() < LoliConfig.CARD_ALBUM_DROP_PROBABILITY.get()) {
            add(event, new ItemStack(ModItems.LOLI_CARD_ALBUM.get()));
        }
        if (entity instanceof Creeper && random.nextDouble() < LoliConfig.RECORD_DROP_PROBABILITY.get()) {
            add(event, new ItemStack(ModItems.LOLI_RECORD.get()));
        }
        if (random.nextDouble() < LoliConfig.ENTITY_SOUL_DROP_PROBABILITY.get()) {
            add(event, new ItemStack(ModItems.LOLI_ENTITY_SOUL_ADDON.get()));
        }
    }

    private static void add(LivingDropsEvent event, ItemStack stack) {
        var entity = event.getEntity();
        event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack));
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        MutableComponent prefix = LegacyTextComponents.literal("LoliPickaxe")
                .withStyle(style -> LegacyTextComponents.normalStyle(style)
                        .withColor(ChatFormatting.DARK_GREEN))
                .append(LegacyTextComponents.literal("开源地址: ")
                        .withStyle(style -> LegacyTextComponents.normalStyle(style)
                                .withColor(ChatFormatting.WHITE)));

        MutableComponent link = LegacyTextComponents.literal("https://github.com/IslenautsGK/LoliPickaxe")
                .withStyle(style -> LegacyTextComponents.normalStyle(style)
                        .withColor(ChatFormatting.BLUE)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                "https://github.com/IslenautsGK/LoliPickaxe"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                LegacyTextComponents.literal("点击前往链接"))));
        event.getEntity().sendSystemMessage(prefix.append(link));
    }

    private LegacyWorldEvents() {
    }
}
