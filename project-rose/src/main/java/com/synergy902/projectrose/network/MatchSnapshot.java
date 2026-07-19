package com.synergy902.projectrose.network;

import com.synergy902.projectrose.config.RoseConfig;
import com.synergy902.projectrose.data.RoseSavedData;
import com.synergy902.projectrose.game.MatchManager;
import com.synergy902.projectrose.game.MatchPhase;
import com.synergy902.projectrose.game.PlayerSession;
import com.synergy902.projectrose.game.RoseTeam;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public record MatchSnapshot(
        MatchPhase phase,
        RoseTeam playerTeam,
        int selectedLoadout,
        int redScore,
        int blueScore,
        int scoreLimit,
        int secondsRemaining,
        int redPlayers,
        int bluePlayers,
        int kills,
        int deaths,
        int assists,
        int balanceWarningSeconds,
        int spawnProtectionSeconds,
        boolean administrator,
        RoseTeam winner,
        String activeMapId,
        int selectedMapVote,
        List<String> mapVoteOptions,
        List<Integer> mapVoteCounts,
        List<String> loadoutNames
) {
    public MatchSnapshot {
        mapVoteOptions = List.copyOf(mapVoteOptions);
        mapVoteCounts = List.copyOf(mapVoteCounts);
        loadoutNames = List.copyOf(loadoutNames);
    }

    public static MatchSnapshot create(MatchManager manager, ServerPlayer player) {
        PlayerSession session = manager.session(player.getUUID());
        RoseSavedData data = RoseSavedData.get(player.server);
        List<String> names = new ArrayList<>(5);
        for (int index = 0; index < 5; index++) {
            names.add(data.loadout(index).map(value -> value.name()).orElse(""));
        }
        return new MatchSnapshot(
                manager.phase(),
                session.team(),
                session.loadoutIndex(),
                manager.score(RoseTeam.RED),
                manager.score(RoseTeam.BLUE),
                RoseConfig.SCORE_LIMIT.get(),
                manager.secondsRemaining(),
                manager.onlineTeamSize(RoseTeam.RED),
                manager.onlineTeamSize(RoseTeam.BLUE),
                session.kills(),
                session.deaths(),
                session.assists(),
                manager.balanceSecondsRemaining(),
                manager.spawnProtectionSeconds(player),
                player.hasPermissions(2),
                manager.winner(),
                manager.activeMapId(),
                manager.selectedMapVote(player.getUUID()),
                manager.mapVoteOptions(),
                manager.mapVoteCounts(),
                names
        );
    }

    public static void encode(MatchSnapshot snapshot, FriendlyByteBuf buffer) {
        buffer.writeEnum(snapshot.phase);
        buffer.writeEnum(snapshot.playerTeam);
        buffer.writeVarInt(snapshot.selectedLoadout);
        buffer.writeVarInt(snapshot.redScore);
        buffer.writeVarInt(snapshot.blueScore);
        buffer.writeVarInt(snapshot.scoreLimit);
        buffer.writeVarInt(snapshot.secondsRemaining);
        buffer.writeVarInt(snapshot.redPlayers);
        buffer.writeVarInt(snapshot.bluePlayers);
        buffer.writeVarInt(snapshot.kills);
        buffer.writeVarInt(snapshot.deaths);
        buffer.writeVarInt(snapshot.assists);
        buffer.writeVarInt(snapshot.balanceWarningSeconds);
        buffer.writeVarInt(snapshot.spawnProtectionSeconds);
        buffer.writeBoolean(snapshot.administrator);
        buffer.writeEnum(snapshot.winner);
        buffer.writeUtf(snapshot.activeMapId, 32);
        buffer.writeVarInt(snapshot.selectedMapVote);
        buffer.writeVarInt(snapshot.mapVoteOptions.size());
        for (int index = 0; index < snapshot.mapVoteOptions.size(); index++) {
            buffer.writeUtf(snapshot.mapVoteOptions.get(index), 32);
            buffer.writeVarInt(index < snapshot.mapVoteCounts.size() ? snapshot.mapVoteCounts.get(index) : 0);
        }
        buffer.writeVarInt(snapshot.loadoutNames.size());
        snapshot.loadoutNames.forEach(name -> buffer.writeUtf(name, 32));
    }

    public static MatchSnapshot decode(FriendlyByteBuf buffer) {
        MatchPhase phase = buffer.readEnum(MatchPhase.class);
        RoseTeam team = buffer.readEnum(RoseTeam.class);
        int selectedLoadout = buffer.readVarInt();
        int redScore = buffer.readVarInt();
        int blueScore = buffer.readVarInt();
        int scoreLimit = buffer.readVarInt();
        int secondsRemaining = buffer.readVarInt();
        int redPlayers = buffer.readVarInt();
        int bluePlayers = buffer.readVarInt();
        int kills = buffer.readVarInt();
        int deaths = buffer.readVarInt();
        int assists = buffer.readVarInt();
        int balanceWarningSeconds = buffer.readVarInt();
        int spawnProtectionSeconds = buffer.readVarInt();
        boolean administrator = buffer.readBoolean();
        RoseTeam winner = buffer.readEnum(RoseTeam.class);
        String activeMapId = buffer.readUtf(32);
        int selectedMapVote = buffer.readVarInt();
        int voteOptionCount = Math.min(10, buffer.readVarInt());
        List<String> voteOptions = new ArrayList<>(voteOptionCount);
        List<Integer> voteCounts = new ArrayList<>(voteOptionCount);
        for (int index = 0; index < voteOptionCount; index++) {
            voteOptions.add(buffer.readUtf(32));
            voteCounts.add(buffer.readVarInt());
        }
        int nameCount = Math.min(5, buffer.readVarInt());
        List<String> names = new ArrayList<>(5);
        for (int index = 0; index < nameCount; index++) {
            names.add(buffer.readUtf(32));
        }
        while (names.size() < 5) {
            names.add("");
        }
        return new MatchSnapshot(
                phase,
                team,
                selectedLoadout,
                redScore,
                blueScore,
                scoreLimit,
                secondsRemaining,
                redPlayers,
                bluePlayers,
                kills,
                deaths,
                assists,
                balanceWarningSeconds,
                spawnProtectionSeconds,
                administrator,
                winner,
                activeMapId,
                selectedMapVote,
                voteOptions,
                voteCounts,
                names
        );
    }
}
