package com.anotherstar.lolipickaxe.client.screen;

import com.anotherstar.lolipickaxe.client.render.LoliUiShaders;
import com.anotherstar.lolipickaxe.config.LegacyGuiConfig;
import com.anotherstar.lolipickaxe.config.LegacyLoliSettings;
import com.anotherstar.lolipickaxe.network.ModNetwork;
import com.anotherstar.lolipickaxe.network.UpdateLoliSettingsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;








public final class LegacyLoliConfigScreen extends Screen {
    private static final float REF_PANEL_WIDTH_RATIO = 0.610F;    
    private static final float REF_PANEL_HEIGHT_RATIO = 0.629F;   
    private static final float REF_HEADER_RATIO = 0.139F;
    private static final float REF_SIDEBAR_RATIO = 0.224F;
    private static final float REF_SEARCH_TOP_RATIO = 0.169F;
    private static final float REF_LIST_TOP_RATIO = 0.292F;

    private static final int TEXT_PRIMARY = 0xFFF5F6F8;
    private static final int TEXT_SECONDARY = 0xFFD8DCE3;
    private static final int TEXT_MUTED = 0xFFAEB5BF;
    private static final int KNOB = 0xFFF8F8FA;

     
    private static final double SLIDER_SOFT_SNAP_RADIUS = 0.050D;
    private static final double SLIDER_RELEASE_SNAP_RADIUS = 0.018D;
    private static final double SLIDER_SOFT_SNAP_PULL = 0.62D;

    private static final ResourceLocation UI_FONT = new ResourceLocation("lolipickaxe", "ui");
    private static final ResourceLocation UI_FONT_BOLD = new ResourceLocation("lolipickaxe", "ui_bold");

    private final ItemStack stack;
    private final CompoundTag settings;
    private final List<RowLayout> rows = new ArrayList<>();
    private final Map<String, Float> switchAnimations = new HashMap<>();
    private final Map<String, Float> sliderAnimations = new HashMap<>();

    private Category category = Category.BASIC;
    private String searchText = "";
    private boolean searchFocused;
    private LegacyGuiConfig.Option textFocused;
    private LegacyGuiConfig.Option draggingSlider;
    private LegacyGuiConfig.Option keyboardSlider;
    private double scrollAmount;
    private int maxScroll;
    private boolean dirty;
    private long savedFeedbackUntil;
    private long resetConfirmUntil;
    private long resetFeedbackUntil;
    private long lastFrameNanos = System.nanoTime();

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int headerHeight;
    private int sidebarWidth;
    private int contentX;
    private int contentRight;
    private int searchY;
    private int listTop;
    private int listBottom;

    public LegacyLoliConfigScreen(ItemStack stack) {
        super(Component.literal("氪金萝莉 · 设置"));
        this.stack = stack.copy();
        CompoundTag source = LegacyLoliSettings.itemConfig(this.stack);
        this.settings = source == null ? new CompoundTag() : source.copy();
    }

    @Override
    protected void init() {
        updateGeometry();
        rebuildRows();
    }

    private void updateGeometry() {
        panelWidth = Math.min(Math.max(1, width - 12), Math.max(220, Math.round(width * REF_PANEL_WIDTH_RATIO)));
        panelHeight = Math.min(Math.max(1, height - 12), Math.max(210, Math.round(height * REF_PANEL_HEIGHT_RATIO)));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        headerHeight = Math.max(34, Math.round(panelHeight * REF_HEADER_RATIO));
        sidebarWidth = Math.max(72, Math.round(panelWidth * REF_SIDEBAR_RATIO));
        contentX = panelX + sidebarWidth + Math.max(8, panelWidth / 44);
        contentRight = panelX + panelWidth - Math.max(10, panelWidth / 31);
        searchY = panelY + Math.round(panelHeight * REF_SEARCH_TOP_RATIO);
        listTop = panelY + Math.round(panelHeight * REF_LIST_TOP_RATIO);
        listBottom = panelY + panelHeight - Math.max(10, panelHeight / 25);
    }

    private void rebuildRows() {
        rows.clear();

        String needle = searchText.trim().toLowerCase(Locale.ROOT);

        List<LegacyGuiConfig.Option> visible = new ArrayList<>();
        for (LegacyGuiConfig.Option option : LegacyGuiConfig.OPTIONS) {
            if (categoryFor(option) != category) continue;
            if (!needle.isEmpty()) {
                String haystack = (option.comment() + " " + option.key()).toLowerCase(Locale.ROOT);
                if (!haystack.contains(needle)) continue;
            }
            visible.add(option);
        }

         
         
        visible.sort(Comparator.comparingInt(this::visualPriority));

        int y = listTop + 18;
        for (LegacyGuiConfig.Option option : visible) {
            int rowHeight = switch (option.type()) {
                case INT, DOUBLE -> 42;
                case BOOLEAN -> 28;
                case STRING -> 44;
            };
            rows.add(new RowLayout(option, y, rowHeight));
            y += rowHeight + 2;
        }
        int viewport = Math.max(1, listBottom - (listTop + 18));
        maxScroll = Math.max(0, y - (listTop + 18) - viewport);
        scrollAmount = Mth.clamp(scrollAmount, 0.0D, maxScroll);
    }

