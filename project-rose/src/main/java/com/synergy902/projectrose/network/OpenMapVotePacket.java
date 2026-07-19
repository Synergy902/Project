package com.synergy902.projectrose.network;

import com.synergy902.projectrose.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class OpenMapVotePacket {
    public static void encode(OpenMapVotePacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenMapVotePacket decode(FriendlyByteBuf buffer) {
        return new OpenMapVotePacket();
    }

    public static void handle(OpenMapVotePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketHandler.openMapVote()
        ));
        context.setPacketHandled(true);
    }
}

