package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.client.TvMissileClientHandler
import com.atsuishio.superbwarfare.entity.projectile.WireGuideMissileEntity
import com.atsuishio.superbwarfare.init.ModKeyMappings
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sin

@OnlyIn(Dist.CLIENT)
object TvGuidedMissileOverlay : CommonOverlay("tv_guided_missile") {
    private const val GREEN = 0xFF65FF87.toInt()
    private const val DIM_GREEN = 0xFF2FAE55.toInt()
    private const val DARK_GREEN = 0xA0103820.toInt()
    private const val BLACK = 0xE8000000.toInt()
    private const val WARNING = 0xFFFFB347.toInt()

    override fun shouldRender(): Boolean {
        return super.shouldRender() && TvMissileClientHandler.isActive()
    }

    override fun RenderContext.render() {
        val missile = TvMissileClientHandler.controlledMissile() ?: return
        val graphics = guiGraphics
        val font = mc.font
        val cx = screenWidth / 2
        val cy = screenHeight / 2

        // TV tint, letterbox, and scan lines. Registered above the other HUD layers.
        graphics.fill(0, 0, screenWidth, screenHeight, 0x1810FF50)
        graphics.fill(0, 0, screenWidth, 12, BLACK)
        graphics.fill(0, screenHeight - 24, screenWidth, screenHeight, BLACK)
        graphics.fill(0, 0, 8, screenHeight, BLACK)
        graphics.fill(screenWidth - 8, 0, screenWidth, screenHeight, BLACK)
        for (y in 14 until screenHeight - 24 step 4) {
            graphics.fill(8, y, screenWidth - 8, y + 1, 0x16000000)
        }

        renderCornerBrackets(screenWidth, screenHeight)
        renderReticle(cx, cy)
        renderAttitudeCue(missile, cx, cy)

        val distance = player.distanceTo(missile).toDouble()
        val speedKmh = missile.deltaMovement.length() * 72.0
        val remainingTicks = (missile.getLife() - missile.syncedTick).coerceAtLeast(0)
        val signal = ((1.0 - distance / WireGuideMissileEntity.MAX_TV_CONTROL_RANGE) * 100.0)
            .coerceIn(0.0, 100.0)
            .roundToInt()
        val signalColor = if (signal <= 20) WARNING else GREEN

        graphics.drawString(font, "MI-28 // TV GUIDANCE", 18, 18, GREEN, false)
        graphics.drawString(font, "9M120  MANUAL COMMAND", 18, 29, DIM_GREEN, false)
        graphics.drawString(font, "SPD ${format(speedKmh)} KM/H", 18, screenHeight - 57, GREEN, false)
        graphics.drawString(font, "RNG ${format(distance)} M", 18, screenHeight - 46, GREEN, false)

        val signalText = "LINK ${signal.toString().padStart(3, '0')}%"
        graphics.drawString(font, signalText, screenWidth - 18 - font.width(signalText), 18, signalColor, false)
        val timeText = String.format(Locale.ROOT, "TGO %.1F S", remainingTicks / 20.0)
        graphics.drawString(font, timeText, screenWidth - 18 - font.width(timeText), 29, GREEN, false)

        val controlHint = Component.translatable(
            "hud.superbwarfare.tv_missile.cancel",
            ModKeyMappings.INTERACT.key.displayName
        )
        graphics.drawCenteredString(font, controlHint, cx, screenHeight - 17, DIM_GREEN)

        if (signal <= 20) {
            val warning = Component.translatable("hud.superbwarfare.tv_missile.signal_warning")
            graphics.drawCenteredString(font, warning, cx, 26, WARNING)
        }

        val recordingPulse = if ((TvMissileClientHandler.elapsedTicks() / 8) % 2 == 0) GREEN else DIM_GREEN
        graphics.fill(screenWidth - 77, screenHeight - 54, screenWidth - 72, screenHeight - 49, recordingPulse)
        graphics.drawString(font, "REC", screenWidth - 67, screenHeight - 56, recordingPulse, false)
    }

    private fun RenderContext.renderReticle(cx: Int, cy: Int) {
        val graphics = guiGraphics
        val pulse = (2.0 * sin(TvMissileClientHandler.elapsedTicks() * 0.13)).roundToInt()
        val gap = 9 + pulse
        val arm = 17

        graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xCC000000.toInt())
        graphics.hLine(cx - gap - arm, cx - gap, cy, GREEN)
        graphics.hLine(cx + gap, cx + gap + arm, cy, GREEN)
        graphics.vLine(cx, cy - gap - arm, cy - gap, GREEN)
        graphics.vLine(cx, cy + gap, cy + gap + arm, GREEN)
        graphics.hLine(cx - 3, cx + 3, cy, GREEN)
        graphics.vLine(cx, cy - 3, cy + 3, GREEN)

        // Range-gate box.
        val box = 34
        graphics.hLine(cx - box, cx - box + 8, cy - box, DIM_GREEN)
        graphics.vLine(cx - box, cy - box, cy - box + 8, DIM_GREEN)
        graphics.hLine(cx + box - 8, cx + box, cy - box, DIM_GREEN)
        graphics.vLine(cx + box, cy - box, cy - box + 8, DIM_GREEN)
        graphics.hLine(cx - box, cx - box + 8, cy + box, DIM_GREEN)
        graphics.vLine(cx - box, cy + box - 8, cy + box, DIM_GREEN)
        graphics.hLine(cx + box - 8, cx + box, cy + box, DIM_GREEN)
        graphics.vLine(cx + box, cy + box - 8, cy + box, DIM_GREEN)
    }

    private fun RenderContext.renderAttitudeCue(missile: WireGuideMissileEntity, cx: Int, cy: Int) {
        val direction = missile.deltaMovement.normalize()
        val pitchOffset = Mth.clamp((-direction.y * 38.0).roundToInt(), -34, 34)
        val horizonY = cy + pitchOffset
        guiGraphics.hLine(cx - 72, cx - 42, horizonY, DIM_GREEN)
        guiGraphics.hLine(cx + 42, cx + 72, horizonY, DIM_GREEN)
        guiGraphics.vLine(cx - 42, horizonY - 3, horizonY + 3, DIM_GREEN)
        guiGraphics.vLine(cx + 42, horizonY - 3, horizonY + 3, DIM_GREEN)
    }

    private fun RenderContext.renderCornerBrackets(w: Int, h: Int) {
        val graphics = guiGraphics
        val inset = 17
        val length = 24
        graphics.hLine(inset, inset + length, inset, DARK_GREEN)
        graphics.vLine(inset, inset, inset + length, DARK_GREEN)
        graphics.hLine(w - inset - length, w - inset, inset, DARK_GREEN)
        graphics.vLine(w - inset, inset, inset + length, DARK_GREEN)
        graphics.hLine(inset, inset + length, h - inset - 24, DARK_GREEN)
        graphics.vLine(inset, h - inset - 24 - length, h - inset - 24, DARK_GREEN)
        graphics.hLine(w - inset - length, w - inset, h - inset - 24, DARK_GREEN)
        graphics.vLine(w - inset, h - inset - 24 - length, h - inset - 24, DARK_GREEN)
    }

    private fun format(value: Double) = value.roundToInt().toString().padStart(4, '0')
}
