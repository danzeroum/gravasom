package com.gravasom.core.domain.marker

/** "Ir ao marcador anterior/proximo" (definicao do plano v2, item 3). */
object MarkerNavigation {

    /** Se o marcador anterior estiver a menos disso, ele e considerado "o atual" e o botao vai ao anterior a ele. */
    const val PREVIOUS_GRACE_MILLIS = 1_500L

    fun next(markers: List<Marker>, positionMillis: Long): Marker? =
        markers.filter { it.positionMillis > positionMillis }.minByOrNull { it.positionMillis }

    fun previous(markers: List<Marker>, positionMillis: Long): Marker? =
        markers.filter { it.positionMillis <= positionMillis - PREVIOUS_GRACE_MILLIS }.maxByOrNull { it.positionMillis }
}
