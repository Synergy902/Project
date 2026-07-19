package com.synergy902.projectrose.client.screen.admin;

import com.synergy902.projectrose.client.ClientMatchState;
import com.synergy902.projectrose.network.MatchSnapshot;
import com.synergy902.projectrose.network.RoseNetwork;
import net.minecraft.client.gui.GuiGraphics;
import com.synergy902.projectrose.client.widget.RoseButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AdminLoadoutListScreen extends Screen {
    private static final int AMBER = 0xFFFFA31A;
    private static final int WHITE = 0xFFF2F2F2;
    private static final int MUTED = 0xFF909090;
    private final Screen parent;

    public AdminLoadoutListScreen(Screen parent) {
        super(Component.literal("Project Rose Administrator Loadouts"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = width / 2;
        int top = Math.max(48, height / 2 - 68);
        MatchSnapshot snapshot = ClientMatchState.snapshot();
        for (int index = 0; index < 5; index++) {
            String name = snapshot.loadoutNames().get(index);
            String label = "EDIT CLASS " + (index + 1) + " // "
                    + (name.isBlank() ? "NOT CONFIGURED" : name.toUpperCase());
            int selected = index;
            addRenderableWidget(new RoseButton(
                    center - 145, top + index * 24, 290, 20,
                    Component.literal(label), AMBER,
                    ignored -> RoseNetwork.openAdminLoadout(selected)));
        }
        addRenderableWidget(new RoseButton(
                center - 50, top + 126, 100, 20,
                Component.literal("BACK"), AMBER,
                ignored -> onClose()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE0070707);
        int center = width / 2;
        graphics.fill(center - 168, 14, center + 168, 43, 0xEE111111);
        graphics.fill(center - 168, 40, center + 168, 43, AMBER);
        graphics.drawCenteredString(font, "PROJECT ROSE // ADMIN LOADOUTS", center, 22, WHITE);
        graphics.drawCenteredString(font, "CHANGES ARE SERVER-SIDE AND OPERATOR-ONLY", center, 34, MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
