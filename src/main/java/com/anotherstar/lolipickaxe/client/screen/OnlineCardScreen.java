package com.anotherstar.lolipickaxe.client.screen;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

 
public final class OnlineCardScreen extends Screen {
    private final String url;
    private ResourceLocation texture;
    private int imageWidth = 1;
    private int imageHeight = 1;
    private volatile String error;
    private double scale = 1.0D;
    private double offsetX;
    private double offsetY;
    private boolean dragging;
    private double lastX;
    private double lastY;

    public OnlineCardScreen(String url) {
        super(Component.empty());
        this.url = url == null ? "" : url;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose())
                .bounds((width - 200) / 2, height - 20, 200, 20).build());
        if (texture == null && error == null && !url.isBlank()) load();
    }

    private void load() {
        CompletableFuture.runAsync(() -> {
            try (InputStream stream = new URL(url).openStream()) {
                NativeImage image = NativeImage.read(stream);
                int w = image.getWidth();
                int h = image.getHeight();
                Minecraft.getInstance().execute(() -> {
                    imageWidth = w;
                    imageHeight = h;
                    texture = new ResourceLocation(LoliPickaxe.MOD_ID, "online_card/" + Integer.toUnsignedString(url.hashCode()));
                    Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(image));
                });
            } catch (Exception ex) {
                error = ex.getClass().getSimpleName();
            }
        });
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
        if (texture != null) {
            double fit = Math.min((double) width / imageWidth, (double) height / imageHeight);
            float drawScale = (float) (fit * scale);
            int x = (int) ((width / 2.0D + offsetX) / drawScale - imageWidth / 2.0D);
            int y = (int) ((height / 2.0D + offsetY) / drawScale - imageHeight / 2.0D);
            graphics.pose().pushPose();
            graphics.pose().scale(drawScale, drawScale, 1.0F);
            RenderSystem.setShaderTexture(0, texture);
            graphics.blit(texture, x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
            graphics.pose().popPose();
        } else {
            Component message = error == null ? Component.literal(url.isBlank() ? "" : "Loading...") : Component.literal(error);
            graphics.drawCenteredString(font, message, width / 2, height / 2, 0xFFFFFF);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
