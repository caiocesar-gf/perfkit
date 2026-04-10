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

/**
 * Ponto de entrada público do PerfKit SDK (v1.0).
 *
 * ### Integração mínima em [Application.onCreate]:
 * ```kotlin
 * PerfKit.initialize(this, PerfKitConfig())
 * StrictModePlugin.install(this)   // sdk-strictmode
 * DebugUiPlugin.install(this)      // sdk-debug-ui
 * ```
 *
 * ### Por que 3 chamadas?
 * O SDK é modular por design — cada módulo é um AAR independente.
 * Isso permite incluir apenas o que você precisa:
 * - Só logs? Use só `sdk-core` + `sdk-strictmode`.
 * - Sem UI? Omita `sdk-debug-ui`.
 *
 * ### Thread safety
 * [initialize] é idempotente e thread-safe.
 * [violationSink], [observeViolations] e [observeSummaries] são thread-safe.
 */
object PerfKit {

    private const val TAG = "PerfKit"

    // No-ops padrão enquanto não inicializado
    private var _processViolation: ProcessViolation = ProcessViolation { /* no-op */ }
    private var _observeViolations: ObserveViolations = ObserveViolations { emptyFlow() }
    private var _observeSummaries: ObserveViolationSummaries = ObserveViolationSummaries { emptyFlow() }
    private var _buffer: ViolationBuffer? = null
    private var _config: PerfKitConfig = PerfKitConfig()

    @Volatile
    private var initialized = false

    // ------------------------------------------------------------------
    // Pontos de extensão para módulos externos (sdk-strictmode, sdk-debug-ui)
    // ------------------------------------------------------------------

    /**
     * Sink de violações para os adapters de plataforma (sdk-strictmode).
     * Thread-safe via [ProcessViolationUseCase].
     */
    val violationSink: ProcessViolation get() = _processViolation

    /** Exposto para sdk-debug-ui e integradores externos. */
    val observeViolations: ObserveViolations get() = _observeViolations

    /** Exposto para sdk-debug-ui e integradores externos. */
    val observeSummaries: ObserveViolationSummaries get() = _observeSummaries

    /** Configuração ativa (somente leitura após inicialização). */
    val config: PerfKitConfig get() = _config

    // ------------------------------------------------------------------
    // API pública v1.0
    // ------------------------------------------------------------------

    /**
     * Inicializa a infraestrutura central do PerfKit SDK.
     *
     * - Idempotente: chamadas subsequentes são ignoradas.
     * - Respeitoso: não sobrescreve políticas existentes do StrictMode.
     * - Seguro: bloqueia automaticamente em release se [PerfKitConfig.debugOnly] = true.
     *
     * @param context Deve ser o ApplicationContext para evitar memory leaks.
     * @param config  Configuração do SDK. Defaults cobrem o caso de uso principal.
     */
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

    /**
     * Abre o painel de debug de violações.
     *
     * Requer que `sdk-debug-ui` esteja nas dependências do app.
     * Usa resolução por nome de classe para evitar dependência de compilação circular.
     */
    @JvmStatic
    fun openDebugPanel(context: Context) {
        runCatching {
            val cls = Class.forName("com.perfkit.debugui.panel.ViolationPanelActivity")
            val intent = Intent(context, cls).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            Log.w(TAG, "Could not open debug panel. Is 'sdk-debug-ui' in your dependencies? ($it)")
        }
    }

    /**
     * Limpa o histórico de violações do buffer em memória.
     * Não afeta eventos já emitidos no stream [observeViolations].
     */
    @JvmStatic
    fun clearViolations() {
        _buffer?.clear()
    }

    // ------------------------------------------------------------------
    // Uso interno / testes
    // ------------------------------------------------------------------

    /** Reseta o estado — use APENAS em testes. */
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
