package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.network.message.receive.TvMissileControlEndMessage
import com.atsuishio.superbwarfare.network.message.receive.TvMissileControlStartMessage
import com.atsuishio.superbwarfare.tools.sendPacketTo
import net.minecraft.sounds.SoundEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import java.util.*

open class WireGuideMissileEntity(type: EntityType<out WireGuideMissileEntity>, level: Level) :
    MissileProjectile(type, level), BasicGeoProjectileEntity {

    var launcherVehicleUUID: UUID? = null

    private var tvControlStarted = false
    private var tvYawInput = 0f
    private var tvPitchInput = 0f
    private var lastTvInputTick = -100

    override fun getDefaultItem(): Item {
        return ModItems.MEDIUM_ANTI_GROUND_MISSILE.get()
    }

    override fun tick() {
        super.tick()
        mediumTrail()

        if (!isAlive) return

        if (!level().isClientSide) {
            if (!tvControlStarted && tickCount <= TV_START_WINDOW_TICKS) {
                tryStartTvControl()
            }

            if (tvControlStarted) {
                val controller = owner as? ServerPlayer
                if (controller == null || !isTvControllerEligible(controller)
                    || controller.distanceToSqr(this) > MAX_TV_CONTROL_RANGE * MAX_TV_CONTROL_RANGE
                ) {
                    endTvControl()
                } else {
                    applyTvSteering()
                    return
                }
            }
        }

        val owner = this.owner
        val vehicle = owner?.vehicle
        if (tickCount > 0 && owner != null && vehicle is VehicleEntity) {
            var toVec = deltaMovement
            this.deltaMovement = this.deltaMovement.scale(0.5).add(lookAngle.scale(2.0))

            if (launcherVehicleUUID == vehicle.uuid) {
                val lookVec =
                    if ((vehicle.vehicleType == VehicleType.AIRPLANE || vehicle.vehicleType == VehicleType.HELICOPTER)
                        && owner == vehicle.getFirstPassenger()
                    ) {
                        vehicle.getViewVector(1f).scale(1.6)
                    } else {
                        vehicle.getBarrelVector(1f).scale(1.6)
                    }
                val missileVec = vehicle.getShootPosForHud(owner, 1f).vectorTo(position()).normalize()
                toVec = missileVec.vectorTo(lookVec)
            }

            turn(toVec, ((tickCount - 1) * 0.4f).coerceIn(0f, 6f))
        }
    }

    override fun getSound(): SoundEvent {
        return ModSounds.ROCKET_FLY.get()
    }

    override fun getVolume(): Float {
        return 0.4f
    }

    fun setLauncherVehicle(uuid: UUID?) {
        this.launcherVehicleUUID = uuid
    }

    /**
     * Accepts one tick of TV-guidance input. The packet handler calls this only on the server.
     * All authority checks are repeated here so a client cannot steer another player's missile.
     */
    fun acceptTvControlInput(player: ServerPlayer, yawInput: Float, pitchInput: Float, cancel: Boolean) {
        if (!tvControlStarted || owner?.uuid != player.uuid || !isTvControllerEligible(player)) return

        if (cancel) {
            endTvControl()
            return
        }

        tvYawInput = Mth.clamp(yawInput, -MAX_TURN_PER_TICK, MAX_TURN_PER_TICK)
        tvPitchInput = Mth.clamp(pitchInput, -MAX_TURN_PER_TICK, MAX_TURN_PER_TICK)
        lastTvInputTick = tickCount
    }

    private fun tryStartTvControl() {
        val controller = owner as? ServerPlayer ?: return
        if (!isTvControllerEligible(controller)) return

        tvControlStarted = true
        lastTvInputTick = tickCount
        sendPacketTo(controller, TvMissileControlStartMessage(id))
    }

    private fun isTvControllerEligible(player: ServerPlayer): Boolean {
        val vehicle = player.vehicle as? VehicleEntity ?: return false
        return vehicle.type == ModEntities.MI_28.get()
                && vehicle.uuid == launcherVehicleUUID
                && vehicle.getSeatIndex(player) == MI_28_GUNNER_SEAT
    }

    private fun applyTvSteering() {
        if (tickCount - lastTvInputTick > INPUT_TIMEOUT_TICKS) {
            tvYawInput = 0f
            tvPitchInput = 0f
        }

        // Ease steering in just after launch so the missile clears the helicopter before becoming agile.
        val launchAuthority = ((tickCount - 1) / 6f).coerceIn(0.25f, 1f)
        yRot = Mth.wrapDegrees(yRot + tvYawInput * launchAuthority)
        xRot = Mth.clamp(xRot + tvPitchInput * launchAuthority, MIN_TV_PITCH, MAX_TV_PITCH)

        val speed = deltaMovement.length().coerceIn(MIN_TV_SPEED, MAX_TV_SPEED)
        val currentDirection = if (deltaMovement.lengthSqr() > 1.0e-6) deltaMovement.normalize() else lookAngle
        val commandedDirection = Vec3.directionFromRotation(xRot, yRot)
        val blendedDirection = currentDirection.scale(1.0 - STEERING_RESPONSE)
            .add(commandedDirection.scale(STEERING_RESPONSE))
            .normalize()
        deltaMovement = blendedDirection.scale(speed)

        // Input is refreshed every client tick; decay avoids a stuck turn if packets briefly stop.
        tvYawInput *= INPUT_DECAY
        tvPitchInput *= INPUT_DECAY
    }

    private fun endTvControl() {
        if (!tvControlStarted) return
        tvControlStarted = false
        tvYawInput = 0f
        tvPitchInput = 0f
        (owner as? ServerPlayer)?.let { sendPacketTo(it, TvMissileControlEndMessage(id)) }
    }

    override fun remove(reason: Entity.RemovalReason) {
        if (!level().isClientSide) {
            endTvControl()
        }
        super.remove(reason)
    }

    override val maxHealth: Float
        get() = 20f

    companion object {
        const val MI_28_GUNNER_SEAT = 1
        const val MAX_TV_CONTROL_RANGE = 1024.0

        private const val TV_START_WINDOW_TICKS = 3
        private const val INPUT_TIMEOUT_TICKS = 3
        private const val MAX_TURN_PER_TICK = 7.5f
        private const val MIN_TV_PITCH = -80f
        private const val MAX_TV_PITCH = 80f
        private const val MIN_TV_SPEED = 2.35
        private const val MAX_TV_SPEED = 3.25
        private const val STEERING_RESPONSE = 0.82
        private const val INPUT_DECAY = 0.65f
    }
}
