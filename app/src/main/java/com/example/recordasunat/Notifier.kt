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
    const val CANAL_ALERTA = "alerta_sonora"
    const val CANAL_FIJO = "recordatorio_permanente"

    fun crearCanales(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL_ALERTA, "Alertas con sonido", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Suena según el horario configurado mientras haya pendientes"
            })
        nm.createNotificationChannel(
            NotificationChannel(CANAL_FIJO, "Recordatorio permanente", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Aviso fijo en la barra hasta que marques como hecho"
            })
    }

    fun permisoConcedido(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun idFijo(id: Long) = ((id % 500_000) * 2).toInt()
    fun idAlerta(id: Long) = ((id % 500_000) * 2 + 1).toInt()

    private fun base(context: Context, canal: String, idObligacion: Long, accion: String): NotificationCompat.Builder {
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
            .addAction(0, accion, declarar)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
    }

    fun alertaSonora(context: Context, idObligacion: Long, titulo: String, texto: String,
                     accion: String = "YA DECLARÉ ✓") {
        if (!permisoConcedido(context)) return
        NotificationManagerCompat.from(context).notify(
            idAlerta(idObligacion),
            base(context, CANAL_ALERTA, idObligacion, accion)
                .setContentTitle(titulo).setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .build())
    }

    fun fija(context: Context, idObligacion: Long, titulo: String, texto: String,
             accion: String = "YA DECLARÉ ✓") {
        if (!permisoConcedido(context)) return
        NotificationManagerCompat.from(context).notify(
            idFijo(idObligacion),
            base(context, CANAL_FIJO, idObligacion, accion)
                .setContentTitle(titulo).setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build())
    }

    fun cancelar(context: Context, idObligacion: Long) {
        val nm = NotificationManagerCompat.from(context)
        nm.cancel(idFijo(idObligacion))
        nm.cancel(idAlerta(idObligacion))
    }
}