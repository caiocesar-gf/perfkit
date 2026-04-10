package com.perfkit.api.service

import com.perfkit.api.domain.RawViolation
import com.perfkit.api.domain.ViolationCategory
import com.perfkit.api.domain.ViolationEvent
import com.perfkit.api.domain.ViolationSeverity

/** Resultado da classificação de uma [RawViolation]. */
data class ViolationClassification(
    val category: ViolationCategory,
    val severity: ViolationSeverity,
)

/** Converte [RawViolation] em categoria e severidade de domínio. */
fun interface ViolationClassifier {
    fun classify(input: RawViolation): ViolationClassification
}

/** Gera texto human-friendly a partir de um [ViolationEvent]. */
fun interface ViolationFormatter {
    fun format(event: ViolationEvent): String
}

/**
 * Evita spam de eventos repetidos dentro de uma janela de tempo.
 * Retorna true se o evento deve ser emitido, false se deve ser descartado.
 */
fun interface ViolationDeduplicator {
    fun shouldEmit(event: ViolationEvent): Boolean
}

/** Mantém histórico recente de violações em memória (buffer circular). */
interface ViolationBuffer {
    fun add(event: ViolationEvent)
    fun getAll(): List<ViolationEvent>
    fun clear()
}

/** Abstração de log customizável — permite substituir por Timber, Logcat, etc. */
fun interface ViolationLogger {
    fun log(event: ViolationEvent)
}

/** Contrato para publicar eventos no overlay de debug em tempo real. */
interface BubbleNotifier {
    fun show(event: ViolationEvent)
    fun dismiss()
    fun updateCounter(count: Int)
}
