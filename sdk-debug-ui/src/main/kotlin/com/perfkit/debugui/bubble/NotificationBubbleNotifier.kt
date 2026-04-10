package com.perfkit.debugui.bubble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.perfkit.api.domain.ViolationEvent
import com.perfkit.api.domain.ViolationSeverity
import com.perfkit.api.service.BubbleNotifier
import com.perfkit.debugui.panel.ViolationPanelActivity
import java.util.concurrent.atomic.AtomicInteger

/**
 * Implementação de [BubbleNotifier] baseada em notificação persistente.
 *
 * Usa uma notificação sticky (ongoing) que:
 * - Não emite som/vibração ([NotificationCompat.PRIORITY_LOW])
 * - Atualiza o contador de violações em tempo real
 * - Abre [ViolationPanelActivity] ao tocar
 *
 * Design decision: Notificação em vez de overlay de janela ([Settings.canDrawOverlays])
 * porque não requer permissão especial e funciona em todos os API levels suportados (24+).
 */
internal class NotificationBubbleNotifier(
    private val context: Context,
) : BubbleNotifier {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val counter = AtomicInteger(0)

    init {
        createNotificationChannel()
    }

    override fun show(event: ViolationEvent) {
        val count = counter.incrementAndGet()
        notificationManager.notify(NOTIFICATION_ID, buildNotification(count, event))
    }

    override fun dismiss() {
        counter.set(0)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun updateCounter(count: Int) {
        counter.set(count)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(count, latestEvent = null))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PerfKit — Debug Violations",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Monitoramento de violações StrictMode em tempo real"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(count: Int, latestEvent: ViolationEvent?): Notification {
        val openPanelIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, ViolationPanelActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = "PerfKit: $count violation${if (count == 1) "" else "s"} detectada${if (count == 1) "" else "s"}"
        val body = latestEvent?.let {
            val icon = severityIcon(it.severity)
            "$icon ${it.category.name}: ${it.message.take(80)}"
        } ?: "Toque para inspecionar"

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(openPanelIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setNumber(count)
            .build()
    }

    private fun severityIcon(severity: ViolationSeverity): String = when (severity) {
        ViolationSeverity.LOW -> "🔵"
        ViolationSeverity.MEDIUM -> "🟡"
        ViolationSeverity.HIGH -> "🟠"
        ViolationSeverity.CRITICAL -> "🔴"
    }

    companion object {
        private const val CHANNEL_ID = "perfkit_violations"
        private const val NOTIFICATION_ID = 0x5F4B4954 // "PKIT"
    }
}
