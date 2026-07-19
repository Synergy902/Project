package com.synergy902.projectrose.network;

import com.synergy902.projectrose.menu.AdminLoadoutMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public record OpenAdminLoadoutPacket(int loadoutIndex) {
    public static void encode(OpenAdminLoadoutPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.loadoutIndex);
    }

    public static OpenAdminLoadoutPacket decode(FriendlyByteBuf buffer) {
        return new OpenAdminLoadoutPacket(buffer.readVarInt());
    }

    public static void handle(OpenAdminLoadoutPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)
                    || packet.loadoutIndex < 0 || packet.loadoutIndex >= 5) {
                return;
            }
            NetworkHooks.openScreen(
                    player,
                    new SimpleMenuProvider(
                            (containerId, inventory, ignored) ->
                                    new AdminLoadoutMenu(containerId, inventory, packet.loadoutIndex),
                            Component.literal("Project Rose Loadout " + (packet.loadoutIndex + 1))
                    ),
                    buffer -> buffer.writeVarInt(packet.loadoutIndex)
            );
        });
        context.setPacketHandled(true);
    }
}

