package com.synergy902.projectrose.network;

import com.synergy902.projectrose.game.MatchManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SelectLoadoutPacket(int index) {
    public static void encode(SelectLoadoutPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.index);
    }

    public static SelectLoadoutPacket decode(FriendlyByteBuf buffer) {
        return new SelectLoadoutPacket(buffer.readVarInt());
    }

    public static void handle(SelectLoadoutPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && packet.index >= 0 && packet.index < 5) {
                MatchManager manager = MatchManager.get(player.server);
                manager.selectLoadout(player, packet.index);
                RoseNetwork.sync(player, manager);
            }
        });
        context.setPacketHandled(true);
    }
}

