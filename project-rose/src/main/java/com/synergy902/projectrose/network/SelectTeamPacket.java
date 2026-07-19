package com.synergy902.projectrose.network;

import com.synergy902.projectrose.game.MatchManager;
import com.synergy902.projectrose.game.RoseTeam;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SelectTeamPacket(RoseTeam team) {
    public static void encode(SelectTeamPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.team);
    }

    public static SelectTeamPacket decode(FriendlyByteBuf buffer) {
        return new SelectTeamPacket(buffer.readEnum(RoseTeam.class));
    }

    public static void handle(SelectTeamPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && packet.team.isPlayable()) {
                MatchManager manager = MatchManager.get(player.server);
                manager.chooseTeam(player, packet.team);
                RoseNetwork.sync(player, manager);
            }
        });
        context.setPacketHandled(true);
    }
}

