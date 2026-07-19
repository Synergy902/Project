package com.synergy902.projectrose.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.synergy902.projectrose.ProjectRose;
import com.synergy902.projectrose.client.screen.TeamClassScreen;
import com.synergy902.projectrose.client.screen.MapVoteScreen;
import com.synergy902.projectrose.client.screen.admin.AdminLoadoutScreen;
import com.synergy902.projectrose.config.RoseClientConfig;
import com.synergy902.projectrose.game.MatchPhase;
import com.synergy902.projectrose.game.RoseTeam;
import com.synergy902.projectrose.network.MatchSnapshot;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screens.MenuScreens;
import com.synergy902.projectrose.menu.RoseMenus;

public final class RoseClientEvents {
    private static final int BLACK = 0xD9090909;
    private static final int AMBER = 0xFFFFA31A;
    private static final int RED = 0xFFC42A2A;
    private static final int BLUE = 0xFF2B66C2;
    private static final int WHITE = 0xFFF3F3F3;
    private static final int MUTED = 0xFFAAAAAA;

    private static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.projectrose.open_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.projectrose"
    );

    private RoseClientEvents() {
    }

    @Mod.EventBusSubscriber(modid = ProjectRose.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_MENU);
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> MenuScreens.register(RoseMenus.ADMIN_LOADOUT.get(), AdminLoadoutScreen::new));
        }
    }

    @Mod.EventBusSubscriber(modid = ProjectRose.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeBus {
        private ForgeBus() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            while (OPEN_MENU.consumeClick()) {
                if (minecraft.player != null && minecraft.player.isAlive() && minecraft.screen == null) {
                    if (ClientMatchState.snapshot().phase() == MatchPhase.POST_MATCH) {
                        continue;
                    } else if (ClientMatchState.snapshot().phase() == MatchPhase.MAP_VOTE) {
                        minecraft.setScreen(new MapVoteScreen());
                    } else {
                        minecraft.setScreen(new TeamClassScreen());
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.options.hideGui || minecraft.player == null) {
                return;
            }
            renderHud(event.getGuiGraphics(), minecraft.font, ClientMatchState.snapshot());
        }
    }

    private static void renderHud(GuiGraphics graphics, Font font, MatchSnapshot snapshot) {
        if (snapshot.phase() == MatchPhase.POST_MATCH) {
            renderVictoryOverlay(graphics, font, snapshot);
            return;
        }
        if (snapshot.playerTeam() == RoseTeam.NONE && snapshot.phase() == MatchPhase.WAITING) {
            return;
        }
        if (!RoseClientConfig.HUD_ENABLED.get()) {
            return;
        }

        int width = graphics.guiWidth();
        int center = width / 2;
        int top = 8 + RoseClientConfig.SCOREBOARD_Y_OFFSET.get();
        int panelWidth = 198;
        int left = center - panelWidth / 2;
        int panelColor = withOpacity(BLACK, RoseClientConfig.HUD_OPACITY.get());

        graphics.fill(left, top, left + panelWidth, top + 31, panelColor);
        graphics.fill(left, top, left + 4, top + 31, RED);
        graphics.fill(left + panelWidth - 4, top, left + panelWidth, top + 31, BLUE);
        graphics.fill(center - 24, top, center + 24, top + 31, 0xEE141414);

        graphics.drawString(font, "RED", left + 10, top + 5, RED, false);
        graphics.drawString(font, Integer.toString(snapshot.redScore()), left + 10, top + 17, WHITE, false);

        String timer = formatTime(snapshot.secondsRemaining());
        graphics.drawCenteredString(font, timer, center, top + 6, AMBER);
        graphics.drawCenteredString(font, snapshot.phase().name().replace('_', ' '), center, top + 18, MUTED);

        String blueScore = Integer.toString(snapshot.blueScore());
        graphics.drawString(font, "BLUE", left + panelWidth - 10 - font.width("BLUE"), top + 5, BLUE, false);
        graphics.drawString(font, blueScore, left + panelWidth - 10 - font.width(blueScore), top + 17, WHITE, false);

        if (RoseClientConfig.SHOW_PERSONAL_STATS.get()) {
            int statsLeft = 8 + RoseClientConfig.PERSONAL_STATS_X_OFFSET.get();
            int statsTop = graphics.guiHeight() - 39 + RoseClientConfig.PERSONAL_STATS_Y_OFFSET.get();
            graphics.fill(statsLeft, statsTop, statsLeft + 146, statsTop + 31, panelColor);
            int teamColor = snapshot.playerTeam() == RoseTeam.RED ? RED : BLUE;
            graphics.fill(statsLeft, statsTop, statsLeft + 4, statsTop + 31, teamColor);
            graphics.drawString(font, snapshot.playerTeam().serializedName().toUpperCase() + " TEAM",
                    statsLeft + 10, statsTop + 5, teamColor, false);
            graphics.drawString(font, "K " + snapshot.kills() + "   D " + snapshot.deaths()
                    + "   A " + snapshot.assists(), statsLeft + 10, statsTop + 17, WHITE, false);

            if (snapshot.selectedLoadout() >= 0 && snapshot.selectedLoadout() < snapshot.loadoutNames().size()) {
                String name = snapshot.loadoutNames().get(snapshot.selectedLoadout());
                if (!name.isBlank()) {
                    graphics.drawString(font, name.toUpperCase(), statsLeft + 82, statsTop + 5, AMBER, false);
                }
            }
        }

        if (RoseClientConfig.SHOW_BALANCE_WARNING.get() && snapshot.balanceWarningSeconds() > 0) {
            String warning = "FORCED TEAM BALANCING IN " + snapshot.balanceWarningSeconds();
            int warningWidth = font.width(warning) + 22;
            int warningLeft = center - warningWidth / 2;
            graphics.fill(warningLeft, 47, warningLeft + warningWidth, 67, 0xE0200000);
            graphics.drawCenteredString(font, warning, center, 53, RED);
        }

        if (snapshot.spawnProtectionSeconds() > 0) {
            String protection = "SPAWN PROTECTION  " + snapshot.spawnProtectionSeconds();
            int protectionWidth = font.width(protection) + 18;
            graphics.fill(center - protectionWidth / 2, 71, center + protectionWidth / 2, 89,
                    withOpacity(0xFF131313, RoseClientConfig.HUD_OPACITY.get()));
            graphics.drawCenteredString(font, protection, center, 76, AMBER);
        }

        if (snapshot.phase() == MatchPhase.MAP_VOTE) {
            String votePrompt = "MAP VOTE  //  " + snapshot.secondsRemaining() + "  //  PRESS M TO REOPEN";
            int promptWidth = font.width(votePrompt) + 20;
            graphics.fill(center - promptWidth / 2, 47, center + promptWidth / 2, 66,
                    withOpacity(0xFF151515, RoseClientConfig.HUD_OPACITY.get()));
            graphics.drawCenteredString(font, votePrompt, center, 52, AMBER);
        }
    }

    private static void renderVictoryOverlay(GuiGraphics graphics, Font font, MatchSnapshot snapshot) {
        float fade = ClientMatchState.victoryFadeProgress();
        int screenAlpha = Math.max(0, Math.min(220, Math.round(220.0F * fade)));
        int textAlpha = Math.max(0, Math.min(255, Math.round(255.0F * fade)));
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), screenAlpha << 24);

        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        String winnerText = snapshot.winner() == RoseTeam.NONE
                ? "MATCH DRAW"
                : snapshot.winner().serializedName().toUpperCase() + " TEAM WINS";
        int winnerColor = snapshot.winner() == RoseTeam.RED ? RED
                : snapshot.winner() == RoseTeam.BLUE ? BLUE : AMBER;
        winnerColor = (textAlpha << 24) | (winnerColor & 0x00FFFFFF);
        int white = (textAlpha << 24) | (WHITE & 0x00FFFFFF);
        int amber = (textAlpha << 24) | (AMBER & 0x00FFFFFF);

        graphics.fill(centerX - 172, centerY - 39, centerX + 172, centerY + 42,
                (Math.round(205.0F * fade) << 24) | 0x00111111);
        graphics.fill(centerX - 172, centerY + 39, centerX + 172, centerY + 42, amber);
        graphics.drawCenteredString(font, "TEAM DEATHMATCH // " + snapshot.activeMapId().toUpperCase(),
                centerX, centerY - 28, white);
        graphics.drawCenteredString(font, winnerText, centerX, centerY - 6, winnerColor);
        graphics.drawCenteredString(font,
                "RED " + snapshot.redScore() + "     //     " + snapshot.blueScore() + " BLUE",
                centerX, centerY + 10, white);
        graphics.drawCenteredString(font, "MAP VOTE IN " + snapshot.secondsRemaining(),
                centerX, centerY + 26, amber);
    }

    private static String formatTime(int totalSeconds) {
        int minutes = Math.max(0, totalSeconds) / 60;
        int seconds = Math.max(0, totalSeconds) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static int withOpacity(int color, double opacity) {
        int alpha = Math.max(0, Math.min(255, (int) Math.round(opacity * 255.0D)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}
