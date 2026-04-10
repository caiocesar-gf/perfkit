package com.perfkit.strictmode

import android.content.Context
import android.content.pm.ApplicationInfo
import com.perfkit.core.PerfKit
import com.perfkit.strictmode.adapter.StrictModeAdapter

/**
 * Plugin de StrictMode para o PerfKit SDK.
 *
 * Instala listeners de violação no [android.os.StrictMode] e encaminha
 * os eventos para o pipeline central via [PerfKit.violationSink].
 *
 * ### Pré-requisito
 * [PerfKit.initialize] deve ser chamado **antes** de [install].
 *
 * ### Uso mínimo
 * ```kotlin
 * // Application.onCreate()
 * PerfKit.initialize(this, PerfKitConfig())
 * StrictModePlugin.install(this)          // usa a config do PerfKit
 * ```
 */
object StrictModePlugin {

    @Volatile
    private var installed = false

    /**
     * Instala os listeners de StrictMode usando a [PerfKit.config] ativa.
     *
     * Idempotente: múltiplas chamadas são ignoradas após a primeira.
     *
     * @param context Usado para checar se o build é debuggable. Não é retido.
     */
    @JvmStatic
    fun install(context: Context) {
        if (installed) return

        val config = PerfKit.config
        if (!config.enabled || !config.strictModeEnabled) return
        if (config.debugOnly && !isDebugBuild(context)) return

        StrictModeAdapter(config, PerfKit.violationSink).install()
        installed = true
    }

    /** Reseta o estado — use APENAS em testes. */
    @JvmStatic
    fun reset() {
        installed = false
    }

    private fun isDebugBuild(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
