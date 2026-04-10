package com.perfkit.core.infrastructure

import com.perfkit.api.domain.ViolationEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bus interno de eventos de violação.
 *
 * - [replay] = 0: novos coletores não recebem eventos passados (snapshot vem do buffer).
 * - [extraBufferCapacity] = 64: absorve bursts de violações sem bloquear o emissor.
 * - [DROP_OLDEST]: descarta eventos mais antigos se o buffer estiver cheio,
 *   garantindo que o emissor nunca bloqueie (thread do penaltyListener é crítica).
 */
internal class ViolationEventBus {

    private val _events = MutableSharedFlow<ViolationEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val events: SharedFlow<ViolationEvent> = _events.asSharedFlow()

    /** Thread-safe via [MutableSharedFlow.tryEmit]. Nunca bloqueia. */
    fun emit(event: ViolationEvent) {
        _events.tryEmit(event)
    }
}
