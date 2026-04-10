package com.perfkit.api.domain

/** Origem da violação detectada pelo StrictMode. */
enum class ViolationSource(val label: String) {
    THREAD_POLICY("ThreadPolicy"),
    VM_POLICY("VmPolicy"),
}

/**
 * Tipo funcional da violação.
 * Mapeado a partir dos subtipos de [android.os.strictmode.Violation].
 */
enum class ViolationCategory {
    DISK_READ,
    DISK_WRITE,
    NETWORK,
    SLOW_CALL,
    LEAKED_RESOURCE,
    RESOURCE_MISMATCH,
    CLEARTEXT_NETWORK,
    UNTAGGED_SOCKET,
    CUSTOM,
    UNKNOWN,
}

/**
 * Nível de impacto percebido da violação.
 * Declarado em ordem crescente de severidade para permitir [Comparable] via ordinal.
 */
enum class ViolationSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}
