package com.perfkit.strictmode

import android.content.Context
import android.content.pm.ApplicationInfo
import com.perfkit.core.PerfKit
import com.perfkit.strictmode.adapter.StrictModeAdapter

object StrictModePlugin {

    @Volatile
    private var installed = false

    @JvmStatic
    fun install(context: Context) {
        if (installed) return

        val config = PerfKit.config
        if (!config.enabled || !config.strictModeEnabled) return
        if (config.debugOnly && !isDebugBuild(context)) return

        StrictModeAdapter(config, PerfKit.violationSink).install()
        installed = true
    }

    @JvmStatic
    fun reset() {
        installed = false
    }

    private fun isDebugBuild(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
