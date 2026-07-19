package com.synergy902.projectrose.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.synergy902.projectrose.data.RoseSavedData;
import com.synergy902.projectrose.game.ArenaDefinition;
import com.synergy902.projectrose.game.MatchManager;
import com.synergy902.projectrose.game.RoseTeam;
import com.synergy902.projectrose.game.MatchPhase;
import com.synergy902.projectrose.game.SpawnPoint;
import com.synergy902.projectrose.loadout.LoadoutPreset;
import com.synergy902.projectrose.network.RoseNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class RoseCommands {
    private RoseCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rose")
                .then(Commands.literal("menu").executes(RoseCommands::openMenu))
                .then(Commands.literal("vote").executes(RoseCommands::openVote))
                .then(Commands.literal("join")
                        .then(Commands.literal("red").executes(context -> join(context, RoseTeam.RED)))
                        .then(Commands.literal("blue").executes(context -> join(context, RoseTeam.BLUE))))
                .then(Commands.literal("class")
                        .then(Commands.literal("select")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 5))
                                        .executes(RoseCommands::selectClass)))
                        .then(Commands.literal("save")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 5))
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(RoseCommands::saveClass))))
                        .then(Commands.literal("clear")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 5))
                                        .executes(RoseCommands::clearClass)))
                        .then(Commands.literal("apply")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 5))
                                        .executes(RoseCommands::applyClass))))
                .then(Commands.literal("arena")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("setlobby").executes(RoseCommands::setLobby))
                        .then(Commands.literal("setspectator").executes(RoseCommands::setSpectator))
                        .then(Commands.literal("setcorner1").executes(context -> setCorner(context, true)))
                        .then(Commands.literal("setcorner2").executes(context -> setCorner(context, false)))
                        .then(Commands.literal("addspawn")
                                .then(Commands.literal("red").executes(context -> addSpawn(context, RoseTeam.RED)))
                                .then(Commands.literal("blue").executes(context -> addSpawn(context, RoseTeam.BLUE))))
                        .then(Commands.literal("clearspawns")
                                .then(Commands.literal("red").executes(context -> clearSpawns(context, RoseTeam.RED)))
                                .then(Commands.literal("blue").executes(context -> clearSpawns(context, RoseTeam.BLUE))))
                        .then(Commands.literal("status").executes(RoseCommands::arenaStatus)))
                .then(Commands.literal("map")
                        .then(Commands.literal("list").executes(RoseCommands::listMaps))
                        .then(Commands.literal("create")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(RoseCommands::createMap)))
                        .then(Commands.literal("select")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(RoseCommands::selectMap))))
                .then(Commands.literal("match")
                        .then(Commands.literal("status").executes(RoseCommands::matchStatus))
                        .then(Commands.literal("start")
                                .requires(source -> source.hasPermission(2))
                                .executes(RoseCommands::startMatch))
                        .then(Commands.literal("stop")
                                .requires(source -> source.hasPermission(2))
                                .executes(RoseCommands::stopMatch)))
                .executes(RoseCommands::matchStatus));
    }

    private static int openMenu(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        RoseNetwork.openLobby(player);
        return 1;
    }

    private static int openVote(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MatchManager manager = MatchManager.get(context.getSource().getServer());
        if (manager.phase() != MatchPhase.MAP_VOTE) {
            context.getSource().sendFailure(Component.literal("There is no active map vote."));
            return 0;
        }
        RoseNetwork.openMapVote(player);
        return 1;
    }

    private static int join(CommandContext<CommandSourceStack> context, RoseTeam team) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MatchManager manager = MatchManager.get(context.getSource().getServer());
        if (!manager.chooseTeam(player, team)) {
            context.getSource().sendFailure(Component.literal(
                    "You cannot switch teams now. Join-in-progress is available only to unassigned players."));
            return 0;
        }
        return 1;
    }

    private static int selectClass(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int slot = IntegerArgumentType.getInteger(context, "slot") - 1;
        MatchManager manager = MatchManager.get(context.getSource().getServer());
        if (!manager.selectLoadout(player, slot)) {
            context.getSource().sendFailure(Component.literal("That class has not been configured."));
            return 0;
        }
        return 1;
    }

    private static int saveClass(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int slot = IntegerArgumentType.getInteger(context, "slot") - 1;
        String name = StringArgumentType.getString(context, "name");
        RoseSavedData data = RoseSavedData.get(context.getSource().getServer());
        data.setLoadout(slot, LoadoutPreset.capture(player, name));
        success(context, "Saved the complete current inventory as class " + (slot + 1) + ": " + name, true);
        return 1;
    }

    private static int clearClass(CommandContext<CommandSourceStack> context) {
        int slot = IntegerArgumentType.getInteger(context, "slot") - 1;
        RoseSavedData.get(context.getSource().getServer()).clearLoadout(slot);
        success(context, "Cleared class " + (slot + 1) + ".", true);
        return 1;
    }

    private static int applyClass(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int slot = IntegerArgumentType.getInteger(context, "slot") - 1;
        ServerPlayer player = context.getSource().getPlayerOrException();
        return RoseSavedData.get(context.getSource().getServer()).loadout(slot)
                .map(preset -> {
                    preset.apply(player);
                    success(context, "Applied class " + (slot + 1) + ": " + preset.name(), false);
                    return 1;
                })
                .orElseGet(() -> {
                    context.getSource().sendFailure(Component.literal("That class has not been configured."));
                    return 0;
                });
    }

    private static int setLobby(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        RoseSavedData data = RoseSavedData.get(context.getSource().getServer());
        configureDimension(data.arena(), player);
        data.arena().setLobby(currentSpawn(player));
        data.arenaChanged();
        success(context, "Set the arena lobby.", true);
        return 1;
    }

    private static int setSpectator(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        RoseSavedData data = RoseSavedData.get(context.getSource().getServer());
        configureDimension(data.arena(), player);
        data.arena().setSpectator(currentSpawn(player));
        data.arenaChanged();
        success(context, "Set the spectator position.", true);
        return 1;
    }

    private static int setCorner(CommandContext<CommandSourceStack> context, boolean first) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        RoseSavedData data = RoseSavedData.get(context.getSource().getServer());
        configureDimension(data.arena(), player);
        if (first) {
            data.arena().setCornerOne(player.blockPosition());
        } else {
            data.arena().setCornerTwo(player.blockPosition());
        }
        data.arenaChanged();
        success(context, "Set arena corner " + (first ? "one" : "two") + ".", true);
        return 1;
    }

    private static int addSpawn(CommandContext<CommandSourceStack> context, RoseTeam team) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        RoseSavedData data = RoseSavedData.get(context.getSource().getServer());
        configureDimension(data.arena(), player);
        data.arena().addSpawn(team, currentSpawn(player));
        data.arenaChanged();
        success(context, "Added a " + team.serializedName() + " spawn.", true);
        return 1;
    }

    private static int clearSpawns(CommandContext<CommandSourceStack> context, RoseTeam team) {
        RoseSavedData data = RoseSavedData.get(context.getSource().getServer());
        data.arena().clearSpawns(team);
        data.arenaChanged();
        success(context, "Cleared all " + team.serializedName() + " spawns.", true);
        return 1;
    }

    private static int arenaStatus(CommandContext<CommandSourceStack> context) {
        RoseSavedData data = RoseSavedData.get(context.getSource().getServer());
        ArenaDefinition arena = data.arena();
        success(context, "Active map: " + data.activeMapId()
                + " | Arena dimension: " + arena.dimension()
                + " | Red spawns: " + arena.spawns(RoseTeam.RED).size()
                + " | Blue spawns: " + arena.spawns(RoseTeam.BLUE).size()
                + " | Classes: " + data.configuredLoadoutCount() + "/5"
                + " | Ready: " + arena.isReady(), false);
        return 1;
    }

    private static int createMap(CommandContext<CommandSourceStack> context) {
        MatchManager manager = MatchManager.get(context.getSource().getServer());
        if (manager.phase() != MatchPhase.WAITING) {
            context.getSource().sendFailure(Component.literal("Maps can be created only while waiting."));
            return 0;
        }
        String requested = StringArgumentType.getString(context, "id");
        RoseSavedData data = RoseSavedData.get(context.getSource().getServer());
        if (!data.createMap(requested)) {
            context.getSource().sendFailure(Component.literal("That map ID is invalid or already exists."));
            return 0;
        }
        success(context, "Created and selected map: " + data.activeMapId(), true);
        return 1;
    }

    private static int selectMap(CommandContext<CommandSourceStack> context) {
        MatchManager manager = MatchManager.get(context.getSource().getServer());
        if (manager.phase() != MatchPhase.WAITING) {
            context.getSource().sendFailure(Component.literal("The active map can be changed only while waiting."));
            return 0;
        }
        String requested = StringArgumentType.getString(context, "id");
        RoseSavedData data = RoseSavedData.get(context.getSource().getServer());
        if (!data.selectMap(requested)) {
            context.getSource().sendFailure(Component.literal("Unknown map: " + requested));
            return 0;
        }
        success(context, "Selected active map: " + data.activeMapId(), true);
        return 1;
    }

    private static int listMaps(CommandContext<CommandSourceStack> context) {
        RoseSavedData data = RoseSavedData.get(context.getSource().getServer());
        success(context, "Maps (" + data.configuredMapCount() + "): "
                + String.join(", ", data.mapIds()) + " | Active: " + data.activeMapId(), false);
        return 1;
    }

    private static int startMatch(CommandContext<CommandSourceStack> context) {
        MatchManager manager = MatchManager.get(context.getSource().getServer());
        var validation = manager.validateStart();
        if (validation.isPresent()) {
            context.getSource().sendFailure(validation.get());
            return 0;
        }
        manager.startMatch();
        success(context, "Starting Team Deathmatch.", true);
        return 1;
    }

    private static int stopMatch(CommandContext<CommandSourceStack> context) {
        MatchManager.get(context.getSource().getServer()).stopMatch();
        success(context, "Stopped the match.", true);
        return 1;
    }

    private static int matchStatus(CommandContext<CommandSourceStack> context) {
        MatchManager manager = MatchManager.get(context.getSource().getServer());
        success(context, "Phase: " + manager.phase()
                + " | Map: " + manager.activeMapId()
                + " | Red " + manager.score(RoseTeam.RED)
                + " - " + manager.score(RoseTeam.BLUE) + " Blue"
                + " | Time: " + manager.secondsRemaining() + "s", false);
        return 1;
    }

    private static SpawnPoint currentSpawn(ServerPlayer player) {
        return new SpawnPoint(player.blockPosition(), player.getYRot(), player.getXRot());
    }

    private static void configureDimension(ArenaDefinition arena, ServerPlayer player) {
        arena.setDimension(player.level().dimension().location());
    }

    private static void success(CommandContext<CommandSourceStack> context, String message, boolean broadcast) {
        context.getSource().sendSuccess(
                () -> Component.literal(message).withStyle(ChatFormatting.GREEN),
                broadcast
        );
    }
}
