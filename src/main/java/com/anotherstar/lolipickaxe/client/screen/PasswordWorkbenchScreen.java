package com.anotherstar.lolipickaxe.client.screen;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.menu.PasswordWorkbenchMenu;
import com.anotherstar.lolipickaxe.network.ModNetwork;
import com.anotherstar.lolipickaxe.network.UpdatePasswordPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

 
public final class PasswordWorkbenchScreen extends AbstractContainerScreen<PasswordWorkbenchMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            LoliPickaxe.MOD_ID, "textures/gui/container/password_crafting_table.png");
    private EditBox password;

    public PasswordWorkbenchScreen(PasswordWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 196;
    }

    @Override
    protected void init() {
        super.init();
        password = new EditBox(font, leftPos + 29, topPos + 18, 75, 16,
                Component.translatable("gui.password"));
        password.setTextColor(0xFFFFFF);
        addRenderableWidget(password);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE,
                        button -> ModNetwork.CHANNEL.sendToServer(new UpdatePasswordPacket(password.getValue())))
                .bounds(leftPos + 114, topPos + 16, 30, 20).build());
        setInitialFocus(password);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        password.tick();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("gui.password"), 28, 6, 4210752, false);
        graphics.drawString(font, Component.translatable("container.crafting"), 28, 36, 4210752, false);
        graphics.drawString(font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
