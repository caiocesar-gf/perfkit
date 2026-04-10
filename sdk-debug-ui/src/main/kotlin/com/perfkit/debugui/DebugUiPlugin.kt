package com.perfkit.debugui

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.perfkit.core.PerfKit
import com.perfkit.debugui.bubble.NotificationBubbleNotifier
import kotlinx.coroutines.launch

/**
 * Plugin de UI de debug para o PerfKit SDK.
 *
 * Inicia a [NotificationBubbleNotifier] que observa o stream de violações
 * e atualiza a notificação sticky em tempo real.
 *
 * ### Pré-requisito
 * [PerfKit.initialize] deve ser chamado **antes** de [install].
 *
 * ### Uso mínimo
 * ```kotlin
 * // Application.onCreate()
 * PerfKit.initialize(this, PerfKitConfig())
 * StrictModePlugin.install(this)
 * DebugUiPlugin.install(this)
 * ```
 */
object DebugUiPlugin {

    @Volatile
    private var installed = false

    @JvmStatic
    fun install(context: Context) {
        if (installed) return

        val config = PerfKit.config
        if (!config.enabled || !config.debugUiEnabled) return
        if (config.debugOnly && !isDebugBuild(context)) return

        val appContext = context.applicationContext
        val notifier = NotificationBubbleNotifier(appContext)

        // Observa em ProcessLifecycleOwner para sobreviver a recriações de Activity
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            PerfKit.observeViolations().collect { violations ->
                val latest = violations.lastOrNull() ?: return@collect
                if (latest.severity >= config.minSeverityToDisplay) {
                    notifier.show(latest)
                }
            }
        }

        installed = true
    }

    @JvmStatic
    fun reset() {
        installed = false
    }

    private fun isDebugBuild(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
