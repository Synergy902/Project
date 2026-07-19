package com.synergy902.projectrose.game;

import com.synergy902.projectrose.config.RoseConfig;
import com.synergy902.projectrose.data.RoseSavedData;
import com.synergy902.projectrose.loadout.LoadoutPreset;
import com.synergy902.projectrose.network.RoseNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

public final class MatchManager {
    private static final String RED_SCOREBOARD_TEAM = "rose_red";
    private static final String BLUE_SCOREBOARD_TEAM = "rose_blue";
    private static final Map<MinecraftServer, MatchManager> INSTANCES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private final MinecraftServer server;
    private final Map<UUID, PlayerSession> sessions = new LinkedHashMap<>();
    private final EnumMap<RoseTeam, Integer> scores = new EnumMap<>(RoseTeam.class);
    private final Map<UUID, Integer> spawnProtectionEndTicks = new LinkedHashMap<>();
    private final Map<UUID, String> mapVotes = new LinkedHashMap<>();
    private List<String> mapVoteOptions = List.of();
    private MatchPhase phase = MatchPhase.WAITING;
    private RoseTeam winner = RoseTeam.NONE;
    private int phaseTicksRemaining;
    private int balanceTicksRemaining = -1;
    private int nextMapTeamSelectionTicks;
    private boolean nextMapAutoStartPending;
    private int snapshotSyncTicks;
    private long joinSequence;

    private MatchManager(MinecraftServer server) {
        this.server = server;
        scores.put(RoseTeam.RED, 0);
        scores.put(RoseTeam.BLUE, 0);
        ensureScoreboardTeams();
    }

    public static MatchManager get(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(server, MatchManager::new);
    }

    public static void remove(MinecraftServer server) {
        INSTANCES.remove(server);
    }

    public MatchPhase phase() {
        return phase;
    }

    public RoseTeam winner() {
        return winner;
    }

    public int score(RoseTeam team) {
        return scores.getOrDefault(team, 0);
    }

    public int secondsRemaining() {
        return Math.max(0, (phaseTicksRemaining + 19) / 20);
    }

    public int balanceSecondsRemaining() {
        return balanceTicksRemaining < 0 ? 0 : Math.max(1, (balanceTicksRemaining + 19) / 20);
    }

    public int onlineTeamSize(RoseTeam team) {
        return onlineTeamMembers(team).size();
    }

    public String activeMapId() {
        return RoseSavedData.get(server).activeMapId();
    }

    public List<String> mapVoteOptions() {
        return List.copyOf(mapVoteOptions);
    }

    public List<Integer> mapVoteCounts() {
        List<Integer> counts = new ArrayList<>(mapVoteOptions.size());
        for (String option : mapVoteOptions) {
            counts.add((int) mapVotes.values().stream().filter(option::equals).count());
        }
        return counts;
    }

    public int selectedMapVote(UUID playerId) {
        String selected = mapVotes.get(playerId);
        return selected == null ? -1 : mapVoteOptions.indexOf(selected);
    }

    public boolean castMapVote(ServerPlayer player, String requestedMapId) {
        String mapId = RoseSavedData.normalizeMapId(requestedMapId);
        if (phase != MatchPhase.MAP_VOTE || !mapVoteOptions.contains(mapId)) {
            return false;
        }
        mapVotes.put(player.getUUID(), mapId);
        player.sendSystemMessage(Component.literal("Voted for map: " + mapId.toUpperCase())
                .withStyle(ChatFormatting.GOLD));
        RoseNetwork.syncAll(server, this);
        return true;
    }

    public boolean isPostMatchInvulnerable(ServerPlayer player) {
        return isParticipant(player) && (phase == MatchPhase.COUNTDOWN
                || phase == MatchPhase.POST_MATCH
                || phase == MatchPhase.MAP_VOTE);
    }

    public PlayerSession session(UUID playerId) {
        return sessions.computeIfAbsent(playerId, id -> new PlayerSession(id, joinSequence++));
    }

