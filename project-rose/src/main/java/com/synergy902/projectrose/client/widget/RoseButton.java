package com.synergy902.projectrose.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class RoseButton extends AbstractButton {
    private static final int AMBER = 0xFFFFA31A;
    private static final int WHITE = 0xFFF1F1F1;
    private static final int DISABLED = 0xFF626262;
    private final Consumer<RoseButton> action;
    private final int accentColor;

    public RoseButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            int accentColor,
            Consumer<RoseButton> action
    ) {
        super(x, y, width, height, message);
        this.accentColor = accentColor;
        this.action = action;
    }

    @Override
    public void onPress() {
        action.accept(this);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean highlighted = isHoveredOrFocused() && active;
        int background = !active ? 0xC00C0C0C : highlighted ? 0xEE2C2418 : 0xE6171717;
        int border = highlighted ? AMBER : 0xFF3A3A3A;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
        graphics.fill(getX(), getY(), getX() + 3, getY() + height, active ? accentColor : DISABLED);
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, border);
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
        int textColor = !active ? DISABLED : highlighted ? AMBER : WHITE;
        graphics.drawCenteredString(
                Minecraft.getInstance().font,
                getMessage(),
                getX() + width / 2,
                getY() + (height - 8) / 2,
                textColor
        );
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}

