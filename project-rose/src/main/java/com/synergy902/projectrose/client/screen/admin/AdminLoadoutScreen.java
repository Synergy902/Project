package com.synergy902.projectrose.client.screen.admin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.synergy902.projectrose.client.ClientMatchState;
import com.synergy902.projectrose.menu.AdminLoadoutMenu;
import com.synergy902.projectrose.network.AdminLoadoutActionPacket;
import com.synergy902.projectrose.network.RoseNetwork;
import net.minecraft.client.gui.GuiGraphics;
import com.synergy902.projectrose.client.widget.RoseButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AdminLoadoutScreen extends AbstractContainerScreen<AdminLoadoutMenu> {
    private static final int PANEL = 0xF00C0C0C;
    private static final int PANEL_LIGHT = 0xFF202020;
    private static final int SLOT = 0xFF303030;
    private static final int AMBER = 0xFFFFA31A;
    private static final int WHITE = 0xFFF0F0F0;
    private static final int MUTED = 0xFF929292;

    private EditBox nameBox;

    public AdminLoadoutScreen(AdminLoadoutMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 218;
        imageHeight = 220;
        inventoryLabelY = 101;
    }

    @Override
    protected void init() {
        super.init();
        int index = menu.loadoutIndex();
        String existingName = ClientMatchState.snapshot().loadoutNames().get(index);
        nameBox = new EditBox(font, leftPos + 73, topPos + 5, 137, 14, Component.literal("Class name"));
        nameBox.setMaxLength(32);
        nameBox.setValue(existingName.isBlank() ? "Class " + (index + 1) : existingName);
        addRenderableWidget(nameBox);

        int buttonY = topPos + 196;
        addRenderableWidget(new RoseButton(
                leftPos + 8, buttonY, 62, 18, Component.literal("IMPORT"), AMBER,
                ignored -> RoseNetwork.adminLoadoutAction(
                        AdminLoadoutActionPacket.Action.IMPORT_CURRENT, nameBox.getValue())));
        addRenderableWidget(new RoseButton(
                leftPos + 78, buttonY, 62, 18, Component.literal("CLEAR"), 0xFF9D2828,
                ignored -> RoseNetwork.adminLoadoutAction(
                        AdminLoadoutActionPacket.Action.CLEAR, nameBox.getValue())));
        addRenderableWidget(new RoseButton(
                leftPos + 148, buttonY, 62, 18, Component.literal("SAVE"), AMBER,
                ignored -> RoseNetwork.adminLoadoutAction(
                        AdminLoadoutActionPacket.Action.SAVE, nameBox.getValue())));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 3, AMBER);
        graphics.fill(leftPos + 5, topPos + 99, leftPos + imageWidth - 5, topPos + 101, PANEL_LIGHT);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, leftPos + 8 + column * 18, topPos + 18 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, leftPos + 8 + column * 18, topPos + 76);
        }
        for (int armor = 0; armor < 4; armor++) {
            drawSlot(graphics, leftPos + 174, topPos + 18 + armor * 18);
        }
        drawSlot(graphics, leftPos + 192, topPos + 18);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, leftPos + 8 + column * 18, topPos + 112 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, leftPos + 8 + column * 18, topPos + 170);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, "CLASS " + (menu.loadoutIndex() + 1), 8, 7, AMBER, false);
        graphics.drawString(font, "ARMOR", 171, 91, MUTED, false);
        graphics.drawString(font, "PLAYER INVENTORY", 8, inventoryLabelY, WHITE, false);
        graphics.drawString(font, "SAVE CAPTURES CURRENT CURIOS", 8, 91, MUTED, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT);
        graphics.fill(x, y, x + 16, y + 16, 0xFF141414);
    }
}
