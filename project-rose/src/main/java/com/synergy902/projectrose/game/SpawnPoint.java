package com.synergy902.projectrose.game;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public record SpawnPoint(BlockPos position, float yaw, float pitch) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("position", position.asLong());
        tag.putFloat("yaw", yaw);
        tag.putFloat("pitch", pitch);
        return tag;
    }

    public static SpawnPoint load(CompoundTag tag) {
        return new SpawnPoint(
                BlockPos.of(tag.getLong("position")),
                tag.getFloat("yaw"),
                tag.getFloat("pitch")
        );
    }
}

