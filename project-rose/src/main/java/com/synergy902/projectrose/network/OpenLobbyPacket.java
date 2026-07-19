package com.synergy902.projectrose.network;

import com.synergy902.projectrose.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class OpenLobbyPacket {
    public static void encode(OpenLobbyPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenLobbyPacket decode(FriendlyByteBuf buffer) {
        return new OpenLobbyPacket();
    }

    public static void handle(OpenLobbyPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketHandler.openLobby()
        ));
        context.setPacketHandled(true);
    }
}

