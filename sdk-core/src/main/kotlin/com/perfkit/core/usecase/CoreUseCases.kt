package com.perfkit.core.usecase

import com.perfkit.api.domain.RawViolation
import com.perfkit.api.domain.ViolationEvent
import com.perfkit.api.domain.ViolationSummary
import com.perfkit.api.service.ViolationBuffer
import com.perfkit.api.service.ViolationClassifier
import com.perfkit.api.service.ViolationDeduplicator
import com.perfkit.api.service.ViolationLogger
import com.perfkit.api.usecase.ObserveViolations
import com.perfkit.api.usecase.ObserveViolationSummaries
import com.perfkit.api.usecase.ProcessViolation
import com.perfkit.core.infrastructure.ViolationEventBus
import com.perfkit.api.config.PerfKitConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

// ---------------------------------------------------------------------------
// ProcessViolation
// ---------------------------------------------------------------------------

/**
 * Orquestra o pipeline completo ao receber uma [RawViolation]:
 * classifica → filtra → deduplica → armazena → loga → emite no bus.
 *
 * Thread-safe: chamado pelo executor do penaltyListener (background thread).
 */
internal class ProcessViolationUseCase(
    private val classifier: ViolationClassifier,
    private val deduplicator: ViolationDeduplicator,
    private val buffer: ViolationBuffer,
    private val logger: ViolationLogger,
    private val eventBus: ViolationEventBus,
    private val config: PerfKitConfig,
) : ProcessViolation {

    override fun invoke(rawViolation: RawViolation) {
        val classification = classifier.classify(rawViolation)

        // RF-06 — filtro por categoria
        if (!config.isCategoryEnabled(classification.category)) return

        val event = ViolationEvent(
            id = UUID.randomUUID().toString(),
            timestamp = rawViolation.timestamp,
            source = rawViolation.source,
            category = classification.category,
            severity = classification.severity,
            threadName = rawViolation.threadName,
            message = rawViolation.violation.message ?: rawViolation.violation.javaClass.simpleName,
            stacktrace = formatStacktrace(rawViolation.violation),
            className = rawViolation.violation.javaClass.simpleName,
            policyLabel = rawViolation.source.label,
        )

        // RF-09 — deduplicação / throttle
        if (!deduplicator.shouldEmit(event)) return

        // RF-08 — buffer em memória
        buffer.add(event)
        // RF-03 — log customizado
        logger.log(event)
        // emite para observers (UI, BubbleNotifier, etc.)
        eventBus.emit(event)
    }

    private fun formatStacktrace(t: Throwable): String? {
        val frames = t.stackTrace
            .filterNot { frame ->
                // Remove frames internos do StrictMode para focar no código do app
                frame.className.startsWith("android.os.StrictMode") ||
                frame.className.startsWith("android.os.strictmode") ||
                frame.className.startsWith("java.lang.reflect.")
            }
            .take(20)
        return frames
            .takeIf { it.isNotEmpty() }
            ?.joinToString("\n") { "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }
    }
}

// ---------------------------------------------------------------------------
// ObserveViolations
// ---------------------------------------------------------------------------

/**
 * Retorna um [Flow] reativo de snapshots do buffer.
 *
 * - Primeira emissão: snapshot atual (RF-08).
 * - Emissões subsequentes: snapshot atualizado a cada nova violação.
 *
 * Usar [channelFlow] para garantir emissão segura no coletor.
 */
internal class ObserveViolationsUseCase(
    private val eventBus: ViolationEventBus,
    private val buffer: ViolationBuffer,
) : ObserveViolations {

    override fun invoke(): Flow<List<ViolationEvent>> = channelFlow {
        // snapshot inicial
        send(buffer.getAll())
        // snapshot atualizado a cada nova violação
        eventBus.events.collect { _ ->
            send(buffer.getAll())
        }
    }
}

// ---------------------------------------------------------------------------
// ObserveViolationSummaries
// ---------------------------------------------------------------------------

/**
 * Agrega violações por categoria, derivado de [ObserveViolations].
 * Ordena por severidade decrescente para destacar os problemas mais críticos.
 */
internal class ObserveViolationSummariesUseCase(
    private val observeViolations: ObserveViolations,
) : ObserveViolationSummaries {

    override fun invoke(): Flow<List<ViolationSummary>> =
        observeViolations().map { violations ->
            violations
                .groupBy { it.category }
                .map { (category, events) ->
                    ViolationSummary(
                        category = category,
                        count = events.size,
                        lastTimestamp = events.maxOf { it.timestamp },
                        highestSeverity = events.maxOf { it.severity },
                    )
                }
                .sortedByDescending { it.highestSeverity }
        }
}
