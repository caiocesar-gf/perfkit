package com.perfkit.api.domain

/**
 * Representação de domínio de uma violação detectada pelo StrictMode.
 * Objeto imutável, pronto para exibição, persistência ou envio.
 */
data class ViolationEvent(
    /** UUID gerado no momento da captura. */
    val id: String,
    val timestamp: Long,
    val source: ViolationSource,
    val category: ViolationCategory,
    val severity: ViolationSeverity,
    val threadName: String,
    val message: String,
    val stacktrace: String?,
    /** Nome simples da classe de violação da plataforma (ex: "DiskReadViolation"). */
    val className: String?,
    /** Label da policy que originou a violação. */
    val policyLabel: String?,
    val metadata: Map<String, String> = emptyMap(),
)

/** Agregação de violações por categoria — usada no painel de resumo. */
data class ViolationSummary(
    val category: ViolationCategory,
    val count: Int,
    val lastTimestamp: Long,
    val highestSeverity: ViolationSeverity,
)

/** Aggregate root da sessão de monitoramento StrictMode. */
data class StrictModeSession(
    val isEnabled: Boolean,
    val startedAt: Long,
    val config: com.perfkit.api.config.PerfKitConfig,
)