    public Optional<PlayerSession> existingSession(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    public boolean isParticipant(ServerPlayer player) {
        return existingSession(player.getUUID()).map(value -> value.team().isPlayable()).orElse(false);
    }

    public boolean areTeammates(ServerPlayer first, ServerPlayer second) {
        RoseTeam firstTeam = session(first.getUUID()).team();
        RoseTeam secondTeam = session(second.getUUID()).team();
        return firstTeam.isPlayable() && firstTeam == secondTeam;
    }

    public boolean chooseTeam(ServerPlayer player, RoseTeam team) {
        if (!team.isPlayable()) {
            return false;
        }
        PlayerSession session = session(player.getUUID());
        boolean waitingSelection = phase == MatchPhase.WAITING;
        boolean joiningActiveMatch = phase == MatchPhase.ACTIVE && !session.team().isPlayable();
        if (!waitingSelection && !joiningActiveMatch) {
            return false;
        }
        setScoreboardTeam(player, session.team(), team);
        session.setTeam(team);
        balanceTicksRemaining = -1;
        player.sendSystemMessage(Component.literal("Joined ").append(team.displayName()).append(" team."));
        deployJoinInProgressIfReady(player, session);
        return true;
    }

    public boolean selectLoadout(ServerPlayer player, int index) {
        if (index < 0 || index >= 5 || RoseSavedData.get(server).loadout(index).isEmpty()) {
            return false;
        }
        session(player.getUUID()).setLoadoutIndex(index);
        LoadoutPreset preset = RoseSavedData.get(server).loadout(index).orElseThrow();
        player.sendSystemMessage(Component.literal("Selected class " + (index + 1) + ": " + preset.name())
                .withStyle(ChatFormatting.GOLD));
        deployJoinInProgressIfReady(player, session(player.getUUID()));
        return true;
    }

    public Optional<Component> validateStart() {
        RoseSavedData data = RoseSavedData.get(server);
        if (phase != MatchPhase.WAITING) {
            return Optional.of(Component.literal("A match is already running."));
        }
        if (!data.arena().isReady()) {
            return Optional.of(Component.literal("The arena needs at least one Red and one Blue spawn."));
        }
        if (data.configuredLoadoutCount() == 0) {
            return Optional.of(Component.literal("At least one administrator loadout must be saved."));
        }
        if (onlineTeamMembers(RoseTeam.RED).isEmpty() || onlineTeamMembers(RoseTeam.BLUE).isEmpty()) {
            return Optional.of(Component.literal("Both teams need at least one online player."));
        }
        for (ServerPlayer player : onlineParticipants()) {
            if (session(player.getUUID()).loadoutIndex() < 0) {
                return Optional.of(Component.literal(player.getGameProfile().getName() + " has not selected a class."));
            }
        }
        return Optional.empty();
    }

    public boolean startMatch() {
        Optional<Component> validation = validateStart();
        if (validation.isPresent()) {
            return false;
        }
        resetScoresAndStatistics();
        spawnProtectionEndTicks.clear();
        mapVotes.clear();
        mapVoteOptions = List.of();
        nextMapAutoStartPending = false;
        nextMapTeamSelectionTicks = 0;
        phase = MatchPhase.COUNTDOWN;
        phaseTicksRemaining = RoseConfig.START_COUNTDOWN_SECONDS.get() * 20;
        balanceTicksRemaining = -1;
        broadcast(Component.literal("TEAM DEATHMATCH").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        if (phaseTicksRemaining == 0) {
            beginActiveMatch();
        } else {
            broadcast(Component.literal("Match begins in " + secondsRemaining() + " seconds.")
                    .withStyle(ChatFormatting.YELLOW));
        }
        return true;
    }

    public void stopMatch() {
        phase = MatchPhase.WAITING;
        winner = RoseTeam.NONE;
        phaseTicksRemaining = 0;
        balanceTicksRemaining = -1;
        resetScoresAndStatistics();
        mapVotes.clear();
        mapVoteOptions = List.of();
        nextMapAutoStartPending = false;
        nextMapTeamSelectionTicks = 0;
        broadcast(Component.literal("The match was stopped by an administrator.")
                .withStyle(ChatFormatting.RED));
    }

    public void tick() {
        switch (phase) {
            case WAITING -> tickWaiting();
            case COUNTDOWN -> tickCountdown();
            case ACTIVE -> tickActive();
            case POST_MATCH -> tickPostMatch();
            case MAP_VOTE -> tickMapVote();
        }
        snapshotSyncTicks++;
        if (snapshotSyncTicks >= 5) {
            snapshotSyncTicks = 0;
            RoseNetwork.syncAll(server, this);
        }
    }

    public void recordDeath(
            ServerPlayer victim,
            ServerPlayer attacker,
            ResourceLocation gunId,
            boolean headshot
    ) {
        if (phase != MatchPhase.ACTIVE || !isParticipant(victim)) {
            return;
        }
        PlayerSession victimSession = session(victim.getUUID());
        int currentTick = server.getTickCount();
        if (victimSession.lastRecordedDeathTick() == currentTick) {
            return;
        }
        victimSession.setLastRecordedDeathTick(currentTick);
        victimSession.addDeath();

        if (attacker == null || attacker == victim || !isParticipant(attacker)) {
            broadcastKill(victim, null, gunId, headshot);
            return;
        }

        PlayerSession attackerSession = session(attacker.getUUID());
        if (attackerSession.team() == victimSession.team()) {
            return;
        }

        attackerSession.addKill();
        scores.compute(attackerSession.team(), (team, value) ->
                (value == null ? 0 : value) + RoseConfig.POINTS_PER_KILL.get());
        broadcastKill(victim, attacker, gunId, headshot);

        if (score(attackerSession.team()) >= RoseConfig.SCORE_LIMIT.get()) {
            finishMatch(attackerSession.team());
        }
    }

    public void prepareRespawn(ServerPlayer player) {
        PlayerSession session = sessions.get(player.getUUID());
        if (session == null || !session.team().isPlayable()) {
            return;
        }

        RoseSavedData data = RoseSavedData.get(server);
        data.loadout(session.loadoutIndex()).ifPresent(preset -> preset.apply(player));
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        player.getFoodData().setExhaustion(0.0F);
        player.removeAllEffects();
        player.clearFire();
        player.setAirSupply(player.getMaxAirSupply());
        player.fallDistance = 0.0F;
        player.setGameMode(GameType.ADVENTURE);
        teleportToSafeSpawn(player, session.team(), data.arena());
        session.setDeployedThisMatch(true);
        int protectionTicks = RoseConfig.SPAWN_PROTECTION_SECONDS.get() * 20;
        if (phase == MatchPhase.ACTIVE && protectionTicks > 0) {
            spawnProtectionEndTicks.put(player.getUUID(), server.getTickCount() + protectionTicks);
        } else {
            spawnProtectionEndTicks.remove(player.getUUID());
        }
    }

    public void handleLogin(ServerPlayer player) {
        PlayerSession session = session(player.getUUID());
        if (phase == MatchPhase.POST_MATCH || phase == MatchPhase.MAP_VOTE) {
            if (!session.team().isPlayable()) {
                player.setGameMode(GameType.SPECTATOR);
            }
            return;
        }
        if (phase != MatchPhase.ACTIVE) {
            if (!session.team().isPlayable()) {
                player.setGameMode(GameType.SPECTATOR);
            }
            return;
        }
        if (session.team().isPlayable() && session.loadoutIndex() >= 0) {
            prepareRespawn(player);
        } else {
            player.setGameMode(GameType.SPECTATOR);
        }
    }

    public boolean hasSpawnProtection(ServerPlayer player) {
        Integer endTick = spawnProtectionEndTicks.get(player.getUUID());
        if (endTick == null) {
            return false;
        }
        if (server.getTickCount() >= endTick || phase != MatchPhase.ACTIVE) {
            spawnProtectionEndTicks.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public int spawnProtectionSeconds(ServerPlayer player) {
        Integer endTick = spawnProtectionEndTicks.get(player.getUUID());
        if (endTick == null) {
            return 0;
        }
        int remaining = endTick - server.getTickCount();
        if (remaining <= 0 || phase != MatchPhase.ACTIVE) {
            spawnProtectionEndTicks.remove(player.getUUID());
            return 0;
        }
        return Math.max(1, (remaining + 19) / 20);
    }

    public void removeSpawnProtection(ServerPlayer player) {
        spawnProtectionEndTicks.remove(player.getUUID());
    }

    private void tickWaiting() {
        tickTeamBalancing();
        if (nextMapAutoStartPending) {
            if (nextMapTeamSelectionTicks > 0) {
                nextMapTeamSelectionTicks--;
            }
            if (nextMapTeamSelectionTicks <= 0 && validateStart().isEmpty()) {
                startMatch();
            }
        } else if (RoseConfig.AUTO_START.get() && validateStart().isEmpty()) {
            startMatch();
        }
    }

    private void tickCountdown() {
        if (phaseTicksRemaining > 0) {
            phaseTicksRemaining--;
        }
        if (phaseTicksRemaining <= 0) {
            beginActiveMatch();
        }
    }

    private void tickActive() {
        if (phaseTicksRemaining > 0) {
            phaseTicksRemaining--;
        }
        if (phaseTicksRemaining <= 0) {
            int red = score(RoseTeam.RED);
            int blue = score(RoseTeam.BLUE);
            finishMatch(red == blue ? RoseTeam.NONE : red > blue ? RoseTeam.RED : RoseTeam.BLUE);
        }
    }

    private void tickPostMatch() {
        if (phaseTicksRemaining > 0) {
            phaseTicksRemaining--;
        }
        if (phaseTicksRemaining <= 0) {
            beginMapVote();
        }
    }

    private void tickMapVote() {
        if (phaseTicksRemaining > 0) {
            phaseTicksRemaining--;
        }
        if (phaseTicksRemaining <= 0) {
            resolveMapVote();
        }
    }

    private void beginActiveMatch() {
        phase = MatchPhase.ACTIVE;
        phaseTicksRemaining = RoseConfig.TIME_LIMIT_SECONDS.get() * 20;
        broadcast(Component.literal("MATCH START").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        onlineParticipants().forEach(this::prepareRespawn);
    }

    private void finishMatch(RoseTeam winningTeam) {
        if (phase != MatchPhase.ACTIVE) {
            return;
        }
        phase = MatchPhase.POST_MATCH;
        winner = winningTeam;
        phaseTicksRemaining = RoseConfig.POST_MATCH_SECONDS.get() * 20;
        spawnProtectionEndTicks.clear();
        if (winningTeam.isPlayable()) {
            broadcast(winningTeam.displayName().copy().append(" TEAM WINS!")
                    .withStyle(ChatFormatting.BOLD));
        } else {
            broadcast(Component.literal("DRAW").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
        broadcast(Component.literal("Final score: Red " + score(RoseTeam.RED)
                + " - " + score(RoseTeam.BLUE) + " Blue"));
    }

    private void beginMapVote() {
        RoseSavedData data = RoseSavedData.get(server);
        List<String> readyMaps = data.mapIds().stream()
                .filter(mapId -> data.arena(mapId).map(ArenaDefinition::isReady).orElse(false))
                .toList();
        if (readyMaps.isEmpty()) {
            phase = MatchPhase.WAITING;
            winner = RoseTeam.NONE;
            broadcast(Component.literal("No configured map is ready. An administrator must configure one.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        int optionLimit = Math.min(RoseConfig.MAP_VOTE_OPTIONS.get(), readyMaps.size());
        int activeIndex = Math.max(0, readyMaps.indexOf(data.activeMapId()));
        List<String> options = new ArrayList<>(optionLimit);
        for (int offset = 0; offset < readyMaps.size() && options.size() < optionLimit; offset++) {
            options.add(readyMaps.get((activeIndex + offset) % readyMaps.size()));
        }
        mapVoteOptions = List.copyOf(options);
        mapVotes.clear();
        phase = MatchPhase.MAP_VOTE;
        phaseTicksRemaining = RoseConfig.MAP_VOTE_SECONDS.get() * 20;
        broadcast(Component.literal("CHOOSE NEXT MAP").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        RoseNetwork.syncAll(server, this);
        server.getPlayerList().getPlayers().forEach(RoseNetwork::openMapVote);
    }

    private void resolveMapVote() {
        if (mapVoteOptions.isEmpty()) {
            phase = MatchPhase.WAITING;
            winner = RoseTeam.NONE;
            return;
        }

        List<Integer> counts = mapVoteCounts();
        int highest = counts.stream().max(Integer::compareTo).orElse(0);
        List<String> tiedWinners = new ArrayList<>();
        for (int index = 0; index < mapVoteOptions.size(); index++) {
            if (counts.get(index) == highest) {
                tiedWinners.add(mapVoteOptions.get(index));
            }
        }
        String selected = tiedWinners.get(Math.floorMod(server.getTickCount(), tiedWinners.size()));
        RoseSavedData.get(server).selectMap(selected);
        broadcast(Component.literal("NEXT MAP: " + selected.toUpperCase())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        phase = MatchPhase.WAITING;
        winner = RoseTeam.NONE;
        mapVotes.clear();
        mapVoteOptions = List.of();
        resetTeamsForNextMap();
        nextMapAutoStartPending = true;
        nextMapTeamSelectionTicks = RoseConfig.NEXT_MAP_TEAM_SELECTION_SECONDS.get() * 20;
        broadcast(Component.literal("CHOOSE YOUR TEAM // NEXT MATCH PREPARING")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        RoseNetwork.syncAll(server, this);
        server.getPlayerList().getPlayers().forEach(RoseNetwork::openLobby);
    }

    private void tickTeamBalancing() {
        if (!RoseConfig.BALANCING_ENABLED.get()) {
            balanceTicksRemaining = -1;
            return;
        }
        List<ServerPlayer> red = onlineTeamMembers(RoseTeam.RED);
        List<ServerPlayer> blue = onlineTeamMembers(RoseTeam.BLUE);
        int largerSize = Math.max(red.size(), blue.size());
        int gap = Math.abs(red.size() - blue.size());
        boolean eligible = largerSize >= RoseConfig.BALANCE_MIN_LARGER_TEAM_SIZE.get()
                && gap >= RoseConfig.BALANCE_MIN_SIZE_GAP.get();
        RoseTeam largerTeam = red.size() > blue.size() ? RoseTeam.RED : RoseTeam.BLUE;
        boolean hasMovablePlayer = onlineTeamMembers(largerTeam).stream()
                .anyMatch(player -> !player.hasPermissions(2));
        if (!eligible || !hasMovablePlayer) {
            balanceTicksRemaining = -1;
            return;
        }

        if (balanceTicksRemaining < 0) {
            balanceTicksRemaining = RoseConfig.BALANCE_WARNING_SECONDS.get() * 20;
            broadcastBalanceWarning();
            return;
        }

        balanceTicksRemaining--;
        if (balanceTicksRemaining > 0 && balanceTicksRemaining % 20 == 0) {
            broadcastBalanceWarning();
        }
        if (balanceTicksRemaining <= 0) {
            rebalance(red, blue);
            balanceTicksRemaining = -1;
        }
    }

    private void broadcastBalanceWarning() {
        broadcast(Component.literal("FORCED TEAM BALANCING IN "
                        + Math.max(1, (balanceTicksRemaining + 19) / 20) + " SECONDS")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
    }

    private void rebalance(List<ServerPlayer> red, List<ServerPlayer> blue) {
        RoseTeam largerTeam = red.size() > blue.size() ? RoseTeam.RED : RoseTeam.BLUE;
        List<ServerPlayer> candidates = new ArrayList<>(largerTeam == RoseTeam.RED ? red : blue);
        candidates.removeIf(player -> player.hasPermissions(2));
        candidates.sort(Comparator.comparingLong(player -> -session(player.getUUID()).joinOrder()));

        int difference = Math.abs(red.size() - blue.size());
        int playersToMove = difference / 2;
        for (int index = 0; index < playersToMove && index < candidates.size(); index++) {
            ServerPlayer player = candidates.get(index);
            RoseTeam destination = largerTeam.opposite();
            chooseTeam(player, destination);
            player.sendSystemMessage(Component.literal("You were moved to ")
                    .append(destination.displayName())
                    .append(" to balance the match.")
                    .withStyle(ChatFormatting.RED));
        }
    }

    private void teleportToSafeSpawn(ServerPlayer player, RoseTeam team, ArenaDefinition arena) {
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, arena.dimension());
        ServerLevel level = server.getLevel(dimensionKey);
        if (level == null) {
            return;
        }
        List<SpawnPoint> spawns = arena.spawns(team);
        if (spawns.isEmpty()) {
            return;
        }

        List<ServerPlayer> enemies = onlineTeamMembers(team.opposite());
        SpawnPoint safest = spawns.stream()
                .max(Comparator.comparingDouble(spawn -> minimumEnemyDistanceSquared(spawn, enemies, level)))
                .orElse(spawns.get(0));
        player.teleportTo(
                level,
                safest.position().getX() + 0.5D,
                safest.position().getY(),
                safest.position().getZ() + 0.5D,
                safest.yaw(),
                safest.pitch()
        );
    }

    private double minimumEnemyDistanceSquared(SpawnPoint spawn, List<ServerPlayer> enemies, ServerLevel level) {
        double minimum = Double.MAX_VALUE;
        for (ServerPlayer enemy : enemies) {
            if (enemy.serverLevel() == level) {
                minimum = Math.min(minimum, enemy.blockPosition().distSqr(spawn.position()));
            }
        }
        return minimum;
    }

    private List<ServerPlayer> onlineParticipants() {
        return server.getPlayerList().getPlayers().stream().filter(this::isParticipant).toList();
    }

    private List<ServerPlayer> onlineTeamMembers(RoseTeam team) {
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> existingSession(player.getUUID())
                        .map(value -> value.team() == team)
                        .orElse(false))
                .toList();
    }

    private void ensureScoreboardTeams() {
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam red = scoreboard.getPlayerTeam(RED_SCOREBOARD_TEAM);
        if (red == null) {
            red = scoreboard.addPlayerTeam(RED_SCOREBOARD_TEAM);
        }
        red.setColor(ChatFormatting.RED);
        red.setAllowFriendlyFire(RoseConfig.FRIENDLY_FIRE.get());

        PlayerTeam blue = scoreboard.getPlayerTeam(BLUE_SCOREBOARD_TEAM);
        if (blue == null) {
            blue = scoreboard.addPlayerTeam(BLUE_SCOREBOARD_TEAM);
        }
        blue.setColor(ChatFormatting.BLUE);
        blue.setAllowFriendlyFire(RoseConfig.FRIENDLY_FIRE.get());
    }

    private void setScoreboardTeam(ServerPlayer player, RoseTeam oldTeam, RoseTeam newTeam) {
        Scoreboard scoreboard = server.getScoreboard();
        if (oldTeam.isPlayable()) {
            PlayerTeam existing = scoreboard.getPlayerTeam(scoreboardName(oldTeam));
            if (existing != null) {
                scoreboard.removePlayerFromTeam(player.getScoreboardName(), existing);
            }
        }
        if (newTeam.isPlayable()) {
            PlayerTeam destination = scoreboard.getPlayerTeam(scoreboardName(newTeam));
            if (destination != null) {
                scoreboard.addPlayerToTeam(player.getScoreboardName(), destination);
            }
        }
    }

    private void resetTeamsForNextMap() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerSession playerSession = session(player.getUUID());
            setScoreboardTeam(player, playerSession.team(), RoseTeam.NONE);
            player.setGameMode(GameType.SPECTATOR);
        }
        sessions.values().forEach(playerSession -> playerSession.setTeam(RoseTeam.NONE));
        resetScoresAndStatistics();
        balanceTicksRemaining = -1;
    }

    private String scoreboardName(RoseTeam team) {
        return team == RoseTeam.RED ? RED_SCOREBOARD_TEAM : BLUE_SCOREBOARD_TEAM;
    }

    private void resetScoresAndStatistics() {
        scores.put(RoseTeam.RED, 0);
        scores.put(RoseTeam.BLUE, 0);
        sessions.values().forEach(PlayerSession::resetStatistics);
        spawnProtectionEndTicks.clear();
    }

    private void deployJoinInProgressIfReady(ServerPlayer player, PlayerSession session) {
        if (phase == MatchPhase.ACTIVE
                && session.team().isPlayable()
                && session.loadoutIndex() >= 0
                && !session.deployedThisMatch()) {
            prepareRespawn(player);
        }
    }

    private void broadcastKill(
            ServerPlayer victim,
            ServerPlayer attacker,
            ResourceLocation gunId,
            boolean headshot
    ) {
        Component message;
        if (attacker == null) {
            message = Component.literal(victim.getGameProfile().getName() + " died");
        } else {
            String weapon = gunId == null ? "" : " [" + gunId + "]";
            String headshotLabel = headshot ? " HEADSHOT" : "";
            message = Component.literal(attacker.getGameProfile().getName() + weapon + " > "
                    + victim.getGameProfile().getName() + headshotLabel);
        }
        broadcast(message.copy().withStyle(ChatFormatting.GRAY));
    }

    private void broadcast(Component message) {
        server.getPlayerList().broadcastSystemMessage(message, false);
    }
}
