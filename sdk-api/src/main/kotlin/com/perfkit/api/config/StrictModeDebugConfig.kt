@file:Suppress("UNUSED")
package com.perfkit.api.config

/** @deprecated Use [PerfKitConfig]. Kept for source compatibility only. */
@Deprecated(
    message = "Use PerfKitConfig instead.",
    replaceWith = ReplaceWith("PerfKitConfig", "com.perfkit.api.config.PerfKitConfig"),
)
typealias StrictModeDebugConfig = PerfKitConfig
