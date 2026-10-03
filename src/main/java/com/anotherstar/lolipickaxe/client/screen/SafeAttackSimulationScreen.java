package com.anotherstar.lolipickaxe.client.screen;

import com.anotherstar.lolipickaxe.network.SafeAttackEffect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SafeAttackSimulationScreen extends Screen {
    private final SafeAttackEffect effect;
    private int ticks;

    public SafeAttackSimulationScreen(SafeAttackEffect effect) {
        super(Component.translatable(switch (effect) {
            case BLUE_SCREEN -> "loliPickaxe.blueScreenAttack";
            case EXIT -> "loliPickaxe.exitAttack";
            case NOT_RESPONDING -> "loliPickaxe.failRespondAttack";
        }));
        this.effect = effect;
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks > 160 && minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int background = switch (effect) {
            case BLUE_SCREEN -> 0xFF0078D7;
            case EXIT -> 0xFF000000;
            case NOT_RESPONDING -> 0xFFF0F0F0;
        };
        int foreground = effect == SafeAttackEffect.NOT_RESPONDING ? 0xFF202020 : 0xFFFFFFFF;
        graphics.fill(0, 0, width, height, background);

        int cx = width / 2;
        int y = Math.max(40, height / 3);
        if (effect == SafeAttackEffect.BLUE_SCREEN) {
            graphics.drawCenteredString(font, Component.literal(":("), cx, y - 48, foreground);
            graphics.drawCenteredString(font, Component.literal("你的电脑遇到问题，需要重新启动。"), cx, y + 2, foreground);
            graphics.drawCenteredString(font, Component.literal("我们只收集某些错误信息，然后将为你重新启动。"), cx, y + 22, foreground);
        } else if (effect == SafeAttackEffect.EXIT) {
            graphics.drawCenteredString(font, title, cx, y, foreground);
        } else {
            graphics.drawCenteredString(font, Component.literal("Minecraft 未响应"), cx, y, foreground);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
