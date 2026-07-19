package com.synergy902.projectrose.data;

import com.synergy902.projectrose.game.ArenaDefinition;
import com.synergy902.projectrose.loadout.LoadoutPreset;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class RoseSavedData extends SavedData {
    private static final String DATA_NAME = "projectrose";
    private static final int LOADOUT_COUNT = 5;

    private final LinkedHashMap<String, ArenaDefinition> arenas = new LinkedHashMap<>();
    private String activeMapId = "default";
    private final LoadoutPreset[] loadouts = new LoadoutPreset[LOADOUT_COUNT];

    public RoseSavedData() {
        arenas.put(activeMapId, new ArenaDefinition());
    }

    public static RoseSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                RoseSavedData::load,
                RoseSavedData::new,
                DATA_NAME
        );
    }

    public ArenaDefinition arena() {
        return arenas.computeIfAbsent(activeMapId, ignored -> new ArenaDefinition());
    }

    public Optional<ArenaDefinition> arena(String mapId) {
        return Optional.ofNullable(arenas.get(normalizeMapId(mapId)));
    }

    public String activeMapId() {
        return activeMapId;
    }

    public List<String> mapIds() {
        return List.copyOf(arenas.keySet());
    }

    public boolean createMap(String requestedId) {
        String mapId = normalizeMapId(requestedId);
        if (mapId.isBlank() || arenas.containsKey(mapId)) {
            return false;
        }
        arenas.put(mapId, new ArenaDefinition());
        activeMapId = mapId;
        setDirty();
        return true;
    }

    public boolean selectMap(String requestedId) {
        String mapId = normalizeMapId(requestedId);
        if (!arenas.containsKey(mapId)) {
            return false;
        }
        activeMapId = mapId;
        setDirty();
        return true;
    }

    public int configuredMapCount() {
        return arenas.size();
    }

    public Optional<LoadoutPreset> loadout(int index) {
        return validIndex(index) ? Optional.ofNullable(loadouts[index]) : Optional.empty();
    }

    public void setLoadout(int index, LoadoutPreset preset) {
        if (!validIndex(index)) {
            throw new IllegalArgumentException("Loadout index must be between 0 and 4");
        }
        loadouts[index] = preset;
        setDirty();
    }

    public void clearLoadout(int index) {
        if (validIndex(index)) {
            loadouts[index] = null;
            setDirty();
        }
    }

    public long configuredLoadoutCount() {
        return Arrays.stream(loadouts).filter(value -> value != null).count();
    }

    public void arenaChanged() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag savedMaps = new ListTag();
        arenas.forEach((mapId, arena) -> {
            CompoundTag savedMap = new CompoundTag();
            savedMap.putString("id", mapId);
            savedMap.put("arena", arena.save());
            savedMaps.add(savedMap);
        });
        tag.put("maps", savedMaps);
        tag.putString("activeMap", activeMapId);
        ListTag savedLoadouts = new ListTag();
        for (int index = 0; index < loadouts.length; index++) {
            if (loadouts[index] != null) {
                CompoundTag saved = loadouts[index].save();
                saved.putInt("slot", index);
                savedLoadouts.add(saved);
            }
        }
        tag.put("loadouts", savedLoadouts);
        return tag;
    }

    public static RoseSavedData load(CompoundTag tag) {
        RoseSavedData data = new RoseSavedData();
        data.arenas.clear();
        if (tag.contains("maps", Tag.TAG_LIST)) {
            ListTag maps = tag.getList("maps", Tag.TAG_COMPOUND);
            for (int index = 0; index < maps.size(); index++) {
                CompoundTag savedMap = maps.getCompound(index);
                String mapId = normalizeMapId(savedMap.getString("id"));
                if (!mapId.isBlank() && savedMap.contains("arena", Tag.TAG_COMPOUND)) {
                    data.arenas.put(mapId, ArenaDefinition.load(savedMap.getCompound("arena")));
                }
            }
        } else if (tag.contains("arena", Tag.TAG_COMPOUND)) {
            data.arenas.put("default", ArenaDefinition.load(tag.getCompound("arena")));
        }
        if (data.arenas.isEmpty()) {
            data.arenas.put("default", new ArenaDefinition());
        }
        String requestedActiveMap = normalizeMapId(tag.getString("activeMap"));
        if (!requestedActiveMap.isBlank() && data.arenas.containsKey(requestedActiveMap)) {
            data.activeMapId = requestedActiveMap;
        } else {
            data.activeMapId = data.arenas.keySet().iterator().next();
        }
        ListTag loadouts = tag.getList("loadouts", Tag.TAG_COMPOUND);
        for (int index = 0; index < loadouts.size(); index++) {
            CompoundTag saved = loadouts.getCompound(index);
            int slot = saved.getInt("slot");
            if (validIndex(slot)) {
                data.loadouts[slot] = LoadoutPreset.load(saved);
            }
        }
        return data;
    }

    private static boolean validIndex(int index) {
        return index >= 0 && index < LOADOUT_COUNT;
    }

    public static String normalizeMapId(String input) {
        if (input == null) {
            return "";
        }
        String normalized = input.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized.length() > 32 ? normalized.substring(0, 32) : normalized;
    }
}
