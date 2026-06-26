package com.secondmod.autotunefps.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public final class GlassButton {
    private static final int NORMAL_BACKGROUND = 0xB81B1C1E;
    private static final int HOVER_BACKGROUND = 0xCC292A2D;
    private static final int SELECTED_BACKGROUND = 0xD0333437;
    private static final int DISABLED_BACKGROUND = 0x98161719;
    private static final int NORMAL_BORDER = 0xA0787A7E;
    private static final int HOVER_BORDER = 0xDDD5D6D8;
    private static final int SELECTED_BORDER = 0xFFE4E4E5;
    private static final int DISABLED_BORDER = 0x70646669;

    private final Button widget;
    private final int x;
    private final int width;
    private final int height;
    private final Component message;
    private final boolean selected;
    private int y;
    private int clipTop = Integer.MIN_VALUE;
    private int clipBottom = Integer.MAX_VALUE;

    public GlassButton(
        int x,
        int y,
        int width,
        int height,
        Component message,
        Button.OnPress onPress,
        boolean selected
    ) {
        this.widget = Button.builder(Component.empty(), onPress)
            .bounds(x, y, width, height)
            .createNarration(ignored -> message.copy())
            .build();
        this.widget.setAlpha(0.0F);
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.message = message;
        this.selected = selected;
    }

    public Button widget() {
        return widget;
    }

    public void setActive(boolean active) {
        this.widget.active = active;
    }

    public void setTooltip(Tooltip tooltip) {
        this.widget.setTooltip(tooltip);
    }

    public void setY(int y) {
        this.y = y;
        updateWidgetBounds();
    }

    public void setVerticalClip(int top, int bottom) {
        this.clipTop = top;
        this.clipBottom = bottom;
        updateWidgetBounds();
    }

    private void updateWidgetBounds() {
        int visibleTop = Math.max(y, clipTop);
        int visibleBottom = Math.min(y + height, clipBottom);
        this.widget.setY(visibleTop);
        this.widget.setHeight(Math.max(0, visibleBottom - visibleTop));
        this.widget.visible = visibleBottom > visibleTop;
    }

    public void renderGlass(GuiGraphics guiGraphics) {
        int left = x;
        int top = y;
        int right = left + width;
        int bottom = top + height;
        boolean highlighted = widget.isHoveredOrFocused();

        int background = selected
            ? SELECTED_BACKGROUND
            : !widget.active ? DISABLED_BACKGROUND : highlighted ? HOVER_BACKGROUND : NORMAL_BACKGROUND;
        int border = selected
            ? SELECTED_BORDER
            : !widget.active ? DISABLED_BORDER : highlighted ? HOVER_BORDER : NORMAL_BORDER;

        guiGraphics.fill(left + 2, top + 2, right + 2, bottom + 2, 0x52000000);
        guiGraphics.fill(left, top, right, bottom, background);
        guiGraphics.fill(left, top, right, top + 1, border);
        guiGraphics.fill(left, bottom - 1, right, bottom, border);
        guiGraphics.fill(left, top, left + 1, bottom, border);
        guiGraphics.fill(right - 1, top, right, bottom, border);
        guiGraphics.fill(left + 2, top + 2, right - 2, top + 3, highlighted ? 0x48FFFFFF : 0x24FFFFFF);
        if (selected) {
            guiGraphics.fill(left + 2, bottom - 3, right - 2, bottom - 2, 0xA8E0E0E1);
        }

        int textColor = widget.active || selected ? 0xFFFFFFFF : 0xFFD0D0D0;
        guiGraphics.drawCenteredString(
            Minecraft.getInstance().font,
            message,
            left + width / 2,
            top + (height - 8) / 2,
            textColor
        );
    }
}
