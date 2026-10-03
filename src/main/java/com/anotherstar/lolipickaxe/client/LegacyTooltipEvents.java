package com.anotherstar.lolipickaxe.client;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.item.CardItem;
import com.anotherstar.lolipickaxe.registry.ModItems;
import com.anotherstar.lolipickaxe.util.LegacyCardNameCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber(modid = LoliPickaxe.MOD_ID, value = Dist.CLIENT)
public final class LegacyTooltipEvents {
    private static final Pattern LEVEL = Pattern.compile("enchantment\\.level\\.(\\d+)$");

    





    private static final ChatFormatting[] RAINBOW = {
            ChatFormatting.RED,
            ChatFormatting.GOLD,
            ChatFormatting.YELLOW,
            ChatFormatting.GREEN,
            ChatFormatting.AQUA,
            ChatFormatting.BLUE,
            ChatFormatting.LIGHT_PURPLE
    };

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        List<Component> tooltip = event.getToolTip();
        if (event.getItemStack().getItem() instanceof CardItem) {
            for (int i = 0; i < tooltip.size(); i++) {
                Component line = tooltip.get(i);
                String raw = line.getString();
                if (LegacyCardNameCodec.containsEncodedText(raw)) {
                    tooltip.set(i, Component.literal(LegacyCardNameCodec.decode(raw)).withStyle(line.getStyle()));
                }
            }
        }

        boolean premium = event.getItemStack().is(ModItems.LOLI_PICKAXE.get());
        boolean small = event.getItemStack().is(ModItems.SMALL_LOLI_PICKAXE.get());
        if (!premium && !small) return;

        String damageName = Component.translatable("attribute.name.generic.attack_damage").getString();
        String speedName = Component.translatable("attribute.name.generic.attack_speed").getString();
        for (int i = 0; i < tooltip.size(); i++) {
            String plain = tooltip.get(i).getString();
            if (premium && plain.endsWith(damageName)) {
                tooltip.set(i, legacyAttributeLine("loliPickaxe.damage", "attribute.name.generic.attack_damage", 0));
            } else if (premium && plain.endsWith(speedName)) {
                tooltip.set(i, legacyAttributeLine("loliPickaxe.speed", "attribute.name.generic.attack_speed", 0));
            } else {
                Matcher matcher = LEVEL.matcher(plain);
                if (matcher.find()) {
                    try {
                        tooltip.set(i, Component.literal(plain.substring(0, matcher.start())
                                        + roman(Integer.parseInt(matcher.group(1))))
                                .withStyle(tooltip.get(i).getStyle()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
    }

    private static Component legacyAttributeLine(String valueKey, String attributeKey, int offset) {
        MutableComponent rainbow = getRainbowText(Component.translatable(valueKey).getString(), offset);
        return Component.literal(" ").append(Component.translatable("attribute.modifier.equals.0",
                rainbow,
                Component.translatable(attributeKey)).withStyle(ChatFormatting.GRAY));
    }

    private static MutableComponent getRainbowText(String text, int offset) {
        MutableComponent builder = Component.empty();
        long time = System.currentTimeMillis() / 75L;
        for (int i = 0; i < text.length(); i++) {
            int colorIndex = Math.floorMod((int) (time - i + offset), RAINBOW.length);
            builder.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(RAINBOW[colorIndex]));
        }
        return builder;
    }

    private static String roman(int value) {
        if (value <= 0) return Integer.toString(value);
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (value >= values[i]) {
                value -= values[i];
                out.append(numerals[i]);
            }
        }
        return out.toString();
    }

    private LegacyTooltipEvents() {
    }
}
