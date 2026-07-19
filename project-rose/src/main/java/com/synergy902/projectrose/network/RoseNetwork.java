package com.synergy902.projectrose.network;

import com.synergy902.projectrose.ProjectRose;
import com.synergy902.projectrose.client.ClientPacketHandler;
import com.synergy902.projectrose.game.MatchManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public final class RoseNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ProjectRose.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );
    private static int messageId;

    private RoseNetwork() {
    }

    public static void initialize() {
        CHANNEL.registerMessage(
                messageId++,
                MatchSnapshot.class,
                MatchSnapshot::encode,
                MatchSnapshot::decode,
                RoseNetwork::handleSnapshot,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                messageId++,
                OpenLobbyPacket.class,
                OpenLobbyPacket::encode,
                OpenLobbyPacket::decode,
                OpenLobbyPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                messageId++,
                SelectTeamPacket.class,
                SelectTeamPacket::encode,
                SelectTeamPacket::decode,
                SelectTeamPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                messageId++,
                OpenMapVotePacket.class,
                OpenMapVotePacket::encode,
                OpenMapVotePacket::decode,
                OpenMapVotePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                messageId++,
                CastMapVotePacket.class,
                CastMapVotePacket::encode,
                CastMapVotePacket::decode,
                CastMapVotePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                messageId++,
                OpenAdminLoadoutPacket.class,
                OpenAdminLoadoutPacket::encode,
                OpenAdminLoadoutPacket::decode,
                OpenAdminLoadoutPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                messageId++,
                AdminLoadoutActionPacket.class,
                AdminLoadoutActionPacket::encode,
                AdminLoadoutActionPacket::decode,
                AdminLoadoutActionPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                messageId++,
                SelectLoadoutPacket.class,
                SelectLoadoutPacket::encode,
                SelectLoadoutPacket::decode,
                SelectLoadoutPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }

    public static void chooseTeam(SelectTeamPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void chooseLoadout(int index) {
        CHANNEL.sendToServer(new SelectLoadoutPacket(index));
    }

    public static void openAdminLoadout(int index) {
        CHANNEL.sendToServer(new OpenAdminLoadoutPacket(index));
    }

    public static void adminLoadoutAction(AdminLoadoutActionPacket.Action action, String name) {
        CHANNEL.sendToServer(new AdminLoadoutActionPacket(action, name));
    }

    public static void openLobby(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenLobbyPacket());
    }

    public static void openMapVote(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenMapVotePacket());
    }

    public static void castMapVote(String mapId) {
        CHANNEL.sendToServer(new CastMapVotePacket(mapId));
    }

    public static void sync(ServerPlayer player, MatchManager manager) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), MatchSnapshot.create(manager, player));
    }

    public static void syncAll(MinecraftServer server, MatchManager manager) {
        server.getPlayerList().getPlayers().forEach(player -> sync(player, manager));
    }

    private static void handleSnapshot(MatchSnapshot snapshot, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketHandler.handleSnapshot(snapshot)
        ));
        context.setPacketHandled(true);
    }
}
