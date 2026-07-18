package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.projectile.TvMissileVisualState
import com.atsuishio.superbwarfare.entity.projectile.WireGuideMissileEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation

/**
 * Gives the TV-guided missile the full-size 9M336 body, skin, and animated exhaust flare.
 * Its entity keeps the TV-guidance flight behavior and the same medium missile particle trail.
 */
class TvGuidedMissileRenderer(context: EntityRendererProvider.Context) :
    BasicProjectileRenderer<WireGuideMissileEntity>(context) {

    override fun getTextureLocation(entity: WireGuideMissileEntity): ResourceLocation = TEXTURE

    override fun getModelLocation(entity: WireGuideMissileEntity): ResourceLocation = MODEL

    override fun render(
        entity: WireGuideMissileEntity,
        yaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        // The local TV lens does not need to render the body or emissive exhaust
        // surrounding it. External observers still receive the full missile model.
        if (TvMissileVisualState.isLocalControlled(entity.id)) return
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight)
    }

    companion object {
        private val MODEL = loc("models/bedrock/projectile/ru_9m336_missile.geo.json")
        private val TEXTURE = loc("textures/bedrock/projectile/ru_9m336_missile.png")
    }
}
