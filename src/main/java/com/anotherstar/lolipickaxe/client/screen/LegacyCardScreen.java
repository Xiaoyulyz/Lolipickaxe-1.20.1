package com.anotherstar.lolipickaxe.client.screen;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

 
public final class LegacyCardScreen extends Screen {
    public record Page(ResourceLocation texture, int width, int height) {}
    private final List<Page> pages;
    private int page;
    private double scale = 1.0D;
    private double offsetX;
    private double offsetY;
    private boolean dragging;
    private double lastX;
    private double lastY;

    public LegacyCardScreen(List<Page> pages) {
        super(Component.empty());
        this.pages = pages;
    }

    @Override
    protected void init() {
        int y = height - 20;
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose())
                .bounds((width - 200) / 2, y, 200, 20).build());
        if (pages.size() > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(-1))
                    .bounds((width - 220) / 2, y, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(1))
                    .bounds((width + 200) / 2, y, 20, 20).build());
        }
    }

    private void changePage(int delta) {
        page = Math.floorMod(page + delta, pages.size());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta == 0) return super.mouseScrolled(mouseX, mouseY, delta);
        double old = scale;
        if (scale > 0.1D || delta > 0) scale *= delta > 0 ? 1.28D : 0.78125D;
        scale = Math.max(0.08D, Math.min(32.0D, scale));
        double factor = scale / old;
        offsetX = (width / 2.0D + offsetX - mouseX) * factor - (width / 2.0D - mouseX);
        offsetY = (height / 2.0D + offsetY - mouseY) * factor - (height / 2.0D - mouseY);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (!handled && button == 0) {
            dragging = true;
            lastX = mouseX;
            lastY = mouseY;
            return true;
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            offsetX += mouseX - lastX;
            offsetY += mouseY - lastY;
            lastX = mouseX;
            lastY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        if (!pages.isEmpty()) {
            Page current = pages.get(page);
            double fit = Math.min((double) width / current.width(), (double) height / current.height());
            float drawScale = (float) (fit * scale);
            int x = (int) ((width / 2.0D + offsetX) / drawScale - current.width() / 2.0D);
            int y = (int) ((height / 2.0D + offsetY) / drawScale - current.height() / 2.0D);
            graphics.pose().pushPose();
            graphics.pose().scale(drawScale, drawScale, 1.0F);
            RenderSystem.setShaderTexture(0, current.texture());
            graphics.blit(current.texture(), x, y, 0, 0, current.width(), current.height(), current.width(), current.height());
            graphics.pose().popPose();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public static ResourceLocation card(String file) {
        return new ResourceLocation(LoliPickaxe.MOD_ID, "lolicards/" + file);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
