package com.synergy902.projectrose.game;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public final class ArenaDefinition {
    private ResourceLocation dimension = Level.OVERWORLD.location();
    private SpawnPoint lobby;
    private SpawnPoint spectator;
    private BlockPos cornerOne;
    private BlockPos cornerTwo;
    private final EnumMap<RoseTeam, List<SpawnPoint>> spawns = new EnumMap<>(RoseTeam.class);

    public ArenaDefinition() {
        spawns.put(RoseTeam.RED, new ArrayList<>());
        spawns.put(RoseTeam.BLUE, new ArrayList<>());
    }

    public ResourceLocation dimension() {
        return dimension;
    }

    public void setDimension(ResourceLocation dimension) {
        this.dimension = dimension;
    }

    public SpawnPoint lobby() {
        return lobby;
    }

    public void setLobby(SpawnPoint lobby) {
        this.lobby = lobby;
    }

    public SpawnPoint spectator() {
        return spectator;
    }

    public void setSpectator(SpawnPoint spectator) {
        this.spectator = spectator;
    }

    public BlockPos cornerOne() {
        return cornerOne;
    }

    public void setCornerOne(BlockPos cornerOne) {
        this.cornerOne = cornerOne;
    }

    public BlockPos cornerTwo() {
        return cornerTwo;
    }

    public void setCornerTwo(BlockPos cornerTwo) {
        this.cornerTwo = cornerTwo;
    }

    public List<SpawnPoint> spawns(RoseTeam team) {
        return Collections.unmodifiableList(spawns.getOrDefault(team, List.of()));
    }

    public void addSpawn(RoseTeam team, SpawnPoint spawn) {
        if (team.isPlayable()) {
            spawns.get(team).add(spawn);
        }
    }

    public void clearSpawns(RoseTeam team) {
        if (team.isPlayable()) {
            spawns.get(team).clear();
        }
    }

    public boolean isReady() {
        return !spawns.get(RoseTeam.RED).isEmpty() && !spawns.get(RoseTeam.BLUE).isEmpty();
    }

    public boolean contains(ServerPlayer player) {
        if (cornerOne == null || cornerTwo == null || !player.level().dimension().location().equals(dimension)) {
            return true;
        }
        BlockPos pos = player.blockPosition();
        int minX = Math.min(cornerOne.getX(), cornerTwo.getX());
        int minY = Math.min(cornerOne.getY(), cornerTwo.getY());
        int minZ = Math.min(cornerOne.getZ(), cornerTwo.getZ());
        int maxX = Math.max(cornerOne.getX(), cornerTwo.getX());
        int maxY = Math.max(cornerOne.getY(), cornerTwo.getY());
        int maxZ = Math.max(cornerOne.getZ(), cornerTwo.getZ());
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", dimension.toString());
        putSpawn(tag, "lobby", lobby);
        putSpawn(tag, "spectator", spectator);
        if (cornerOne != null) {
            tag.putLong("cornerOne", cornerOne.asLong());
        }
        if (cornerTwo != null) {
            tag.putLong("cornerTwo", cornerTwo.asLong());
        }
        tag.put("redSpawns", saveSpawns(spawns.get(RoseTeam.RED)));
        tag.put("blueSpawns", saveSpawns(spawns.get(RoseTeam.BLUE)));
        return tag;
    }

    public static ArenaDefinition load(CompoundTag tag) {
        ArenaDefinition arena = new ArenaDefinition();
        if (tag.contains("dimension", Tag.TAG_STRING)) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("dimension"));
            if (parsed != null) {
                arena.dimension = parsed;
            }
        }
        arena.lobby = getSpawn(tag, "lobby");
        arena.spectator = getSpawn(tag, "spectator");
        if (tag.contains("cornerOne", Tag.TAG_LONG)) {
            arena.cornerOne = BlockPos.of(tag.getLong("cornerOne"));
        }
        if (tag.contains("cornerTwo", Tag.TAG_LONG)) {
            arena.cornerTwo = BlockPos.of(tag.getLong("cornerTwo"));
        }
        loadSpawns(tag.getList("redSpawns", Tag.TAG_COMPOUND), arena.spawns.get(RoseTeam.RED));
        loadSpawns(tag.getList("blueSpawns", Tag.TAG_COMPOUND), arena.spawns.get(RoseTeam.BLUE));
        return arena;
    }

    private static void putSpawn(CompoundTag tag, String key, SpawnPoint spawn) {
        if (spawn != null) {
            tag.put(key, spawn.save());
        }
    }

    private static SpawnPoint getSpawn(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_COMPOUND) ? SpawnPoint.load(tag.getCompound(key)) : null;
    }

    private static ListTag saveSpawns(List<SpawnPoint> values) {
        ListTag list = new ListTag();
        values.forEach(spawn -> list.add(spawn.save()));
        return list;
    }

    private static void loadSpawns(ListTag tags, List<SpawnPoint> destination) {
        for (int i = 0; i < tags.size(); i++) {
            destination.add(SpawnPoint.load(tags.getCompound(i)));
        }
    }
}

