package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.client.TvMissileClientHandler
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Serializable

@Serializable
data class TvMissileControlEndMessage(val missileId: Int) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        TvMissileClientHandler.stop(missileId)
    }
}
