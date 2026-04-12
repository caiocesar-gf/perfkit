package com.perfkit.core

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import com.perfkit.api.config.PerfKitConfig
import com.perfkit.api.service.ViolationBuffer
import com.perfkit.api.usecase.ObserveViolationSummaries
import com.perfkit.api.usecase.ObserveViolations
import com.perfkit.api.usecase.ProcessViolation
import com.perfkit.core.domain.DefaultViolationClassifier
import com.perfkit.core.infrastructure.AndroidViolationLogger
import com.perfkit.core.infrastructure.CircularViolationBuffer
import com.perfkit.core.infrastructure.ThrottledViolationDeduplicator
import com.perfkit.core.infrastructure.ViolationEventBus
import com.perfkit.core.usecase.ObserveViolationsUseCase
import com.perfkit.core.usecase.ObserveViolationSummariesUseCase
import com.perfkit.core.usecase.ProcessViolationUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

object PerfKit {

    private const val TAG = "PerfKit"

    private var _processViolation: ProcessViolation = ProcessViolation { }
    private var _observeViolations: ObserveViolations = ObserveViolations { emptyFlow() }
    private var _observeSummaries: ObserveViolationSummaries = ObserveViolationSummaries { emptyFlow() }
    private var _buffer: ViolationBuffer? = null
    private var _config: PerfKitConfig = PerfKitConfig()

    @Volatile
    private var initialized = false

    val violationSink: ProcessViolation get() = _processViolation
    val observeViolations: ObserveViolations get() = _observeViolations
    val observeSummaries: ObserveViolationSummaries get() = _observeSummaries
    val config: PerfKitConfig get() = _config

    @JvmStatic
    fun initialize(
        context: Context,
        config: PerfKitConfig = PerfKitConfig(),
    ) {
        if (initialized) return
        if (!config.enabled) return
        if (config.debugOnly && !isDebugBuild(context)) return

        _config = config

        val eventBus = ViolationEventBus()
        val buffer = CircularViolationBuffer(config.maxBufferSize)
        val classifier = DefaultViolationClassifier()
        val deduplicator = ThrottledViolationDeduplicator(config.dedupWindowMs)
        val logger = config.logger ?: AndroidViolationLogger()

        val observeViolations = ObserveViolationsUseCase(eventBus, buffer)

        _buffer = buffer
        _processViolation = ProcessViolationUseCase(
            classifier = classifier,
            deduplicator = deduplicator,
            buffer = buffer,
            logger = logger,
            eventBus = eventBus,
            config = config,
        )
        _observeViolations = observeViolations
        _observeSummaries = ObserveViolationSummariesUseCase(observeViolations)

        initialized = true
        Log.i(TAG, "Initialized. SDK=${config.strictModeEnabled}, UI=${config.debugUiEnabled}")
    }

    // Reflection avoids a compile-time sdk-core → sdk-debug-ui circular dependency.
    @JvmStatic
    fun openDebugPanel(context: Context) {
        runCatching {
            val cls = Class.forName("com.perfkit.debugui.panel.ViolationPanelActivity")
            context.startActivity(
                Intent(context, cls).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        }.onFailure {
            Log.w(TAG, "Could not open debug panel. Is 'sdk-debug-ui' in your dependencies? ($it)")
        }
    }

    @JvmStatic
    fun clearViolations() {
        _buffer?.clear()
    }

    @JvmStatic
    fun reset() {
        initialized = false
        _processViolation = ProcessViolation { }
        _observeViolations = ObserveViolations { emptyFlow() }
        _observeSummaries = ObserveViolationSummaries { emptyFlow() }
        _buffer = null
        _config = PerfKitConfig()
    }

    private fun isDebugBuild(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
