package com.anotherstar.lolipickaxe.client.screen;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.network.ModNetwork;
import com.anotherstar.lolipickaxe.network.UpdateCardUrlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

public final class CardUrlConfigScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation(LoliPickaxe.MOD_ID, "textures/gui/loli_card_online_config.png");
    private final InteractionHand hand;
    private final String initialUrl;
    private EditBox urlField;

    public CardUrlConfigScreen(InteractionHand hand, String initialUrl) {
        super(Component.translatable("gui.loliCardOnline"));
        this.hand = hand;
        this.initialUrl = initialUrl;
    }

    @Override
    protected void init() {
        urlField = new EditBox(font, width / 2 - 80, height / 2 - 20, 160, 20, title);
        urlField.setMaxLength(500);
        urlField.setValue(initialUrl == null ? "" : initialUrl);
        addRenderableWidget(urlField);
        setInitialFocus(urlField);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> {
            ModNetwork.CHANNEL.sendToServer(new UpdateCardUrlPacket(hand, urlField.getValue()));
            onClose();
        }).bounds(width / 2 - 100, height / 2 + 10, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.blit(TEXTURE, (width - 220) / 2, (height - 100) / 2, 0, 0, 220, 90, 256, 256);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 40, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
