package com.perfkit.core.domain

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.perfkit.api.domain.RawViolation
import com.perfkit.api.domain.ViolationCategory
import com.perfkit.api.domain.ViolationSeverity
import com.perfkit.api.service.ViolationClassification
import com.perfkit.api.service.ViolationClassifier

/**
 * Implementação padrão do classificador de violações.
 *
 * Mapeia subtipos de [android.os.strictmode.Violation] (API 28+) para
 * [ViolationCategory] e [ViolationSeverity] de domínio.
 *
 * Para API < 28, usa string-matching no nome simplificado da classe — sem
 * reflection agressiva, sem dependência em classes ausentes no runtime.
 *
 * @param sdkVersion Injetável para testes unitários sem framework Android.
 */
internal class DefaultViolationClassifier(
    private val sdkVersion: Int = Build.VERSION.SDK_INT,
) : ViolationClassifier {

    override fun classify(input: RawViolation): ViolationClassification {
        return if (sdkVersion >= Build.VERSION_CODES.P) {
            classifyModern(input.violation)
        } else {
            classifyByClassName(input.violation.javaClass.simpleName)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun classifyModern(v: Throwable): ViolationClassification = when {
        v is android.os.strictmode.DiskReadViolation ->
            ViolationClassification(ViolationCategory.DISK_READ, ViolationSeverity.MEDIUM)

        v is android.os.strictmode.DiskWriteViolation ->
            ViolationClassification(ViolationCategory.DISK_WRITE, ViolationSeverity.MEDIUM)

        v is android.os.strictmode.NetworkViolation ->
            ViolationClassification(ViolationCategory.NETWORK, ViolationSeverity.HIGH)

        v is android.os.strictmode.SlowCallViolation ->
            ViolationClassification(ViolationCategory.SLOW_CALL, ViolationSeverity.MEDIUM)

        v is android.os.strictmode.LeakedClosableViolation ||
        v is android.os.strictmode.LeakedRegistrationObjectsViolation ||
        isLeakedSqlLite(v) ->
            ViolationClassification(ViolationCategory.LEAKED_RESOURCE, ViolationSeverity.HIGH)

        v is android.os.strictmode.ResourceMismatchViolation ->
            ViolationClassification(ViolationCategory.RESOURCE_MISMATCH, ViolationSeverity.MEDIUM)

        v is android.os.strictmode.CleartextNetworkViolation ->
            ViolationClassification(ViolationCategory.CLEARTEXT_NETWORK, ViolationSeverity.CRITICAL)

        v is android.os.strictmode.UntaggedSocketViolation ->
            ViolationClassification(ViolationCategory.UNTAGGED_SOCKET, ViolationSeverity.LOW)

        sdkVersion >= Build.VERSION_CODES.R && isCustomViolation(v) ->
            ViolationClassification(ViolationCategory.CUSTOM, ViolationSeverity.MEDIUM)

        else -> ViolationClassification(ViolationCategory.UNKNOWN, ViolationSeverity.LOW)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun isLeakedSqlLite(v: Throwable): Boolean =
        v.javaClass.simpleName == "LeakedSqlLiteObjectsViolation"

    @RequiresApi(Build.VERSION_CODES.R)
    private fun isCustomViolation(v: Throwable): Boolean =
        v is android.os.strictmode.CustomViolation

    /**
     * Fallback para API 24–27 via string-matching no simpleName.
     *
     * `internal` e `@VisibleForTesting` para permitir testes unitários
     * puros sem dependência do framework Android.
     */
    @VisibleForTesting
    internal fun classifyByClassName(simpleName: String): ViolationClassification = when {
        simpleName.contains("DiskRead", ignoreCase = true) ->
            ViolationClassification(ViolationCategory.DISK_READ, ViolationSeverity.MEDIUM)

        simpleName.contains("DiskWrite", ignoreCase = true) ->
            ViolationClassification(ViolationCategory.DISK_WRITE, ViolationSeverity.MEDIUM)

        simpleName.contains("Network", ignoreCase = true) ->
            ViolationClassification(ViolationCategory.NETWORK, ViolationSeverity.HIGH)

        simpleName.contains("SlowCall", ignoreCase = true) ->
            ViolationClassification(ViolationCategory.SLOW_CALL, ViolationSeverity.MEDIUM)

        simpleName.contains("Leaked", ignoreCase = true) ||
        simpleName.contains("SqlLite", ignoreCase = true) ->
            ViolationClassification(ViolationCategory.LEAKED_RESOURCE, ViolationSeverity.HIGH)

        simpleName.contains("Cleartext", ignoreCase = true) ->
            ViolationClassification(ViolationCategory.CLEARTEXT_NETWORK, ViolationSeverity.CRITICAL)

        simpleName.contains("ResourceMismatch", ignoreCase = true) ->
            ViolationClassification(ViolationCategory.RESOURCE_MISMATCH, ViolationSeverity.MEDIUM)

        simpleName.contains("UntaggedSocket", ignoreCase = true) ->
            ViolationClassification(ViolationCategory.UNTAGGED_SOCKET, ViolationSeverity.LOW)

        simpleName.contains("Custom", ignoreCase = true) ->
            ViolationClassification(ViolationCategory.CUSTOM, ViolationSeverity.MEDIUM)

        else ->
            ViolationClassification(ViolationCategory.UNKNOWN, ViolationSeverity.LOW)
    }
}
