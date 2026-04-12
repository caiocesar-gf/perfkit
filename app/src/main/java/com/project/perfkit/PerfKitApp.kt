package com.project.perfkit

import android.app.Application
import com.perfkit.api.config.PerfKitConfig
import com.perfkit.api.domain.ViolationSeverity
import com.perfkit.core.PerfKit
import com.perfkit.debugui.DebugUiPlugin
import com.perfkit.strictmode.StrictModePlugin

class PerfKitApp : Application() {

    override fun onCreate() {
        super.onCreate()

        PerfKit.initialize(
            this,
            PerfKitConfig(
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
        )
        StrictModePlugin.install(this)
        DebugUiPlugin.install(this)
    }
}
