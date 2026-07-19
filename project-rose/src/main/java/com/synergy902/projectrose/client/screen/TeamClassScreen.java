package com.synergy902.projectrose.client.screen;

import com.synergy902.projectrose.client.ClientMatchState;
import com.synergy902.projectrose.game.MatchPhase;
import com.synergy902.projectrose.game.RoseTeam;
import com.synergy902.projectrose.network.MatchSnapshot;
import com.synergy902.projectrose.network.RoseNetwork;
import com.synergy902.projectrose.network.SelectTeamPacket;
import com.synergy902.projectrose.client.screen.admin.AdminLoadoutListScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import com.synergy902.projectrose.client.widget.RoseButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TeamClassScreen extends Screen {
    private static final int CLASS_TOP = 108;
    private static final int CLASS_STEP = 22;
    private static final int PANEL = 0xE20A0A0A;
    private static final int PANEL_LIGHT = 0xE21B1B1B;
    private static final int AMBER = 0xFFFFA31A;
    private static final int RED = 0xFFB32222;
    private static final int BLUE = 0xFF2457A6;
    private static final int WHITE = 0xFFF2F2F2;
    private static final int MUTED = 0xFF8A8A8A;

    public TeamClassScreen() {
        super(Component.literal("Project Rose Team and Class Selection"));
    }

    @Override
    protected void init() {
        int center = width / 2;
        int teamY = 48;
        int teamWidth = 126;
        MatchSnapshot snapshot = ClientMatchState.snapshot();

        RoseButton redTeamButton = new RoseButton(
                center - teamWidth - 8, teamY, teamWidth, 24,
                Component.literal("JOIN RED TEAM"), RED,
                button -> RoseNetwork.chooseTeam(new SelectTeamPacket(RoseTeam.RED)));
        redTeamButton.active = snapshot.phase() == MatchPhase.WAITING
                || (snapshot.phase() == MatchPhase.ACTIVE && snapshot.playerTeam() == RoseTeam.NONE);
        addRenderableWidget(redTeamButton);
        RoseButton blueTeamButton = new RoseButton(
                center + 8, teamY, teamWidth, 24,
                Component.literal("JOIN BLUE TEAM"), BLUE,
                button -> RoseNetwork.chooseTeam(new SelectTeamPacket(RoseTeam.BLUE)));
        blueTeamButton.active = redTeamButton.active;
        addRenderableWidget(blueTeamButton);

        for (int index = 0; index < 5; index++) {
            String configuredName = snapshot.loadoutNames().get(index);
            String label = configuredName.isBlank()
                    ? "CLASS " + (index + 1) + " // NOT CONFIGURED"
                    : "CLASS " + (index + 1) + " // " + configuredName.toUpperCase();
            int selectedIndex = index;
            RoseButton button = new RoseButton(
                    center - 138, CLASS_TOP + index * CLASS_STEP, 276, 20,
                    Component.literal(label), AMBER,
                    ignored -> RoseNetwork.chooseLoadout(selectedIndex));
            button.active = !configuredName.isBlank();
            addRenderableWidget(button);
        }

        int bottomY = Math.min(CLASS_TOP + 116, height - 24);
        if (snapshot.administrator()) {
            addRenderableWidget(new RoseButton(
                    center - 138, bottomY, 132, 20,
                    Component.literal("ADMIN LOADOUTS"), AMBER,
                    ignored -> minecraft.setScreen(new AdminLoadoutListScreen(this))));
            addRenderableWidget(new RoseButton(
                    center + 6, bottomY, 132, 20,
                    Component.literal("CLOSE"), AMBER,
                    ignored -> onClose()));
        } else {
            addRenderableWidget(new RoseButton(
                    center - 50, bottomY, 100, 20,
                    Component.literal("CLOSE"), AMBER,
                    ignored -> onClose()));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xD8070707);
        int center = width / 2;
        MatchSnapshot snapshot = ClientMatchState.snapshot();

        graphics.fill(center - 166, 14, center + 166, 42, PANEL);
        graphics.fill(center - 166, 40, center + 166, 42, AMBER);
        graphics.drawCenteredString(font, "PROJECT ROSE // TEAM DEATHMATCH", center, 23, WHITE);

        drawTeamPanel(graphics, center - 142, 76, 126, RED, "RED TEAM", snapshot.redPlayers(),
                snapshot.playerTeam() == RoseTeam.RED);
        drawTeamPanel(graphics, center + 16, 76, 126, BLUE, "BLUE TEAM", snapshot.bluePlayers(),
                snapshot.playerTeam() == RoseTeam.BLUE);

        graphics.fill(center - 148, 99, center + 148, 104, PANEL_LIGHT);
        graphics.drawString(font, "SELECT CLASS", center - 138, 97, AMBER, false);

        if (snapshot.selectedLoadout() >= 0) {
            int selectedY = CLASS_TOP + snapshot.selectedLoadout() * CLASS_STEP;
            graphics.fill(center - 143, selectedY - 2, center - 139, selectedY + 22, AMBER);
        }

        if (snapshot.phase() != MatchPhase.WAITING) {
            graphics.drawCenteredString(font, "CLASS CHANGES APPLY ON YOUR NEXT RESPAWN", center,
                    Math.min(height - 34, 246), MUTED);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawTeamPanel(
            GuiGraphics graphics,
            int x,
            int y,
            int panelWidth,
            int color,
            String label,
            int players,
            boolean selected
    ) {
        graphics.fill(x, y, x + panelWidth, y + 22, PANEL_LIGHT);
        graphics.fill(x, y, x + 4, y + 22, color);
        graphics.drawString(font, label, x + 10, y + 4, WHITE, false);
        String count = players + (players == 1 ? " PLAYER" : " PLAYERS");
        graphics.drawString(font, count, x + 10, y + 13, MUTED, false);
        if (selected) {
            graphics.fill(x, y + 20, x + panelWidth, y + 22, AMBER);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
