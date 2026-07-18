package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.client.TvMissileClientHandler
import com.atsuishio.superbwarfare.entity.projectile.WireGuideMissileEntity
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@OnlyIn(Dist.CLIENT)
object TvGuidedMissileOverlay : CommonOverlay("tv_guided_missile") {
    private const val GREEN = 0xFF65FF87.toInt()
    private const val DIM_GREEN = 0xFF2FAE55.toInt()
    private const val DARK_GREEN = 0xA0103820.toInt()
    private const val BLACK = 0xE8000000.toInt()
    private const val WARNING = 0xFFFFB347.toInt()
    private const val RETICLE_WHITE = 0xFFF5F5F5.toInt()
    private const val RETICLE_GLOW = 0xB0000000.toInt()

    private var telemetryMissileId = -1
    private var telemetryBucket = -1
    private var telemetry = Telemetry("SPD 0000 KM/H", "RNG 0000 M", "LINK 100%", "TGO 0.0 S", 100)

    private val controlHint by lazy(LazyThreadSafetyMode.NONE) {
        Component.translatable("hud.superbwarfare.tv_missile.cancel", ModKeyMappings.INTERACT.key.displayName)
    }
    private val signalWarning by lazy(LazyThreadSafetyMode.NONE) {
        Component.translatable("hud.superbwarfare.tv_missile.signal_warning")
    }

    override fun shouldRender(): Boolean {
        return super.shouldRender() && TvMissileClientHandler.isActive()
    }

    override fun RenderContext.render() {
        val missile = TvMissileClientHandler.controlledMissile() ?: return
        val graphics = guiGraphics
        val font = mc.font
        val cx = screenWidth / 2
        val cy = screenHeight / 2
        val telemetry = updateTelemetry(missile)
        val recordingPulse = if ((TvMissileClientHandler.elapsedTicks() / 8) % 2 == 0) GREEN else DIM_GREEN

        // Flush queued GUI work, then submit every non-text TV element in one batch.
        graphics.flush()
        val batch = HudBatch(graphics.pose().last().pose())
        batch.rect(0.0, 0.0, screenWidth.toDouble(), screenHeight.toDouble(), 0x1810FF50)
        batch.rect(0.0, 0.0, screenWidth.toDouble(), 12.0, BLACK)
        batch.rect(0.0, (screenHeight - 24).toDouble(), screenWidth.toDouble(), screenHeight.toDouble(), BLACK)
        batch.rect(0.0, 0.0, 8.0, screenHeight.toDouble(), BLACK)
        batch.rect((screenWidth - 8).toDouble(), 0.0, screenWidth.toDouble(), screenHeight.toDouble(), BLACK)
        for (y in 14 until screenHeight - 24 step 4) {
            batch.rect(8.0, y.toDouble(), (screenWidth - 8).toDouble(), (y + 1).toDouble(), 0x16000000)
        }

        renderCornerBrackets(batch, screenWidth, screenHeight)
        renderReticle(batch, cx, cy, partialTick)
        batch.rect(
            (screenWidth - 77).toDouble(), (screenHeight - 54).toDouble(),
            (screenWidth - 72).toDouble(), (screenHeight - 49).toDouble(), recordingPulse
        )
        batch.flush()

        graphics.drawString(font, "MI-28 // TV GUIDANCE", 18, 18, GREEN, false)
        graphics.drawString(font, "9M120  MANUAL COMMAND", 18, 29, DIM_GREEN, false)
        graphics.drawString(font, telemetry.speedText, 18, screenHeight - 57, GREEN, false)
        graphics.drawString(font, telemetry.rangeText, 18, screenHeight - 46, GREEN, false)
        val signalColor = if (telemetry.signal <= 20) WARNING else GREEN
        graphics.drawString(
            font, telemetry.signalText, screenWidth - 18 - font.width(telemetry.signalText), 18, signalColor, false
        )
        graphics.drawString(font, telemetry.timeText, screenWidth - 18 - font.width(telemetry.timeText), 29, GREEN, false)
        graphics.drawCenteredString(font, controlHint, cx, screenHeight - 17, DIM_GREEN)

        if (telemetry.signal <= 20) {
            graphics.drawCenteredString(font, signalWarning, cx, 26, WARNING)
        }

        graphics.drawString(font, "REC", screenWidth - 67, screenHeight - 56, recordingPulse, false)
    }

