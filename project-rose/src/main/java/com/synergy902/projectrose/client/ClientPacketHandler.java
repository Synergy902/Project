package com.synergy902.projectrose.client;

import com.synergy902.projectrose.client.screen.TeamClassScreen;
import com.synergy902.projectrose.client.screen.MapVoteScreen;
import com.synergy902.projectrose.game.MatchPhase;
import com.synergy902.projectrose.network.MatchSnapshot;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handleSnapshot(MatchSnapshot snapshot) {
        ClientMatchState.update(snapshot);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof MapVoteScreen && snapshot.phase() != MatchPhase.MAP_VOTE) {
            minecraft.setScreen(null);
        }
    }

    public static void openLobby() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && !(minecraft.screen instanceof TeamClassScreen)) {
            minecraft.setScreen(new TeamClassScreen());
        }
    }

    public static void openMapVote() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && !(minecraft.screen instanceof MapVoteScreen)) {
            minecraft.setScreen(new MapVoteScreen());
        }
    }
}
