package com.synergy902.projectrose.event;

import com.synergy902.projectrose.ProjectRose;
import com.synergy902.projectrose.command.RoseCommands;
import com.synergy902.projectrose.config.RoseConfig;
import com.synergy902.projectrose.game.MatchManager;
import com.synergy902.projectrose.game.MatchPhase;
import com.synergy902.projectrose.network.RoseNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.Container;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ProjectRose.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RoseEvents {
    private RoseEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        RoseCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MatchManager.get(event.getServer()).tick();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MatchManager.remove(event.getServer());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MatchManager manager = MatchManager.get(player.server);
            manager.handleLogin(player);
            RoseNetwork.sync(player, manager);
            if (manager.phase() == MatchPhase.MAP_VOTE) {
                RoseNetwork.openMapVote(player);
            } else if (manager.phase() != MatchPhase.POST_MATCH) {
                RoseNetwork.openLobby(player);
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MatchManager.get(player.server).prepareRespawn(player);
        }
    }

    @SubscribeEvent
    public static void onFriendlyFire(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        MatchManager manager = MatchManager.get(victim.server);
        if (manager.isPostMatchInvulnerable(victim)) {
            event.setCanceled(true);
            return;
        }
        if (manager.hasSpawnProtection(victim)) {
            event.setCanceled(true);
            return;
        }
        if (RoseConfig.FRIENDLY_FIRE.get()) {
            return;
        }
        ServerPlayer attacker = resolveAttacker(event.getSource().getEntity(), event.getSource().getDirectEntity());
        if (attacker != null && manager.areTeammates(attacker, victim)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        ServerPlayer attacker = resolveAttacker(event.getSource().getEntity(), event.getSource().getDirectEntity());
        MatchManager.get(victim.server).recordDeath(victim, attacker, null, false);
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!RoseConfig.BLOCK_ITEM_DROPS.get() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (MatchManager.get(player.server).isParticipant(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (RoseConfig.BLOCK_ITEM_DROPS.get() && MatchManager.get(player.server).isParticipant(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (RoseConfig.BLOCK_ITEM_PICKUPS.get() && MatchManager.get(player.server).isParticipant(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (RoseConfig.PROTECT_ARENA.get() && MatchManager.get(player.server).isParticipant(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (RoseConfig.PROTECT_ARENA.get() && MatchManager.get(player.server).isParticipant(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseContainer(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !RoseConfig.PROTECT_ARENA.get()
                || !MatchManager.get(player.server).isParticipant(player)) {
            return;
        }
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof Container) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseEquipmentEntity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !RoseConfig.PROTECT_ARENA.get()
                || !MatchManager.get(player.server).isParticipant(player)) {
            return;
        }
        if (event.getTarget() instanceof ItemFrame || event.getTarget() instanceof ArmorStand) {
            event.setCanceled(true);
        }
    }

    private static ServerPlayer resolveAttacker(Entity causingEntity, Entity directEntity) {
        if (causingEntity instanceof ServerPlayer player) {
            return player;
        }
        if (directEntity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }
}
