package com.atsuishio.superbwarfare.entity.projectile

/**
 * Dist-safe local visual state shared by the client controller and common projectile code.
 * Dedicated servers leave the ID at -1 and never suppress effects.
 */
object TvMissileVisualState {
    @Volatile
    private var localControlledMissileId = -1

    fun setLocalControlled(entityId: Int) {
        localControlledMissileId = entityId
    }

    fun clearLocalControlled(entityId: Int) {
        if (localControlledMissileId == entityId) {
            localControlledMissileId = -1
        }
    }

    fun isLocalControlled(entityId: Int): Boolean = localControlledMissileId == entityId
}
