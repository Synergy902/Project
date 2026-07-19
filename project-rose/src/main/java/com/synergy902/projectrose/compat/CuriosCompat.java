package com.synergy902.projectrose.compat;

import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import top.theillusivec4.curios.api.CuriosApi;

public final class CuriosCompat {
    private CuriosCompat() {
    }

    public static ListTag capture(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.saveInventory(false).copy())
                .orElseGet(ListTag::new);
    }

    public static void restore(ServerPlayer player, ListTag savedInventory) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.loadInventory(savedInventory.copy()));
    }
}

