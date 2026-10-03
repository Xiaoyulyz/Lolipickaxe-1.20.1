package com.anotherstar.lolipickaxe.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

 
public abstract class LegacyRegistryEditorScreen extends Screen {
    public record Entry(ResourceLocation id, int level) {}
    private static final int VISIBLE = 12;
    protected final ItemStack stack;
    protected final List<ResourceLocation> available;
    protected final List<Entry> selected;
    private final Button[] availableButtons = new Button[VISIBLE];
    private final Button[] selectedButtons = new Button[VISIBLE];
    private EditBox levelField;
    private Button remove;
    private int availableOffset;
    private int selectedOffset;
    private int availableSelection;
    private int selectedSelection;

    protected LegacyRegistryEditorScreen(ItemStack stack, Component title) {
        super(title);
        this.stack = stack.copy();
        this.available = new ArrayList<>(allRegistryIds());
        this.available.sort(java.util.Comparator.comparing(ResourceLocation::toString));
        this.selected = new ArrayList<>(readSelected(this.stack));
    }

    protected abstract List<ResourceLocation> allRegistryIds();
    protected abstract List<Entry> readSelected(ItemStack stack);
    protected abstract Component display(ResourceLocation id, int level, boolean selectedList);
    protected abstract int defaultLevel();
    protected abstract void save(List<Entry> entries);
    protected abstract Component editorLabel();

    @Override
    protected void init() {
        int left = width / 2 - 160;
        int top = height / 2 - 115;
        for (int i = 0; i < VISIBLE; i++) {
            final int row = i;
            availableButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> selectAvailable(row))
                    .bounds(left, top + i * 15, 100, 15).build());
            selectedButtons[i] = addRenderableWidget(Button.builder(Component.empty(), b -> selectSelected(row))
                    .bounds(width / 2 - 50, top + i * 15, 100, 15).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.loliAdd"), b -> addEntry())
                .bounds(width / 2 + 60, height / 2 - 95, 100, 20).build());
        remove = addRenderableWidget(Button.builder(Component.translatable("gui.loliRemove"), b -> removeEntry())
                .bounds(width / 2 + 60, height / 2 - 65, 100, 20).build());
        levelField = new EditBox(font, width / 2 + 60, height / 2 - 35, 100, 20, Component.empty());
        levelField.setValue(Integer.toString(defaultLevel()));
        levelField.setMaxLength(11);
        addRenderableWidget(levelField);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> done())
                .bounds(width / 2 - 160, height / 2 + 95, 320, 20).build());
        refresh();
    }

    private void selectAvailable(int row) {
        int index = availableOffset + row;
        if (index < available.size()) availableSelection = index;
        refresh();
    }

    private void selectSelected(int row) {
        int index = selectedOffset + row;
        if (index < selected.size()) selectedSelection = index;
        refresh();
    }

    private int typedLevel() {
        try { return Integer.parseInt(levelField.getValue()); }
        catch (Exception ignored) { return defaultLevel(); }
    }

    private void addEntry() {
        if (available.isEmpty()) return;
        availableSelection = Math.max(0, Math.min(availableSelection, available.size() - 1));
        selected.add(new Entry(available.get(availableSelection), typedLevel()));
        selectedSelection = selected.size() - 1;
        selectedOffset = Math.max(0, selected.size() - VISIBLE);
        refresh();
    }

    private void removeEntry() {
        if (selected.isEmpty()) return;
        selectedSelection = Math.max(0, Math.min(selectedSelection, selected.size() - 1));
        selected.remove(selectedSelection);
        if (selectedSelection >= selected.size()) selectedSelection = selected.size() - 1;
        selectedOffset = Math.max(0, Math.min(selectedOffset, Math.max(0, selected.size() - VISIBLE)));
        refresh();
    }

    private void done() {
        save(List.copyOf(selected));
        if (minecraft != null) minecraft.setScreen(null);
    }

    private void refresh() {
        availableOffset = Math.max(0, Math.min(availableOffset, Math.max(0, available.size() - VISIBLE)));
        selectedOffset = Math.max(0, Math.min(selectedOffset, Math.max(0, selected.size() - VISIBLE)));
        for (int row = 0; row < VISIBLE; row++) {
            int index = availableOffset + row;
            Button button = availableButtons[row];
            button.visible = index < available.size();
            button.active = button.visible;
            if (button.visible) {
                Component name = display(available.get(index), 0, false);
                button.setMessage(index == availableSelection ? Component.literal("> ").append(name) : name);
            }
            int selectedIndex = selectedOffset + row;
            Button selectedButton = selectedButtons[row];
            selectedButton.visible = selectedIndex < selected.size();
            selectedButton.active = selectedButton.visible;
            if (selectedButton.visible) {
                Entry entry = selected.get(selectedIndex);
                Component name = display(entry.id(), entry.level(), true);
                selectedButton.setMessage(selectedIndex == selectedSelection ? Component.literal("> ").append(name) : name);
            }
        }
        if (remove != null) remove.active = !selected.isEmpty();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int top = height / 2 - 115;
        if (mouseY >= top && mouseY < top + 200) {
            if (mouseX >= width / 2 - 160 && mouseX < width / 2 - 60) {
                availableOffset -= (int) Math.signum(delta);
                refresh();
                return true;
            }
            if (mouseX >= width / 2 - 50 && mouseX < width / 2 + 50) {
                selectedOffset -= (int) Math.signum(delta);
                refresh();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawString(font, editorLabel(), width / 2 + 60, height / 2 - 110, 0xFFFFFF, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
