package com.perfkit.api.domain

/**
 * DTO na fronteira entre infraestrutura (plataforma Android) e domínio.
 *
 * Representa a violação próxima da plataforma, antes de classificação e enriquecimento.
 * Criado pelo adapter de StrictMode (sdk-strictmode) e consumido pelo
 * [com.perfkit.api.usecase.ProcessViolation] (sdk-core).
 */
data class RawViolation(
    val source: ViolationSource,
    /** O [Throwable] original emitido pela plataforma via penaltyListener. */
    val violation: Throwable,
    val threadName: String,
    val timestamp: Long,
)
