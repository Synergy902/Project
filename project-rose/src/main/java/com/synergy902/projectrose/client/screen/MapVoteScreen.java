package com.synergy902.projectrose.client.screen;

import com.synergy902.projectrose.client.ClientMatchState;
import com.synergy902.projectrose.client.widget.RoseButton;
import com.synergy902.projectrose.network.MatchSnapshot;
import com.synergy902.projectrose.network.RoseNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class MapVoteScreen extends Screen {
    private static final int AMBER = 0xFFFFA31A;
    private static final int WHITE = 0xFFF1F1F1;
    private static final int MUTED = 0xFF929292;
    private static final int BLUE = 0xFF315F9E;

    public MapVoteScreen() {
        super(Component.literal("Choose Next Map"));
    }

    @Override
    protected void init() {
        MatchSnapshot snapshot = ClientMatchState.snapshot();
        int center = width / 2;
        int top = Math.max(58, height / 2 - snapshot.mapVoteOptions().size() * 12);
        for (int index = 0; index < snapshot.mapVoteOptions().size(); index++) {
            String mapId = snapshot.mapVoteOptions().get(index);
            int selectedIndex = index;
            addRenderableWidget(new RoseButton(
                    center - 150,
                    top + index * 28,
                    300,
                    23,
                    Component.literal(formatMapName(mapId)),
                    selectedIndex == snapshot.selectedMapVote() ? AMBER : BLUE,
                    ignored -> RoseNetwork.castMapVote(mapId)
            ));
        }
        addRenderableWidget(new RoseButton(
                center - 55,
                Math.min(height - 27, top + snapshot.mapVoteOptions().size() * 28 + 8),
                110,
                20,
                Component.literal("CLOSE POLL"),
                AMBER,
                ignored -> onClose()
        ));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE5080808);
        int center = width / 2;
        MatchSnapshot snapshot = ClientMatchState.snapshot();
        int top = Math.max(58, height / 2 - snapshot.mapVoteOptions().size() * 12);

        graphics.fill(center - 177, 14, center + 177, 47, 0xF0121212);
        graphics.fill(center - 177, 44, center + 177, 47, AMBER);
        graphics.drawCenteredString(font, "PROJECT ROSE // CHOOSE NEXT MAP", center, 21, WHITE);
        graphics.drawCenteredString(font, "POLL CLOSES IN " + snapshot.secondsRemaining() + " SECONDS",
                center, 34, AMBER);

        super.render(graphics, mouseX, mouseY, partialTick);
        for (int index = 0; index < snapshot.mapVoteOptions().size(); index++) {
            int votes = index < snapshot.mapVoteCounts().size() ? snapshot.mapVoteCounts().get(index) : 0;
            String count = votes + (votes == 1 ? " VOTE" : " VOTES");
            graphics.drawString(font, count,
                    center + 143 - font.width(count), top + index * 28 + 8, MUTED, false);
            if (snapshot.selectedMapVote() == index) {
                graphics.fill(center - 155, top + index * 28, center - 151, top + index * 28 + 23, AMBER);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String formatMapName(String mapId) {
        return mapId.replace('_', ' ').toUpperCase(Locale.ROOT);
    }
}
