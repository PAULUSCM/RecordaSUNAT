package com.example.recordasunat

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifier {
    const val CANAL_ALERTA = "alerta_diaria"
    const val CANAL_FIJO = "recordatorio_permanente"

    fun crearCanales(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL_ALERTA, "Avisos con sonido", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Aviso diario cuando se acerca un vencimiento"
            })
        nm.createNotificationChannel(
            NotificationChannel(CANAL_FIJO, "Recordatorio permanente", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Aviso fijo hasta que marques la declaración como hecha"
            })
    }

    fun permisoConcedido(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun idFijo(id: Long) = ((id % 500_000) * 2).toInt()
    fun idAlerta(id: Long) = ((id % 500_000) * 2 + 1).toInt()

    private fun base(context: Context, canal: String, idObligacion: Long): NotificationCompat.Builder {
        val abrirApp = PendingIntent.getActivity(
            context, (idObligacion % 500_000).toInt() + 1,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val declarar = PendingIntent.getBroadcast(
            context, (idObligacion % 500_000).toInt() + 1,
            Intent(context, DeclaredReceiver::class.java).putExtra("id", idObligacion),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, canal)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentIntent(abrirApp)
            .addAction(0, "YA DECLARÉ ✓", declarar)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
    }

    /** Aviso con sonido: 1 vez por día mientras esté pendiente */
    fun alertaDiaria(context: Context, idObligacion: Long, titulo: String, texto: String) {
        if (!permisoConcedido(context)) return
        NotificationManagerCompat.from(context).notify(
            idAlerta(idObligacion),
            base(context, CANAL_ALERTA, idObligacion)
                .setContentTitle(titulo).setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build())
    }

    /** Notificación FIJA en la barra hasta marcar "YA DECLARÉ" */
    fun fija(context: Context, idObligacion: Long, titulo: String, texto: String) {
        if (!permisoConcedido(context)) return
        NotificationManagerCompat.from(context).notify(
            idFijo(idObligacion),
            base(context, CANAL_FIJO, idObligacion)
                .setContentTitle(titulo).setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setOngoing(true)        // no se puede deslizar para quitar
                .setOnlyAlertOnce(true)  // no suena en cada actualización
                .build())
    }

    fun cancelar(context: Context, idObligacion: Long) {
        val nm = NotificationManagerCompat.from(context)
        nm.cancel(idFijo(idObligacion))
        nm.cancel(idAlerta(idObligacion))
    }
}