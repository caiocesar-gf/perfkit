package com.project.perfkit

import android.app.Application
import com.perfkit.api.config.PerfKitConfig
import com.perfkit.api.domain.ViolationSeverity
import com.perfkit.core.PerfKit
import com.perfkit.debugui.DebugUiPlugin
import com.perfkit.strictmode.StrictModePlugin

/**
 * Application class do sample app do PerfKit SDK.
 *
 * Demonstra a integração mínima em 3 linhas após declarar a config:
 * 1. [PerfKit.initialize] — configura infraestrutura central
 * 2. [StrictModePlugin.install] — ativa captura de violações
 * 3. [DebugUiPlugin.install] — ativa notificação de debug em tempo real
 */
class PerfKitApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = PerfKitConfig(
            enabled = true,
            debugOnly = true,
            strictModeEnabled = true,
            debugUiEnabled = true,
            detectDiskReads = true,
            detectDiskWrites = true,
            detectNetwork = true,
            detectCustomSlowCalls = true,
            detectLeakedClosableObjects = true,
            detectActivityLeaks = true,
            detectCleartextNetwork = true,
            maxBufferSize = 200,
            dedupWindowMs = 2_000L,
            minSeverityToDisplay = ViolationSeverity.LOW,
        )

        PerfKit.initialize(this, config)    // 1. sempre primeiro
        StrictModePlugin.install(this)      // 2. instala penaltyListener
        DebugUiPlugin.install(this)         // 3. ativa overlay de debug
    }
}