    private int visualPriority(LegacyGuiConfig.Option option) {
        return switch (option.type()) {
            case INT, DOUBLE -> 0;
            case BOOLEAN -> 1;
            case STRING -> 2;
        };
    }

    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        updateGeometry();
        rebuildRows();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateGeometry();
        updateAnimations(animationDelta());

        drawBackdrop(graphics);
        drawPanel(graphics);
        drawHeader(graphics, mouseX, mouseY);
        drawSidebar(graphics, mouseX, mouseY);
        drawSearch(graphics, mouseX, mouseY);
        drawList(graphics, mouseX, mouseY);
    }

    private float animationDelta() {
        long now = System.nanoTime();
        float dt = (now - lastFrameNanos) / 1_000_000_000.0F;
        lastFrameNanos = now;
        return Mth.clamp(dt, 0.0F, 0.05F);
    }

    private void updateAnimations(float dt) {
        float toggleAmount = Mth.clamp(dt * 12.0F, 0.0F, 1.0F);
        float sliderAmount = Mth.clamp(dt * 10.0F, 0.0F, 1.0F);
        for (LegacyGuiConfig.Option option : LegacyGuiConfig.OPTIONS) {
            if (option.type() == LegacyGuiConfig.Type.BOOLEAN) {
                float target = currentBoolean(option) ? 1.0F : 0.0F;
                float current = switchAnimations.getOrDefault(option.key(), target);
                switchAnimations.put(option.key(), current + (target - current) * toggleAmount);
            } else if (option.type() == LegacyGuiConfig.Type.INT || option.type() == LegacyGuiConfig.Type.DOUBLE) {
                float target = (float) numericFraction(option);
                float current = sliderAnimations.getOrDefault(option.key(), target);
                sliderAnimations.put(option.key(), current + (target - current) * sliderAmount);
            }
        }
    }

    private void drawBackdrop(GuiGraphics graphics) {
        graphics.fill(0, 0, width, height, minecraft != null && minecraft.level != null ? 0x18FFFFFF : 0xFFF4F5F6);
    }

    private void drawPanel(GuiGraphics graphics) {
        int radius = Math.max(12, panelHeight / 17);
        rounded(graphics, panelX, panelY, panelWidth, panelHeight, radius,
                0xF4FBFBFC, 1.0F, 0xD9FFFFFF);
        graphics.fill(panelX + 12, panelY + headerHeight, panelX + panelWidth - 12,
                panelY + headerHeight + 1, dividerColor());
    }

    private void drawHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        drawText(graphics, "氪金萝莉 · 设置", panelX + 14, panelY + 12, TEXT_PRIMARY, true);

        int diameter = Math.max(20, panelHeight / 14);
        int buttonH = Math.max(18, diameter - 2);
        int saveW = 46;
        int resetW = 46;
        int headerGap = 6;
        int saveX = panelX + panelWidth - 14 - diameter - 8 - saveW;
        int resetX = saveX - headerGap - resetW;
        int buttonY = panelY + 10;
        long now = System.currentTimeMillis();

        boolean resetHover = inside(mouseX, mouseY, resetX, buttonY, resetW, buttonH);
        boolean resetConfirm = now < resetConfirmUntil;
        boolean justReset = now < resetFeedbackUntil;
        if (resetConfirm) {
            activeAccent(graphics, resetX, buttonY, resetW, buttonH, buttonH / 2.0F, resetHover ? 1.0F : 0.88F, 0.7F, 0x2AFFFFFF);
        } else {
            rounded(graphics, resetX, buttonY, resetW, buttonH, buttonH / 2.0F,
                    resetHover ? 0x26FFFFFF : 0x14FFFFFF, 0.7F, 0x22FFFFFF);
        }
        String resetLabel = justReset ? "已重置" : (resetConfirm ? "确认" : "重置");
        drawText(graphics, resetLabel, resetX + (resetW - textWidth(resetLabel, false)) / 2,
                buttonY + (buttonH - 9) / 2, resetConfirm ? 0xFFFFFFFF : 0xDCE8ECF3, false);

        boolean saveHover = inside(mouseX, mouseY, saveX, buttonY, saveW, buttonH);
        boolean justSaved = now < savedFeedbackUntil;
        if (dirty) {
            activeAccent(graphics, saveX, buttonY, saveW, buttonH, buttonH / 2.0F, saveHover ? 1.0F : 0.90F, 0.7F, 0x2AFFFFFF);
        } else {
            rounded(graphics, saveX, buttonY, saveW, buttonH, buttonH / 2.0F, saveHover ? 0x24FFFFFF : 0x14FFFFFF, 0.7F, 0x22FFFFFF);
        }
        String saveLabel = justSaved && !dirty ? "已保存" : "保存";
        drawText(graphics, saveLabel, saveX + (saveW - textWidth(saveLabel, false)) / 2,
                buttonY + (buttonH - 9) / 2, dirty ? 0xFFFFFFFF : 0xDCE8ECF3, false);

        int cx = panelX + panelWidth - 14 - diameter / 2;
        int cy = panelY + 10 + diameter / 2;
        boolean hover = distanceSquared(mouseX, mouseY, cx, cy) <= (diameter / 2.0) * (diameter / 2.0);
        rounded(graphics, cx - diameter / 2, cy - diameter / 2, diameter, diameter, diameter / 2.0F,
                hover ? 0x38FFFFFF : 0x22FFFFFF, 0.7F, 0x18FFFFFF);
        String x = "×";
        drawText(graphics, x, cx - textWidth(x, false) / 2, cy - 5, hover ? 0xFFFFFFFF : 0xE4FFFFFF, false);
    }

    private void drawSidebar(GuiGraphics graphics, int mouseX, int mouseY) {
        int dividerX = panelX + sidebarWidth;
        graphics.fill(dividerX, panelY + headerHeight + 8, dividerX + 1, panelY + panelHeight - 10, dividerColor());

        int tabX = panelX + 10;
        int tabW = sidebarWidth - 20;
        int tabH = 25;
        int gap = 16;
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            Category item = categories[i];
            int y = searchY + i * (tabH + gap);
            boolean hovered = inside(mouseX, mouseY, tabX, y, tabW, tabH);
            if (item == category) {
                rounded(graphics, tabX, y, tabW, tabH, 9.0F, selectedTabFill(), 0.7F, controlBorderColor());
            } else if (hovered) {
                rounded(graphics, tabX, y, tabW, tabH, 9.0F, hoverTabFill(), 0.0F, 0);
            }
            drawText(graphics, item.label, tabX + 12, y + 8,
                    item == category ? TEXT_PRIMARY : TEXT_SECONDARY, item == category);
        }
    }

    private void drawSearch(GuiGraphics graphics, int mouseX, int mouseY) {
        int h = Math.max(20, panelHeight / 13);
        int w = Math.max(70, contentRight - contentX);

        boolean hovered = inside(mouseX, mouseY, contentX, searchY, w, h);
        rounded(graphics, contentX, searchY, w, h, h / 2.0F,
                searchFill(searchFocused, hovered),
                0.7F, searchFocused ? searchFocusBorder() : controlBorderColor());

        int icon = 10;
        int ix = contentX + 10;
        int iy = searchY + (h - icon) / 2;
        rounded(graphics, ix, iy, icon, icon, icon / 2.0F, 0x00000000, 1.0F, 0x88747D88);
        graphics.fill(ix + 8, iy + 8, ix + 12, iy + 9, 0x88747D88);

        String value = searchText.isEmpty() ? "搜索设置" : searchText;
        int color = searchText.isEmpty() ? 0x8C7F8791 : 0xD0545B64;
        drawText(graphics, ellipsize(value, w - 36, false), contentX + 25, searchY + (h - 9) / 2, color, false);
        if (searchFocused && ((System.currentTimeMillis() / 500L) & 1L) == 0L) {
            int caret = Math.min(contentRight - 8, contentX + 25 + textWidth(searchText, false) + 1);
            graphics.fill(caret, searchY + 5, caret + 1, searchY + h - 5, 0xB858606A);
        }
    }

    private void drawList(GuiGraphics graphics, int mouseX, int mouseY) {

        String sectionTitle = searchText.isEmpty() ? category.section : "搜索结果";
        drawText(graphics, sectionTitle, contentX + 1, listTop, TEXT_PRIMARY, true);
        graphics.fill(contentX, listTop + 13, contentRight, listTop + 14, dividerColor());

        graphics.enableScissor(contentX - 2, listTop + 15, contentRight + 5, listBottom);
        int scroll = Mth.floor(scrollAmount);
        for (RowLayout row : rows) {
            int y = row.baseY - scroll;
            row.renderY = y;
            row.visible = y + row.height >= listTop + 15 && y <= listBottom;
            if (!row.visible) continue;
            drawOption(graphics, row.option, y, row.height, mouseX, mouseY);
        }
        graphics.disableScissor();

        drawScrollbar(graphics);
        if (rows.isEmpty()) drawText(graphics, "没有匹配的设置", contentX + 1, listTop + 34, TEXT_MUTED, false);
    }

    private int controlTickColor() {
        return 0x40323840;
    }

    private void drawOption(GuiGraphics graphics, LegacyGuiConfig.Option option, int y, int rowHeight, int mouseX, int mouseY) {
        switch (option.type()) {
            case BOOLEAN -> drawBoolean(graphics, option, y, mouseX, mouseY);
            case INT, DOUBLE -> drawNumeric(graphics, option, y, mouseX, mouseY);
            case STRING -> drawStringOption(graphics, option, y, mouseX, mouseY);
        }
        if (y + rowHeight < listBottom) {
            graphics.fill(contentX + 1, y + rowHeight, contentRight - 1, y + rowHeight + 1, rowDividerColor());
        }
    }

    private void drawBoolean(GuiGraphics graphics, LegacyGuiConfig.Option option, int y, int mouseX, int mouseY) {
        int switchW = 40;
        int switchH = 20;
        int sx = contentRight - switchW;
        int sy = y + 4;
        int labelMax = sx - contentX - 10;
        drawText(graphics, ellipsize(option.comment(), labelMax, false), contentX + 1, y + 9, TEXT_PRIMARY, false);

        boolean hovered = inside(mouseX, mouseY, sx - 3, sy - 3, switchW + 6, switchH + 6);
        float p = switchAnimations.getOrDefault(option.key(), currentBoolean(option) ? 1.0F : 0.0F);
        rounded(graphics, sx, sy, switchW, switchH, switchH / 2.0F, switchOffFill(), 0.7F, controlBorderColor());
        if (p > 0.001F) {
            activeAccent(graphics, sx, sy, switchW, switchH, switchH / 2.0F, p * (hovered ? 1.0F : 0.94F), 0.7F, 0x20FFFFFF);
        }

        float knobX = Mth.lerp(p, sx + 10.0F, sx + switchW - 10.0F);
        rounded(graphics, knobX - 7.5F, sy + 2.5F, 15.0F, 15.0F, 7.5F, KNOB, 0.6F, 0x28FFFFFF);
    }

    private void drawNumeric(GuiGraphics graphics, LegacyGuiConfig.Option option, int y, int mouseX, int mouseY) {
        String value = formattedValue(option);
        int valueWidth = textWidth(value, false);
        drawText(graphics, ellipsize(option.comment(), Math.max(40, contentRight - contentX - valueWidth - 14), false),
                contentX + 1, y + 4, TEXT_PRIMARY, false);
        drawText(graphics, value, contentRight - valueWidth, y + 4, TEXT_SECONDARY, false);

        int x1 = contentX + 5;
        int x2 = contentRight - 5;
        int sliderY = y + 25;
        graphics.fill(x1, sliderY, x2, sliderY + 1, sliderTrackColor());
        float p = sliderAnimations.getOrDefault(option.key(), (float) numericFraction(option));
        for (int i = 0; i <= 3; i++) {
            float tickFraction = i / 3.0F;
            int tx = x1 + Math.round((x2 - x1) * tickFraction);
            float distance = Math.abs(p - tickFraction);
            float magneticGlow = 1.0F - Mth.clamp(distance / (float) SLIDER_SOFT_SNAP_RADIUS, 0.0F, 1.0F);
            int alpha = 0x30 + Math.round(magneticGlow * 0x34);
            graphics.fill(tx, sliderY - 2, tx + 1, sliderY + 3, alpha << 24 | 0x00323840);
        }

        int knobX = x1 + Math.round((x2 - x1) * p);
        if (knobX > x1) activeAccent(graphics, x1, sliderY - 0.5F, knobX - x1, 2.0F, 1.0F, 0.98F, 0.0F, 0);
        boolean hovered = distanceSquared(mouseX, mouseY, knobX, sliderY) <= 9 * 9
                || inside(mouseX, mouseY, x1, sliderY - 5, x2 - x1, 10);
        float r = hovered || option == draggingSlider || option == keyboardSlider ? 8.0F : 7.0F;
        rounded(graphics, knobX - r, sliderY - r + 0.5F, r * 2.0F, r * 2.0F, r, KNOB, 0.6F, 0x24FFFFFF);
    }

    private void drawStringOption(GuiGraphics graphics, LegacyGuiConfig.Option option, int y, int mouseX, int mouseY) {
        drawText(graphics, ellipsize(option.comment(), contentRight - contentX, false), contentX + 1, y + 4, TEXT_PRIMARY, false);
        int fieldY = y + 19;
        int h = 19;
        boolean focused = textFocused == option;
        boolean hovered = inside(mouseX, mouseY, contentX, fieldY, contentRight - contentX, h);
        rounded(graphics, contentX, fieldY, contentRight - contentX, h, 7.0F,
                fieldFill(focused, hovered),
                0.7F, focused ? searchFocusBorder() : controlBorderColor());
        String value = currentString(option);
        drawText(graphics, ellipsize(value.isEmpty() ? "输入文本" : value, contentRight - contentX - 10, false),
                contentX + 5, fieldY + 5, value.isEmpty() ? 0x7FC9CED4 : TEXT_SECONDARY, false);
        if (focused && ((System.currentTimeMillis() / 500L) & 1L) == 0L) {
            int caret = Math.min(contentRight - 5, contentX + 5 + textWidth(value, false) + 1);
            graphics.fill(caret, fieldY + 3, caret + 1, fieldY + h - 3, 0xD8323840);
        }
    }

    private void drawScrollbar(GuiGraphics graphics) {
        if (maxScroll <= 0) return;
        int x = contentRight + 5;
        int top = listTop + 17;
        int bottom = listBottom - 1;
        int view = Math.max(1, bottom - top);
        int content = view + maxScroll;
        int thumb = Math.max(22, view * view / Math.max(1, content));
        int thumbY = top + (int) ((view - thumb) * (scrollAmount / Math.max(1.0D, maxScroll)));
        rounded(graphics, x, top, 3, bottom - top, 1.5F, 0x16323840, 0.0F, 0);
        rounded(graphics, x, thumbY, 3, thumb, 1.5F, 0x72323840, 0.0F, 0);
    }

    private void rounded(GuiGraphics graphics, float x, float y, float w, float h, float radius,
                         int fill, float borderWidth, int border) {
        if (LoliUiShaders.drawRoundedRect(graphics, x, y, w, h, radius, fill, borderWidth, border)) return;
         
        fillRoundedFallback(graphics, Math.round(x), Math.round(y), Math.round(x + w), Math.round(y + h), Math.round(radius), fill);
    }

    private void activeAccent(GuiGraphics graphics, float x, float y, float w, float h, float radius,
                              float opacity, float borderWidth, int border) {
        int alpha = Mth.clamp(Math.round(opacity * 255.0F), 0, 255);
        rounded(graphics, x, y, w, h, radius, alpha << 24 | 0x00272A30, borderWidth, 0x30272A30);
    }

    private void fillRoundedFallback(GuiGraphics graphics, int x1, int y1, int x2, int y2, int radius, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        radius = Math.max(0, Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
        graphics.fill(x1 + radius, y1, x2 - radius, y2, color);
        graphics.fill(x1, y1 + radius, x1 + radius, y2 - radius, color);
        graphics.fill(x2 - radius, y1 + radius, x2, y2 - radius, color);
        for (int dy = 0; dy < radius; dy++) {
            double yy = radius - dy - 0.5D;
            int dx = radius - (int) Math.ceil(Math.sqrt(Math.max(0.0D, radius * radius - yy * yy)));
            graphics.fill(x1 + dx, y1 + dy, x2 - dx, y1 + dy + 1, color);
            graphics.fill(x1 + dx, y2 - dy - 1, x2 - dx, y2 - dy, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(mouseX, mouseY, button);

        int diameter = Math.max(20, panelHeight / 14);
        int buttonH = Math.max(18, diameter - 2);
        int saveW = 46;
        int resetW = 46;
        int headerGap = 6;
        int saveX = panelX + panelWidth - 14 - diameter - 8 - saveW;
        int resetX = saveX - headerGap - resetW;
        int buttonY = panelY + 10;
        if (inside(mouseX, mouseY, resetX, buttonY, resetW, buttonH)) {
            long now = System.currentTimeMillis();
            if (now < resetConfirmUntil) {
                resetToDefaults();
            } else {
                resetConfirmUntil = now + 2500L;
                resetFeedbackUntil = 0L;
            }
            return true;
        }
         
        resetConfirmUntil = 0L;
        if (inside(mouseX, mouseY, saveX, buttonY, saveW, buttonH)) {
            saveSettings();
            return true;
        }
        int closeX = panelX + panelWidth - 14 - diameter / 2;
        int closeY = panelY + 10 + diameter / 2;
        if (distanceSquared(mouseX, mouseY, closeX, closeY) <= (diameter / 2.0) * (diameter / 2.0)) {
            submitAndClose();
            return true;
        }

        int tabX = panelX + 10;
        int tabW = sidebarWidth - 20;
        int tabH = 25;
        int gap = 16;
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            int y = searchY + i * (tabH + gap);
            if (inside(mouseX, mouseY, tabX, y, tabW, tabH)) {
                category = categories[i];
                scrollAmount = 0;
                draggingSlider = null;
                keyboardSlider = null;
                textFocused = null;
                searchFocused = false;
                searchText = "";
                rebuildRows();
                return true;
            }
        }

        int searchH = Math.max(20, panelHeight / 13);
        if (inside(mouseX, mouseY, contentX, searchY, contentRight - contentX, searchH)) {
            searchFocused = true;
            textFocused = null;
            keyboardSlider = null;
            return true;
        }

        searchFocused = false;
        for (RowLayout row : rows) {
            if (!row.visible || row.renderY < listTop + 15 || row.renderY > listBottom) continue;
            LegacyGuiConfig.Option option = row.option;
            int y = row.renderY;
            if (option.type() == LegacyGuiConfig.Type.BOOLEAN) {
                int sx = contentRight - 40;
                int sy = y + 4;
                if (inside(mouseX, mouseY, sx - 4, sy - 4, 48, 28)) {
                    settings.putBoolean(option.key(), !currentBoolean(option));
                    markDirty();
                    saveSettings();
                    keyboardSlider = null;
                    textFocused = null;
                    return true;
                }
            } else if (option.type() == LegacyGuiConfig.Type.INT || option.type() == LegacyGuiConfig.Type.DOUBLE) {
                int x1 = contentX + 5;
                int x2 = contentRight - 5;
                int sliderY = y + 25;
                if (inside(mouseX, mouseY, x1 - 4, sliderY - 7, x2 - x1 + 8, 14)) {
                    draggingSlider = option;
                    keyboardSlider = option;
                    textFocused = null;
                    setNumericFromMouse(option, mouseX, x1, x2);
                    return true;
                }
            } else if (option.type() == LegacyGuiConfig.Type.STRING) {
                int fieldY = y + 19;
                if (inside(mouseX, mouseY, contentX, fieldY, contentRight - contentX, 19)) {
                    textFocused = option;
                    keyboardSlider = null;
                    return true;
                }
            }
        }

        textFocused = null;
        keyboardSlider = null;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingSlider != null) {
            setNumericFromMouse(draggingSlider, mouseX, contentX + 5, contentRight - 5);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingSlider != null) {
            LegacyGuiConfig.Option released = draggingSlider;
            draggingSlider = null;
            snapNumericToTickOnRelease(released);
            saveSettings();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (inside(mouseX, mouseY, contentX, listTop, Math.max(1, contentRight - contentX + 10), Math.max(1, listBottom - listTop)) && maxScroll > 0) {
            scrollAmount = Mth.clamp(scrollAmount - delta * 22.0D, 0.0D, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_N) {
            submitAndClose();
            return true;
        }
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                    scrollAmount = 0;
                    rebuildRows();
                }
                return true;
            }
            if (Screen.isPaste(keyCode) && minecraft != null) {
                searchText = limitText(searchText + minecraft.keyboardHandler.getClipboard(), 48);
                scrollAmount = 0;
                rebuildRows();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        if (textFocused != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                String value = currentString(textFocused);
                if (!value.isEmpty()) {
                    settings.putString(textFocused.key(), value.substring(0, value.length() - 1));
                    markDirty();
                }
                return true;
            }
            if (Screen.isPaste(keyCode) && minecraft != null) {
                settings.putString(textFocused.key(), limitText(currentString(textFocused) + minecraft.keyboardHandler.getClipboard(), 100));
                markDirty();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                textFocused = null;
                saveSettings();
                return true;
            }
        }
        if (keyboardSlider != null && (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT)) {
            nudgeNumeric(keyboardSlider, keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : -1);
            saveSettings();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (Character.isISOControl(codePoint)) return false;
        if (searchFocused) {
            searchText = limitText(searchText + codePoint, 48);
            scrollAmount = 0;
            rebuildRows();
            return true;
        }
        if (textFocused != null) {
            settings.putString(textFocused.key(), limitText(currentString(textFocused) + codePoint, 100));
            markDirty();
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        submitAndClose();
    }

    @Override
    public void removed() {
        submit();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void submitAndClose() {
        saveSettings();
        if (minecraft != null) minecraft.setScreen(null);
    }

    private void submit() {
        saveSettings();
    }

    private void markDirty() {
        dirty = true;
        savedFeedbackUntil = 0L;
    }

    private void saveSettings() {
        if (!dirty) {
            savedFeedbackUntil = System.currentTimeMillis() + 1200L;
            return;
        }
        CompoundTag sanitized = LegacyGuiConfig.sanitize(settings.copy());

         
         
        if (minecraft != null && minecraft.player != null) {
            ItemStack held = minecraft.player.getMainHandItem();
            if (!held.isEmpty() && held.getItem() == stack.getItem()) {
                held.getOrCreateTag().put(LegacyLoliSettings.ROOT, sanitized.copy());
            }
        }

        ModNetwork.CHANNEL.sendToServer(new UpdateLoliSettingsPacket(sanitized.copy()));
        dirty = false;
        savedFeedbackUntil = System.currentTimeMillis() + 1400L;
    }

    private void resetToDefaults() {
        for (LegacyGuiConfig.Option option : LegacyGuiConfig.OPTIONS) {
            switch (option.type()) {
                case BOOLEAN -> settings.putBoolean(option.key(), LegacyGuiConfig.defaultBoolean(option.key()));
                case INT -> settings.putInt(option.key(), LegacyGuiConfig.defaultInt(option.key()));
                case DOUBLE -> settings.putDouble(option.key(), LegacyGuiConfig.defaultDouble(option.key()));
                case STRING -> settings.putString(option.key(), LegacyGuiConfig.defaultString(option.key()));
            }
        }
        resetConfirmUntil = 0L;
        resetFeedbackUntil = System.currentTimeMillis() + 1600L;
        markDirty();
        saveSettings();
        rebuildRows();
    }

    private Category categoryFor(LegacyGuiConfig.Option option) {
        String key = option.key();
        if (key.equals("loliPickaxeMandatoryDrop")
                || key.equals("loliPickaxeStopOnLiquid")
                || key.equals("loliPickaxeBlockReachDistance")
                || key.equals("loliPickaxeAutoAccept")
                || key.equals("loliPickaxeInfiniteBattery")
                || key.equals("loliPickaxeInvisible")
                || key.equals("loliPickaxeShowInvisible")) {
            return Category.BASIC;
        }
        if (key.equals("loliPickaxeThorns")
                || key.equals("loliPickaxeKillRangeEntity")
                || key.equals("loliPickaxeKillRange")
                || key.equals("loliPickaxeAutoKillRangeEntity")
                || key.equals("loliPickaxeAutoKillRange")
                || key.equals("loliPickaxeCompulsoryRemove")
                || key.equals("loliPickaxeValidToAmityEntity")
                || key.equals("loliPickaxeValidToAllEntity")
                || key.equals("loliPickaxeKillFacing")
                || key.equals("loliPickaxeKillFacingRange")
                || key.equals("loliPickaxeKillFacingSlope")) {
            return Category.COMBAT;
        }
        return Category.OTHER;
    }

    private boolean currentBoolean(LegacyGuiConfig.Option option) {
        return settings.contains(option.key()) ? settings.getBoolean(option.key()) : LegacyGuiConfig.defaultBoolean(option.key());
    }

    private String currentString(LegacyGuiConfig.Option option) {
        return settings.contains(option.key()) ? settings.getString(option.key()) : LegacyGuiConfig.defaultString(option.key());
    }

    private double currentNumber(LegacyGuiConfig.Option option) {
        if (option.type() == LegacyGuiConfig.Type.INT) {
            return settings.contains(option.key()) ? settings.getInt(option.key()) : LegacyGuiConfig.defaultInt(option.key());
        }
        return settings.contains(option.key()) ? settings.getDouble(option.key()) : LegacyGuiConfig.defaultDouble(option.key());
    }

    private double numericFraction(LegacyGuiConfig.Option option) {
        double span = option.max() - option.min();
        return span <= 0.0D ? 0.0D : Mth.clamp((currentNumber(option) - option.min()) / span, 0.0D, 1.0D);
    }

    private String formattedValue(LegacyGuiConfig.Option option) {
        if (option.type() == LegacyGuiConfig.Type.INT) return Integer.toString((int) Math.round(currentNumber(option)));
        String raw = String.format(Locale.ROOT, "%.2f", currentNumber(option));
        while (raw.endsWith("0")) raw = raw.substring(0, raw.length() - 1);
        if (raw.endsWith(".")) raw = raw.substring(0, raw.length() - 1);
        return raw;
    }

    private void setNumericFromMouse(LegacyGuiConfig.Option option, double mouseX, int x1, int x2) {
        double fraction = Mth.clamp((mouseX - x1) / Math.max(1.0D, x2 - x1), 0.0D, 1.0D);
        setNumericFromFraction(option, softSnapSliderFraction(fraction));
    }

    private double softSnapSliderFraction(double fraction) {
        double nearest = nearestSliderTick(fraction);
        double distance = Math.abs(fraction - nearest);
        if (distance >= SLIDER_SOFT_SNAP_RADIUS) return fraction;

        double closeness = 1.0D - distance / SLIDER_SOFT_SNAP_RADIUS;
         
        closeness = closeness * closeness * (3.0D - 2.0D * closeness);
        return Mth.lerp(closeness * SLIDER_SOFT_SNAP_PULL, fraction, nearest);
    }

    private void snapNumericToTickOnRelease(LegacyGuiConfig.Option option) {
        double fraction = numericFraction(option);
        double nearest = nearestSliderTick(fraction);
        if (Math.abs(fraction - nearest) <= SLIDER_RELEASE_SNAP_RADIUS) {
            setNumericFromFraction(option, nearest);
        }
    }

    private double nearestSliderTick(double fraction) {
        return Mth.clamp(Math.round(fraction * 3.0D) / 3.0D, 0.0D, 1.0D);
    }

    private void setNumericFromFraction(LegacyGuiConfig.Option option, double fraction) {
        double before = currentNumber(option);
        double raw = option.min() + (option.max() - option.min()) * Mth.clamp(fraction, 0.0D, 1.0D);
        if (option.type() == LegacyGuiConfig.Type.INT) {
            settings.putInt(option.key(), Mth.clamp((int) Math.round(raw), (int) option.min(), (int) option.max()));
        } else {
            double step = option.max() - option.min() <= 1.0D ? 0.01D : 0.1D;
            settings.putDouble(option.key(), Mth.clamp(Math.round(raw / step) * step, option.min(), option.max()));
        }
        if (Math.abs(currentNumber(option) - before) > 1.0E-9D) markDirty();
    }

    private void nudgeNumeric(LegacyGuiConfig.Option option, int direction) {
        double before = currentNumber(option);
        if (option.type() == LegacyGuiConfig.Type.INT) {
            settings.putInt(option.key(), Mth.clamp((int) Math.round(before) + direction,
                    (int) option.min(), (int) option.max()));
        } else {
            double step = option.max() - option.min() <= 1.0D ? 0.01D : 0.1D;
            settings.putDouble(option.key(), Mth.clamp(before + step * direction, option.min(), option.max()));
        }
        if (Math.abs(currentNumber(option) - before) > 1.0E-9D) markDirty();
    }

    private String limitText(String text, int maxLength) {
        if (text == null) return "";
        text = text.replace("\r", "").replace("\n", " ");
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private int dividerColor() {
        return 0x1F34383F;
    }

    private int rowDividerColor() {
        return 0x1234383F;
    }

    private int selectedTabFill() {
        return 0x1434383F;
    }

    private int hoverTabFill() {
        return 0x0D34383F;
    }

    private int searchFill(boolean focused, boolean hovered) {
        return focused ? 0xFFF2F4F6 : (hovered ? 0xFFEFF1F3 : 0xFFE9ECEF);
    }

    private int searchFocusBorder() {
        return 0x55323840;
    }

    private int controlBorderColor() {
        return 0x24323840;
    }

    private int switchOffFill() {
        return 0xFFD7DADF;
    }

    private int sliderTrackColor() {
        return 0x70373C43;
    }

    private int fieldFill(boolean focused, boolean hovered) {
        return focused ? 0xFFF0F2F4 : (hovered ? 0xFFECEFF1 : 0xFFE7EAED);
    }

    private int themedTextColor(int color) {
        int a = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        int r = (rgb >>> 16) & 0xFF;
        int g = (rgb >>> 8) & 0xFF;
        int b = rgb & 0xFF;
        int brightness = (r * 299 + g * 587 + b * 114) / 1000;
        if (brightness < 185) return color;
        int themed = 0x002B3037;
        if (color == TEXT_SECONDARY) themed = 0x005A6068;
        if (color == TEXT_MUTED) themed = 0x007B8189;
        return a << 24 | themed;
    }

    private void drawText(GuiGraphics graphics, String text, int x, int y, int color, boolean bold) {
        Component component = Component.literal(text).withStyle(style -> style.withFont(bold ? UI_FONT_BOLD : UI_FONT));
        graphics.drawString(font, component, x, y, themedTextColor(color), false);
    }

    private int textWidth(String text, boolean bold) {
        Component component = Component.literal(text).withStyle(style -> style.withFont(bold ? UI_FONT_BOLD : UI_FONT));
        return font.width(component);
    }

    private String ellipsize(String text, int maxWidth, boolean bold) {
        if (maxWidth <= 0 || text == null || text.isEmpty()) return "";
        if (textWidth(text, bold) <= maxWidth) return text;
        String suffix = "…";
        int suffixWidth = textWidth(suffix, bold);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String next = result.toString() + text.charAt(i);
            if (textWidth(next, bold) + suffixWidth > maxWidth) break;
            result.append(text.charAt(i));
        }
        return result + suffix;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static double distanceSquared(double x, double y, double cx, double cy) {
        double dx = x - cx;
        double dy = y - cy;
        return dx * dx + dy * dy;
    }

    private enum Category {
        BASIC("基础", "常用"),
        COMBAT("战斗", "攻击"),
        OTHER("其它", "其它");

        private final String label;
        private final String section;

        Category(String label, String section) {
            this.label = label;
            this.section = section;
        }
    }

    private static final class RowLayout {
        private final LegacyGuiConfig.Option option;
        private final int baseY;
        private final int height;
        private int renderY;
        private boolean visible;

        private RowLayout(LegacyGuiConfig.Option option, int baseY, int height) {
            this.option = option;
            this.baseY = baseY;
            this.height = height;
        }
    }
}
