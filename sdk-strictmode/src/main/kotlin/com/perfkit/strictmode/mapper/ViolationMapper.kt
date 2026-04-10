package com.perfkit.strictmode.mapper

import android.os.Build
import androidx.annotation.RequiresApi
import com.perfkit.api.domain.RawViolation
import com.perfkit.api.domain.ViolationSource

/**
 * Converte o [Throwable] emitido pelo [android.os.StrictMode.OnThreadViolationListener] /
 * [android.os.StrictMode.OnVmViolationListener] em um [RawViolation] de domínio.
 *
 * A conversão é mínima e deliberada — enriquecimento (classificação, dedup, stacktrace
 * filtering) acontece no sdk-core, mantendo este módulo focado na fronteira de plataforma.
 */
@RequiresApi(Build.VERSION_CODES.P)
internal object ViolationMapper {

    fun fromThreadViolation(violation: Throwable): RawViolation = RawViolation(
        source = ViolationSource.THREAD_POLICY,
        violation = violation,
        threadName = Thread.currentThread().name,
        timestamp = System.currentTimeMillis(),
    )

    fun fromVmViolation(violation: Throwable): RawViolation = RawViolation(
        source = ViolationSource.VM_POLICY,
        violation = violation,
        // VM violations são capturadas em threads internas do listener;
        // o nome da thread aqui é o do executor, não da thread que causou a violação.
        // O stacktrace do Throwable contém a callsite original.
        threadName = Thread.currentThread().name,
        timestamp = System.currentTimeMillis(),
    )
}
