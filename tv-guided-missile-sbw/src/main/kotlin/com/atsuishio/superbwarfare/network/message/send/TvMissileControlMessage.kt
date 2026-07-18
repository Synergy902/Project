package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.projectile.WireGuideMissileEntity
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import kotlinx.serialization.Serializable

@Serializable
data class TvMissileControlMessage(
    val missileId: Int,
    val yawInput: Float,
    val pitchInput: Float,
    val cancel: Boolean = false
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val missile = player.level().getEntity(missileId) as? WireGuideMissileEntity ?: return
        missile.acceptTvControlInput(player, yawInput, pitchInput, cancel)
    }
}
