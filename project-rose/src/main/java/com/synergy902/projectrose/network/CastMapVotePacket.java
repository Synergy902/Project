package com.synergy902.projectrose.network;

import com.synergy902.projectrose.game.MatchManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CastMapVotePacket(String mapId) {
    public static void encode(CastMapVotePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.mapId, 32);
    }

    public static CastMapVotePacket decode(FriendlyByteBuf buffer) {
        return new CastMapVotePacket(buffer.readUtf(32));
    }

    public static void handle(CastMapVotePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                MatchManager.get(player.server).castMapVote(player, packet.mapId);
            }
        });
        context.setPacketHandled(true);
    }
}

