package com.perfkit.api.usecase

import com.perfkit.api.domain.ViolationEvent
import com.perfkit.api.domain.ViolationSummary
import kotlinx.coroutines.flow.Flow

/**
 * Inicia a sessão de monitoramento e registra os listeners de StrictMode.
 * Deve ser chamado uma única vez, preferencialmente em [Application.onCreate].
 */
fun interface StartStrictModeMonitoring {
    operator fun invoke()
}

/**
 * Recebe uma violação bruta da plataforma, classifica, deduplica,
 * armazena no buffer e emite no stream de eventos.
 *
 * Implementado em sdk-core; chamado pelos adapters da plataforma (sdk-strictmode).
 */
fun interface ProcessViolation {
    operator fun invoke(rawViolation: com.perfkit.api.domain.RawViolation)
}

/**
 * Expõe o stream completo de violações para a UI de debug.
 * A primeira emissão contém o snapshot atual do buffer.
 */
fun interface ObserveViolations {
    operator fun invoke(): Flow<List<ViolationEvent>>
}

/** Expõe agregações por categoria, reativo a novas violações. */
fun interface ObserveViolationSummaries {
    operator fun invoke(): Flow<List<ViolationSummary>>
}

/** Liga/desliga o overlay de debug em tempo real. */
fun interface ToggleBubbleOverlay {
    operator fun invoke(enabled: Boolean)
}
