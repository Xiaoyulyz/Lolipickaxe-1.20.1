package com.anotherstar.lolipickaxe.client.screen;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.menu.LoliPickaxeMenu;
import com.anotherstar.lolipickaxe.network.ChangePagePacket;
import com.anotherstar.lolipickaxe.network.ModNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class LoliPickaxeScreen extends AbstractContainerScreen<LoliPickaxeMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(LoliPickaxe.MOD_ID, "textures/gui/container/loli_pickaxe_container.png");

    public LoliPickaxeScreen(LoliPickaxeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 240;
        imageHeight = 256;
        inventoryLabelY = 164;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), button -> ModNetwork.CHANNEL.sendToServer(new ChangePagePacket(-1)))
                .bounds(leftPos + 173, topPos + 22, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> ModNetwork.CHANNEL.sendToServer(new ChangePagePacket(1)))
                .bounds(leftPos + 213, topPos + 22, 20, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        String page = Integer.toString(menu.getPage() + 1);
        graphics.drawCenteredString(font, page, 203, 27, 0x404040);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
