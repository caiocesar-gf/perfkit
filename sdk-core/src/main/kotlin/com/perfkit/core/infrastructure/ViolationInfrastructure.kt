package com.perfkit.core.infrastructure

import android.util.Log
import com.perfkit.api.domain.ViolationEvent
import com.perfkit.api.domain.ViolationSeverity
import com.perfkit.api.service.ViolationBuffer
import com.perfkit.api.service.ViolationDeduplicator
import com.perfkit.api.service.ViolationFormatter
import com.perfkit.api.service.ViolationLogger
import java.util.concurrent.ConcurrentHashMap

internal class DefaultViolationFormatter : ViolationFormatter {
    override fun format(event: ViolationEvent): String = buildString {
        append("[${event.severity}][${event.category}] ")
        append("thread=${event.threadName} | ")
        append(event.message.ifBlank { event.className ?: "?" })
        event.stacktrace?.let { append("\n").append(it) }
    }
}

internal class AndroidViolationLogger(
    private val formatter: ViolationFormatter = DefaultViolationFormatter(),
) : ViolationLogger {

    override fun log(event: ViolationEvent) {
        val tag = "PerfKit/${event.category.name}"
        val msg = formatter.format(event)
        when (event.severity) {
            ViolationSeverity.LOW -> Log.d(tag, msg)
            ViolationSeverity.MEDIUM -> Log.w(tag, msg)
            ViolationSeverity.HIGH -> Log.e(tag, msg)
            ViolationSeverity.CRITICAL -> Log.e(tag, "⚠️ CRITICAL — $msg")
        }
    }
}

internal class CircularViolationBuffer(private val maxSize: Int) : ViolationBuffer {

    private val buffer = ArrayDeque<ViolationEvent>(maxSize)

    @Synchronized
    override fun add(event: ViolationEvent) {
        if (buffer.size >= maxSize) buffer.removeFirst()
        buffer.addLast(event)
    }

    @Synchronized
    override fun getAll(): List<ViolationEvent> = buffer.toList()

    @Synchronized
    override fun clear() = buffer.clear()
}

internal class ThrottledViolationDeduplicator(
    private val dedupWindowMs: Long,
) : ViolationDeduplicator {

    private val seen = ConcurrentHashMap<String, Long>(64)

    override fun shouldEmit(event: ViolationEvent): Boolean {
        val key = "${event.category}|${event.className}|${event.stacktrace?.take(200)}"
        val now = System.currentTimeMillis()
        val lastSeen = seen[key]

        return if (lastSeen == null || (now - lastSeen) > dedupWindowMs) {
            seen[key] = now
            if (seen.size > 500) {
                seen.entries.removeIf { (_, time) -> now - time > dedupWindowMs * 2 }
            }
            true
        } else {
            false
        }
    }
}
