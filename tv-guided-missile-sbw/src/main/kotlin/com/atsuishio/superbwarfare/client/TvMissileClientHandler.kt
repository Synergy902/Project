package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.entity.projectile.WireGuideMissileEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.network.message.send.TvMissileControlMessage
import com.atsuishio.superbwarfare.tools.mc
import com.atsuishio.superbwarfare.tools.sendPacketToServer
import net.minecraft.client.CameraType
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import kotlin.math.atan2

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object TvMissileClientHandler {
    private var missileId = -1
    private var previousCameraType: CameraType? = null
    private var missingTicks = 0
    private var sessionTicks = 0
    private var hasSeenMissile = false
    private var smoothedYawInput = 0f
    private var smoothedPitchInput = 0f

    @JvmStatic
    fun isActive() = missileId >= 0

    @JvmStatic
    fun start(entityId: Int) {
        if (missileId != entityId) {
            if (!isActive()) {
                previousCameraType = mc.options.cameraType
            }
            missileId = entityId
            missingTicks = 0
            sessionTicks = 0
            hasSeenMissile = false
            smoothedYawInput = 0f
            smoothedPitchInput = 0f
        }
        mc.options.cameraType = CameraType.FIRST_PERSON
    }

    @JvmStatic
    fun stop(entityId: Int) {
        if (entityId != missileId) return
        stopLocal()
    }

    @JvmStatic
    fun cancel() {
        if (!isActive()) return
        val currentId = missileId
        sendPacketToServer(TvMissileControlMessage(currentId, 0f, 0f, cancel = true))
        stopLocal()
    }

    @JvmStatic
    fun controlledMissile(): WireGuideMissileEntity? {
        if (!isActive()) return null
        return mc.level?.getEntity(missileId) as? WireGuideMissileEntity
    }

    @JvmStatic
    fun sendSteering(mouseDeltaX: Float, mouseDeltaY: Float) {
        if (!isActive()) return

        val targetYaw = Mth.clamp(mouseDeltaX * MOUSE_TO_TURN_RATE, -MAX_INPUT, MAX_INPUT)
        val targetPitch = Mth.clamp(mouseDeltaY * MOUSE_TO_TURN_RATE, -MAX_INPUT, MAX_INPUT)
        smoothedYawInput = Mth.lerp(INPUT_SMOOTHING, smoothedYawInput, targetYaw)
        smoothedPitchInput = Mth.lerp(INPUT_SMOOTHING, smoothedPitchInput, targetPitch)

        sendPacketToServer(
            TvMissileControlMessage(
                missileId,
                smoothedYawInput,
                smoothedPitchInput
            )
        )
    }

    @JvmStatic
    fun cameraPosition(partialTick: Float): Vec3? {
        val missile = controlledMissile() ?: return null
        val base = Vec3(
            Mth.lerp(partialTick.toDouble(), missile.xo, missile.x),
            Mth.lerp(partialTick.toDouble(), missile.yo, missile.y),
            Mth.lerp(partialTick.toDouble(), missile.zo, missile.z)
        )
        val direction = flightDirection(missile)
        // Put the lens just ahead of the model so the missile body and exhaust never obscure the feed.
        return base.add(direction.scale(0.34)).add(0.0, 0.08, 0.0)
    }

    @JvmStatic
    fun cameraRotation(): Vec2? {
        val missile = controlledMissile() ?: return null
        val direction = flightDirection(missile)
        val horizontal = direction.horizontalDistance()
        val yaw = Math.toDegrees(-atan2(direction.x, direction.z)).toFloat()
        val pitch = Math.toDegrees(-atan2(direction.y, horizontal)).toFloat()
        return Vec2(yaw, pitch)
    }

    @JvmStatic
    fun elapsedTicks() = sessionTicks

    @SubscribeEvent
    fun clientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END || !isActive()) return

        sessionTicks++
        mc.options.cameraType = CameraType.FIRST_PERSON

        val player = mc.player
        val missile = controlledMissile()
        if (player == null || mc.level == null) {
            stopLocal()
            return
        }

        if (missile == null || missile.isRemoved) {
            missingTicks++
            val grace = if (hasSeenMissile) LOST_ENTITY_GRACE_TICKS else SPAWN_GRACE_TICKS
            if (missingTicks > grace) stopLocal()
            return
        }

        hasSeenMissile = true
        missingTicks = 0

        val vehicle = player.vehicle
        if (vehicle == null || vehicle.type != ModEntities.MI_28.get()) {
            stopLocal()
        }
    }

    private fun flightDirection(missile: WireGuideMissileEntity): Vec3 {
        val motion = missile.deltaMovement
        return if (motion.lengthSqr() > 1.0e-6) motion.normalize() else missile.lookAngle.normalize()
    }

    private fun stopLocal() {
        missileId = -1
        missingTicks = 0
        sessionTicks = 0
        hasSeenMissile = false
        smoothedYawInput = 0f
        smoothedPitchInput = 0f
        previousCameraType?.let { mc.options.cameraType = it }
        previousCameraType = null
    }

    private const val MOUSE_TO_TURN_RATE = 0.075f
    private const val MAX_INPUT = 7.5f
    private const val INPUT_SMOOTHING = 0.48f
    private const val SPAWN_GRACE_TICKS = 40
    private const val LOST_ENTITY_GRACE_TICKS = 5
}