    private fun RenderContext.updateTelemetry(missile: WireGuideMissileEntity): Telemetry {
        val bucket = TvMissileClientHandler.elapsedTicks() / TELEMETRY_INTERVAL_TICKS
        if (telemetryMissileId == missile.id && telemetryBucket == bucket) return telemetry

        val distance = player.distanceTo(missile).toDouble()
        val speedKmh = missile.deltaMovement.length() * 72.0
        val remainingTicks = (missile.getLife() - missile.syncedTick).coerceAtLeast(0)
        val signal = ((1.0 - distance / WireGuideMissileEntity.MAX_TV_CONTROL_RANGE) * 100.0)
            .coerceIn(0.0, 100.0)
            .roundToInt()
        val timeTenths = (remainingTicks / 2.0).roundToInt()

        telemetryMissileId = missile.id
        telemetryBucket = bucket
        telemetry = Telemetry(
            "SPD ${format(speedKmh)} KM/H",
            "RNG ${format(distance)} M",
            "LINK ${signal.toString().padStart(3, '0')}%",
            "TGO ${timeTenths / 10}.${timeTenths % 10} S",
            signal
        )
        return telemetry
    }

    private fun renderReticle(batch: HudBatch, cx: Int, cy: Int, partialTick: Float) {
        val turnRotation = TvMissileClientHandler.yawMotionCue(partialTick).toDouble() * 34.0
        val angle = Math.toRadians(turnRotation)
        val cosine = cos(angle)
        val sine = sin(angle)
        for (segment in RETICLE_SEGMENTS) {
            batch.line(
                cx + segment.x0 * cosine - segment.y0 * sine,
                cy + segment.x0 * sine + segment.y0 * cosine,
                cx + segment.x1 * cosine - segment.y1 * sine,
                cy + segment.x1 * sine + segment.y1 * cosine,
                segment.color,
                segment.thickness
            )
        }
    }

    private fun renderCornerBrackets(batch: HudBatch, w: Int, h: Int) {
        val inset = 17
        val length = 24
        batch.line(inset.toDouble(), inset.toDouble(), (inset + length).toDouble(), inset.toDouble(), DARK_GREEN)
        batch.line(inset.toDouble(), inset.toDouble(), inset.toDouble(), (inset + length).toDouble(), DARK_GREEN)
        batch.line((w - inset - length).toDouble(), inset.toDouble(), (w - inset).toDouble(), inset.toDouble(), DARK_GREEN)
        batch.line((w - inset).toDouble(), inset.toDouble(), (w - inset).toDouble(), (inset + length).toDouble(), DARK_GREEN)
        val bottom = h - inset - 24
        batch.line(inset.toDouble(), bottom.toDouble(), (inset + length).toDouble(), bottom.toDouble(), DARK_GREEN)
        batch.line(inset.toDouble(), (bottom - length).toDouble(), inset.toDouble(), bottom.toDouble(), DARK_GREEN)
        batch.line((w - inset - length).toDouble(), bottom.toDouble(), (w - inset).toDouble(), bottom.toDouble(), DARK_GREEN)
        batch.line((w - inset).toDouble(), (bottom - length).toDouble(), (w - inset).toDouble(), bottom.toDouble(), DARK_GREEN)
    }

    private fun format(value: Double) = value.roundToInt().toString().padStart(4, '0')

    private data class Telemetry(
        val speedText: String,
        val rangeText: String,
        val signalText: String,
        val timeText: String,
        val signal: Int
    )

    private data class ReticleSegment(
        val x0: Double,
        val y0: Double,
        val x1: Double,
        val y1: Double,
        val color: Int,
        val thickness: Double
    )

    private class HudBatch(private val matrix: Matrix4f) {
        private val builder: BufferBuilder = Tesselator.getInstance().builder

