package com.perfkit.api.config

import com.perfkit.api.domain.ViolationCategory
import com.perfkit.api.domain.ViolationSeverity
import com.perfkit.api.service.ViolationLogger

/**
 * Configuração pública da versão 1.0 do PerfKit SDK.
 *
 * Design goal: mínimo de campos para integração, máximo de sensatos defaults.
 *
 * ### Exemplo de uso
 * ```kotlin
 * PerfKit.initialize(
 *     context = this,
 *     config = PerfKitConfig(
 *         strictModeEnabled = true,
 *         debugUiEnabled = true,
 *         minSeverityToDisplay = ViolationSeverity.MEDIUM,
 *     )
 * )
 * ```
 */
data class PerfKitConfig(

    // --- Global ---

    /** Liga/desliga o SDK completamente sem remover a dependência. */
    val enabled: Boolean = true,

    /**
     * Se `true` (padrão), a inicialização é bloqueada quando o build não é debuggable.
     * Garante que o SDK nunca rode em produção com configuração default.
     */
    val debugOnly: Boolean = true,

    // --- Módulos ---

    /** Ativa o adapter de StrictMode (sdk-strictmode). */
    val strictModeEnabled: Boolean = true,

    /** Ativa o overlay de debug visual (sdk-debug-ui). */
    val debugUiEnabled: Boolean = true,

    // --- StrictMode: Thread detections ---

    val detectDiskReads: Boolean = true,
    val detectDiskWrites: Boolean = true,
    val detectNetwork: Boolean = true,
    val detectCustomSlowCalls: Boolean = true,
    val detectResourceMismatches: Boolean = true,

    // --- StrictMode: VM detections ---

    val detectLeakedClosableObjects: Boolean = true,
    val detectActivityLeaks: Boolean = true,
    val detectCleartextNetwork: Boolean = true,
    val detectLeakedRegistrationObjects: Boolean = true,
    val detectFileUriExposure: Boolean = true,

    // --- Processing ---

    /** Tamanho máximo do buffer circular em memória. */
    val maxBufferSize: Int = 200,

    /**
     * Janela de deduplicação em ms.
     * Violações com a mesma assinatura dentro desta janela são descartadas (RF-09).
     */
    val dedupWindowMs: Long = 2_000L,

    /** Severidade mínima para notificação no overlay de debug. */
    val minSeverityToDisplay: ViolationSeverity = ViolationSeverity.MEDIUM,

    /** Categorias explicitamente ignoradas no pipeline. */
    val ignoredCategories: Set<ViolationCategory> = emptySet(),

    // --- Extensão ---

    /**
     * Logger customizado. Se `null`, usa a implementação padrão
     * baseada em [android.util.Log] com tag `PerfKit/<CATEGORY>`.
     */
    val logger: ViolationLogger? = null,
) {
    fun isCategoryEnabled(category: ViolationCategory): Boolean =
        category !in ignoredCategories
}
