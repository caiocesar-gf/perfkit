package com.perfkit.strictmode.adapter

import android.os.Build
import android.os.StrictMode
import androidx.annotation.RequiresApi
import com.perfkit.api.config.PerfKitConfig
import com.perfkit.api.usecase.ProcessViolation
import com.perfkit.strictmode.mapper.ViolationMapper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Adapter que integra [StrictMode] com o pipeline de domínio do PerfKit.
 *
 * ## Estratégia (API 28+)
 * 1. **Preserva política existente** via `Builder(existingPolicy)` — não sobrescreve
 *    detecções já configuradas pelo desenvolvedor.
 * 2. **Adiciona só o necessário** — cada detection flag é aplicado conforme [PerfKitConfig].
 * 3. **Executor isolado** — listener roda em thread daemon de prioridade mínima;
 *    nunca bloqueia a thread que causou a violação.
 * 4. **penaltyLog() mantido** — violações aparecem no Logcat independentemente do SDK.
 *
 * ## Fallback (API 24–27)
 * `penaltyListener` não disponível. Aplica políticas de detecção + `penaltyLog()`.
 * Violações ficam visíveis no Logcat mas não são capturadas programaticamente.
 */
internal class StrictModeAdapter(
    private val config: PerfKitConfig,
    private val sink: ProcessViolation,
) {
    private val executor: Executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "perfkit-strictmode-listener").also {
            it.priority = Thread.MIN_PRIORITY
            it.isDaemon = true
        }
    }

    fun install() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            installThreadPolicy()
            installVmPolicy()
        } else {
            installLegacy()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun installThreadPolicy() {
        val builder = StrictMode.ThreadPolicy.Builder(StrictMode.getThreadPolicy())

        if (config.detectDiskReads) builder.detectDiskReads()
        if (config.detectDiskWrites) builder.detectDiskWrites()
        if (config.detectNetwork) builder.detectNetwork()
        if (config.detectCustomSlowCalls) builder.detectCustomSlowCalls()
        if (config.detectResourceMismatches) builder.detectResourceMismatches()

        builder.penaltyLog()
        builder.penaltyListener(executor) { violation ->
            sink(ViolationMapper.fromThreadViolation(violation))
        }

        StrictMode.setThreadPolicy(builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun installVmPolicy() {
        val builder = StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())

        if (config.detectLeakedClosableObjects) builder.detectLeakedClosableObjects()
        if (config.detectActivityLeaks) builder.detectActivityLeaks()
        if (config.detectLeakedRegistrationObjects) builder.detectLeakedRegistrationObjects()
        if (config.detectFileUriExposure) builder.detectFileUriExposure()
        if (config.detectCleartextNetwork && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.detectCleartextNetwork()
        }

        builder.penaltyLog()
        builder.penaltyListener(executor) { violation ->
            sink(ViolationMapper.fromVmViolation(violation))
        }

        StrictMode.setVmPolicy(builder.build())
    }

    private fun installLegacy() {
        val threadBuilder = StrictMode.ThreadPolicy.Builder(StrictMode.getThreadPolicy())
        if (config.detectDiskReads) threadBuilder.detectDiskReads()
        if (config.detectDiskWrites) threadBuilder.detectDiskWrites()
        if (config.detectNetwork) threadBuilder.detectNetwork()
        if (config.detectCustomSlowCalls) threadBuilder.detectCustomSlowCalls()
        threadBuilder.penaltyLog()
        StrictMode.setThreadPolicy(threadBuilder.build())

        val vmBuilder = StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
        if (config.detectLeakedClosableObjects) vmBuilder.detectLeakedClosableObjects()
        if (config.detectActivityLeaks) vmBuilder.detectActivityLeaks()
        if (config.detectFileUriExposure) vmBuilder.detectFileUriExposure()
        vmBuilder.penaltyLog()
        StrictMode.setVmPolicy(vmBuilder.build())

        android.util.Log.i(
            "PerfKit/StrictMode",
            "penaltyListener requires API 28+. Violations are Logcat-only on this device (API ${Build.VERSION.SDK_INT}).",
        )
    }
}
