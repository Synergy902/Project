package com.synergy902.projectrose.network;

import com.synergy902.projectrose.game.MatchManager;
import com.synergy902.projectrose.menu.AdminLoadoutMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AdminLoadoutActionPacket(Action action, String name) {
    public enum Action {
        IMPORT_CURRENT,
        CLEAR,
        SAVE
    }

    public AdminLoadoutActionPacket {
        name = name == null ? "" : name;
    }

    public static void encode(AdminLoadoutActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeUtf(packet.name, 32);
    }

    public static AdminLoadoutActionPacket decode(FriendlyByteBuf buffer) {
        return new AdminLoadoutActionPacket(buffer.readEnum(Action.class), buffer.readUtf(32));
    }

    public static void handle(AdminLoadoutActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)
                    || !(player.containerMenu instanceof AdminLoadoutMenu menu)) {
                return;
            }
            switch (packet.action) {
                case IMPORT_CURRENT -> menu.importPlayerInventory(player);
                case CLEAR -> menu.clearEditor();
                case SAVE -> {
                    menu.save(player, packet.name);
                    player.sendSystemMessage(Component.literal(
                            "Saved Project Rose class " + (menu.loadoutIndex() + 1) + ": " + packet.name));
                    RoseNetwork.sync(player, MatchManager.get(player.server));
                }
            }
        });
        context.setPacketHandled(true);
    }
}