        init {
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.disableDepthTest()
            RenderSystem.depthMask(false)
            RenderSystem.disableCull()
            RenderSystem.setShader(GameRenderer::getPositionColorShader)
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
        }

        fun rect(x0: Double, y0: Double, x1: Double, y1: Double, color: Int) {
            vertex(x0, y0, color)
            vertex(x0, y1, color)
            vertex(x1, y1, color)
            vertex(x1, y0, color)
        }

        fun line(x0: Double, y0: Double, x1: Double, y1: Double, color: Int, thickness: Double = 1.0) {
            val length = hypot(x1 - x0, y1 - y0)
            if (length <= 1.0e-6) return
            val half = thickness * 0.5
            val normalX = -(y1 - y0) / length * half
            val normalY = (x1 - x0) / length * half
            vertex(x0 + normalX, y0 + normalY, color)
            vertex(x0 - normalX, y0 - normalY, color)
            vertex(x1 - normalX, y1 - normalY, color)
            vertex(x1 + normalX, y1 + normalY, color)
        }

        private fun vertex(x: Double, y: Double, color: Int) {
            builder.vertex(matrix, x.toFloat(), y.toFloat(), 0f)
                .color(color shr 16 and 0xFF, color shr 8 and 0xFF, color and 0xFF, color ushr 24 and 0xFF)
                .endVertex()
        }

        fun flush() {
            BufferUploader.drawWithShader(builder.end())
            RenderSystem.enableCull()
            RenderSystem.depthMask(true)
            RenderSystem.enableDepthTest()
            RenderSystem.disableBlend()
        }
    }

    private fun buildReticleSegments(): List<ReticleSegment> = buildList {
        add(ReticleSegment(-106.0, 0.0, -39.0, 0.0, RETICLE_GLOW, 3.0))
        add(ReticleSegment(39.0, 0.0, 106.0, 0.0, RETICLE_GLOW, 3.0))
        add(ReticleSegment(-106.0, 0.0, -39.0, 0.0, RETICLE_WHITE, 1.0))
        add(ReticleSegment(39.0, 0.0, 106.0, 0.0, RETICLE_WHITE, 1.0))

        addArc(-63.0, 63.0, RETICLE_GLOW, 5.0)
        addArc(117.0, 243.0, RETICLE_GLOW, 5.0)
        addArc(-63.0, 63.0, RETICLE_WHITE, 3.0)
        addArc(117.0, 243.0, RETICLE_WHITE, 3.0)

        addRadialTick(-90.0, 38.0, 48.0)
        addRadialTick(45.0, 39.0, 49.0)
        addRadialTick(135.0, 39.0, 49.0)
    }

    private fun MutableList<ReticleSegment>.addArc(
        startDegrees: Double,
        endDegrees: Double,
        color: Int,
        thickness: Double
    ) {
        var angle = startDegrees
        var previousX = BRACKET_RADIUS * cos(Math.toRadians(angle))
        var previousY = BRACKET_RADIUS * sin(Math.toRadians(angle))
        while (angle < endDegrees) {
            angle = (angle + ARC_STEP_DEGREES).coerceAtMost(endDegrees)
            val x = BRACKET_RADIUS * cos(Math.toRadians(angle))
            val y = BRACKET_RADIUS * sin(Math.toRadians(angle))
            add(ReticleSegment(previousX, previousY, x, y, color, thickness))
            previousX = x
            previousY = y
        }
    }

    private fun MutableList<ReticleSegment>.addRadialTick(
        angleDegrees: Double,
        innerRadius: Double,
        outerRadius: Double
    ) {
        val angle = Math.toRadians(angleDegrees)
        add(
            ReticleSegment(
                innerRadius * cos(angle), innerRadius * sin(angle),
                outerRadius * cos(angle), outerRadius * sin(angle),
                RETICLE_WHITE, 1.0
            )
        )
    }

    private const val TELEMETRY_INTERVAL_TICKS = 4
    private const val BRACKET_RADIUS = 22.0
    private const val ARC_STEP_DEGREES = 2.0
    private val RETICLE_SEGMENTS = buildReticleSegments()
}
