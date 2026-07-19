package com.synergy902.projectrose.compat;

import com.synergy902.projectrose.ProjectRose;
import com.synergy902.projectrose.game.MatchManager;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.event.common.GunFireEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ProjectRose.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TaczCombatEvents {
    private TaczCombatEvents() {
    }

    @SubscribeEvent
    public static void onEntityKilledByGun(EntityKillByGunEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER
                || !(event.getKilledEntity() instanceof ServerPlayer victim)) {
            return;
        }
        ServerPlayer attacker = event.getAttacker() instanceof ServerPlayer player ? player : null;
        MatchManager.get(victim.server).recordDeath(
                victim,
                attacker,
                event.getGunDisplayId(),
                event.isHeadShot()
        );
    }

    @SubscribeEvent
    public static void onGunFired(GunFireEvent event) {
        if (event.getLogicalSide() == LogicalSide.SERVER && event.getShooter() instanceof ServerPlayer player) {
            MatchManager.get(player.server).removeSpawnProtection(player);
        }
    }
}
