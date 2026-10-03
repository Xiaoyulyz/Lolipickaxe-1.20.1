package com.anotherstar.lolipickaxe.client.screen;

import com.anotherstar.lolipickaxe.LoliPickaxe;
import com.anotherstar.lolipickaxe.network.ModNetwork;
import com.anotherstar.lolipickaxe.network.SpaceFoldingPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

 
public final class LegacySpaceFoldingScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation(LoliPickaxe.MOD_ID, "textures/gui/loli_pickaxe_space_folding.png");
    private final List<ResourceKey<Level>> dimensions = new ArrayList<>();
    private ResourceKey<Level> selectedWorld;
    private final Map<ResourceKey<Level>, Button> worldButtons = new LinkedHashMap<>();
    private boolean relative;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;
    private Button absoluteButton;
    private Button relativeButton;
    private int rows;
    private int columns;
    private int dx;
    private int dy;

    public LegacySpaceFoldingScreen() { super(Component.empty()); }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        dimensions.clear();
        worldButtons.clear();
        if (mc.getConnection() != null) dimensions.addAll(mc.getConnection().levels());
        dimensions.sort(Comparator.comparing(key -> key.location().toString()));
        if (mc.level != null && !dimensions.contains(mc.level.dimension())) dimensions.add(mc.level.dimension());
        selectedWorld = mc.level == null ? (dimensions.isEmpty() ? Level.OVERWORLD : dimensions.get(0)) : mc.level.dimension();

        int count = Math.max(1, dimensions.size());
        rows = (count - 1) / 4 + 1;
        columns = Math.min(4, count);
        dx = 5 - columns * 35;
        dy = -25 - rows * 15;
        for (int index = 0; index < dimensions.size(); index++) {
            ResourceKey<Level> key = dimensions.get(index);
            Button button = Button.builder(Component.literal(worldName(key)), b -> selectWorld(key))
                    .bounds(width / 2 + dx + (index % columns) * 70, height / 2 + dy + (index / 4) * 30, 60, 20).build();
            button.active = !key.equals(selectedWorld);
            worldButtons.put(key, button);
            addRenderableWidget(button);
        }

        rows += 2;
        columns = 4;
        dx = -135;
        int fieldY = height / 2 + dy + rows * 30 - 60;
        xField = edit(width / 2 + dx, fieldY, Double.toString(mc.player == null ? 0 : mc.player.getX()));
        yField = edit(width / 2 + dx + 70, fieldY, Double.toString(mc.player == null ? 0 : mc.player.getY()));
        zField = edit(width / 2 + dx + 140, fieldY, Double.toString(mc.player == null ? 0 : mc.player.getZ()));
        xField.setFocused(true);

        absoluteButton = addRenderableWidget(Button.builder(Component.translatable("gui.loliAbsolute"), b -> setRelative(false))
                .bounds(width / 2 + dx, fieldY + 30, 60, 20).build());
        relativeButton = addRenderableWidget(Button.builder(Component.translatable("gui.loliRelative"), b -> setRelative(true))
                .bounds(width / 2 + dx + 70, fieldY + 30, 60, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> done())
                .bounds(width / 2 + dx + 210, fieldY, 60, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(width / 2 + dx + 140, fieldY + 30, 130, 20).build());
        updateModeButtons();
    }

    private EditBox edit(int x, int y, String value) {
        EditBox box = new EditBox(font, x, y, 60, 20, Component.empty());
        box.setMaxLength(100); box.setValue(value); addRenderableWidget(box); return box;
    }

    private void selectWorld(ResourceKey<Level> key) {
        selectedWorld = key;
        worldButtons.forEach((world, button) -> button.active = !world.equals(selectedWorld));
    }

    private void setRelative(boolean value) {
        if (relative == value) return;
        double px = minecraft != null && minecraft.player != null ? minecraft.player.getX() : 0;
        double py = minecraft != null && minecraft.player != null ? minecraft.player.getY() : 0;
        double pz = minecraft != null && minecraft.player != null ? minecraft.player.getZ() : 0;
        try {
            double x = Double.parseDouble(xField.getValue());
            double y = Double.parseDouble(yField.getValue());
            double z = Double.parseDouble(zField.getValue());
            if (value) { x -= px; y -= py; z -= pz; }
            else { x += px; y += py; z += pz; }
            xField.setValue(Double.toString(x)); yField.setValue(Double.toString(y)); zField.setValue(Double.toString(z));
        } catch (NumberFormatException ignored) {
            xField.setValue("0.0"); yField.setValue("0.0"); zField.setValue("0.0");
        }
        relative = value;
        updateModeButtons();
    }

    private void updateModeButtons() {
        if (absoluteButton != null) absoluteButton.active = relative;
        if (relativeButton != null) relativeButton.active = !relative;
    }

    private void done() {
        if (minecraft == null || minecraft.player == null) return;
        try {
            double x = Double.parseDouble(xField.getValue());
            double y = Double.parseDouble(yField.getValue());
            double z = Double.parseDouble(zField.getValue());
            if (!relative) { x -= minecraft.player.getX(); y -= minecraft.player.getY(); z -= minecraft.player.getZ(); }
            ModNetwork.CHANNEL.sendToServer(new SpaceFoldingPacket(selectedWorld.location(), x, y, z));
        } catch (NumberFormatException ignored) {}
        minecraft.setScreen(null);
    }

    private static String worldName(ResourceKey<Level> key) {
        String path = key.location().getPath();
        return path.length() > 9 ? path.substring(0, 9) : path;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int baseX = width / 2 + dx;
        int baseY = height / 2 + dy;
        graphics.blit(TEXTURE, baseX - 10, baseY - 10, 0, 0, 10, 10, 90, 50);
        graphics.blit(TEXTURE, baseX + 70 * columns - 10, baseY - 10, 80, 0, 10, 10, 90, 50);
        graphics.blit(TEXTURE, baseX - 10, baseY + 30 * rows - 10, 0, 40, 10, 10, 90, 50);
        graphics.blit(TEXTURE, baseX + 70 * columns - 10, baseY + 30 * rows - 10, 80, 40, 10, 10, 90, 50);
        for (int i = 0; i < columns - 1; i++) {
            graphics.blit(TEXTURE, baseX + 70 * i, baseY - 10, 10, 0, 70, 10, 90, 50);
            graphics.blit(TEXTURE, baseX + 70 * i, baseY + 30 * rows - 10, 10, 40, 70, 10, 90, 50);
        }
        graphics.blit(TEXTURE, baseX + 70 * columns - 70, baseY - 10, 10, 0, 60, 10, 90, 50);
        graphics.blit(TEXTURE, baseX + 70 * columns - 70, baseY + 30 * rows - 10, 10, 40, 60, 10, 90, 50);
        for (int i = 0; i < rows - 1; i++) {
            graphics.blit(TEXTURE, baseX - 10, baseY + 30 * i, 0, 10, 10, 30, 90, 50);
            graphics.blit(TEXTURE, baseX + 70 * columns - 10, baseY + 30 * i, 80, 10, 10, 30, 90, 50);
        }
        graphics.blit(TEXTURE, baseX - 10, baseY + 30 * rows - 30, 0, 10, 10, 20, 90, 50);
        graphics.blit(TEXTURE, baseX + 70 * columns - 10, baseY + 30 * rows - 30, 80, 10, 10, 20, 90, 50);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                graphics.blit(TEXTURE, baseX + 70 * column, baseY + 30 * row, 10, 10,
                        column == columns - 1 ? 60 : 70, row == rows - 1 ? 20 : 30, 90, 50);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
