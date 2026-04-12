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

        if (!deduplicator.shouldEmit(event)) return

        buffer.add(event)
        logger.log(event)
        eventBus.emit(event)
    }

    private fun formatStacktrace(t: Throwable): String? {
        val frames = t.stackTrace
            .filterNot { frame ->
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

internal class ObserveViolationsUseCase(
    private val eventBus: ViolationEventBus,
    private val buffer: ViolationBuffer,
) : ObserveViolations {

    override fun invoke(): Flow<List<ViolationEvent>> = channelFlow {
        send(buffer.getAll())
        eventBus.events.collect { _ ->
            send(buffer.getAll())
        }
    }
}

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
