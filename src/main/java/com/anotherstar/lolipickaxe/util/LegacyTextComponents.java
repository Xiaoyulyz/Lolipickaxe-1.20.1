package com.anotherstar.lolipickaxe.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;






public final class LegacyTextComponents {
    public static MutableComponent translatable(String key, Object... arguments) {
        return normalize(Component.translatable(key, arguments));
    }

    public static MutableComponent literal(String text) {
        return normalize(Component.literal(text));
    }

    public static MutableComponent normalize(Component component) {
        return component.copy().withStyle(LegacyTextComponents::normalStyle);
    }

    public static Style normalStyle(Style style) {
        return style.withBold(false)
                .withItalic(false)
                .withUnderlined(false)
                .withStrikethrough(false)
                .withObfuscated(false)
                .withFont(Style.DEFAULT_FONT);
    }

    private LegacyTextComponents() {
    }
}
