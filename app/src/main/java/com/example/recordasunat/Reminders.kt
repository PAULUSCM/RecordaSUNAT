package com.example.recordasunat

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/* ============================================================
   MOTOR DE ALARMAS Y NOTIFICACIONES - v4.0
   - Aviso permanente desde días antes (5 en MENSUAL/ANUAL/GENERAL,
     7 en ANIVERSARIOS)
   - Alarma que suena repetidamente (cada 10 min; después de las
     22:00 pasa a las 08:00 del día siguiente) hasta confirmar
   - Botón de confirmación dinámico: YA DECLARÉ / YA PAGUÉ / YA LO HICE
   ============================================================ */

object MotorAlarmas {

    const val CANAL_AVISO = "inges_aviso"
    const val CANAL_ALARMA = "inges_alarma"
    const val ACCION_ALARMA = "com.example.recordasunat.ALARMA"
    const val ACCION_CONFIRMAR = "com.example.recordasunat.CONFIRMAR"
    const val EXTRA_ID = "id_item"

    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val canalAviso = NotificationChannel(
                CANAL_AVISO, "Avisos de vencimiento",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Aviso fijo mientras se acerca un vencimiento"
            }
            val uriAlarma: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val atributos = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val canalAlarma = NotificationChannel(
                CANAL_ALARMA, "Alarma de vencimiento",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Suena repetidamente hasta que confirmas"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
                setSound(uriAlarma, atributos)
            }
            nm.createNotificationChannel(canalAviso)
            nm.createNotificationChannel(canalAlarma)
        } catch (e: Exception) {
        }
    }

    fun idNotificacion(id: String): Int = id.hashCode()

    fun abrirApp(context: Context): PendingIntent {
        val intento = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 900001, intento,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun pendingAlarma(context: Context, id: String): PendingIntent {
        val intento = Intent(context, ReceptorAlarma::class.java).apply {
            action = ACCION_ALARMA
            putExtra(EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context, id.hashCode(), intento,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun pendingConfirmar(context: Context, id: String): PendingIntent {
        val intento = Intent(context, ReceptorAlarma::class.java).apply {
            action = ACCION_CONFIRMAR
            putExtra(EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context, id.hashCode() + 500000, intento,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun notificacionPermanente(context: Context, item: ItemRecordatorio, titulo: String, texto: String) {
        crearCanales(context)
        val extra = if (item.nota.isNotBlank()) "\n" + item.nota else ""
        val notif = NotificationCompat.Builder(context, CANAL_AVISO)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto + extra))
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(abrirApp(context))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(idNotificacion(item.id), notif)
        } catch (e: Exception) {
        }
    }

    fun notificacionAlarma(context: Context, item: ItemRecordatorio, titulo: String, texto: String) {
        crearCanales(context)
        val extra = if (item.nota.isNotBlank()) "\n" + item.nota else ""
        val notif = NotificationCompat.Builder(context, CANAL_ALARMA)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto + extra))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(abrirApp(context))
            .addAction(0, DatosApp.textoBotonConfirmar(item), pendingConfirmar(context, item.id))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(idNotificacion(item.id), notif)
        } catch (e: Exception) {
        }
    }

    fun limpiarNotificacion(context: Context, id: String) {
        try {
            NotificationManagerCompat.from(context).cancel(idNotificacion(id))
        } catch (e: Exception) {
        }
    }

    fun notificacionPrueba(context: Context) {
        crearCanales(context)
        val notif = NotificationCompat.Builder(context, CANAL_ALARMA)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("PRUEBA DE NOTIFICACIÓN")
            .setContentText("Si lees esto, las notificaciones están activadas.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(999999, notif)
        } catch (e: Exception) {
        }
    }

    fun fijarAlarma(context: Context, item: ItemRecordatorio, cuando: LocalDateTime) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = pendingAlarma(context, item.id)
            val millis = cuando.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val exacto = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.canScheduleExactAlarms()
            } else true
            if (exacto) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            }
        } catch (e: Exception) {
        }
    }

    fun cancelarAlarma(context: Context, id: String) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pendingAlarma(context, id))
        } catch (e: Exception) {
        }
    }

    fun programar(context: Context, item: ItemRecordatorio) {
        val ahora = LocalDateTime.now()
        val venc = DatosApp.vencimientoDe(item, ahora) ?: return

        if (item.grupo == Grupo.GENERAL.name) {
            if (DatosApp.yaConfirmado(item, venc)) {
                limpiarNotificacion(context, item.id)
                cancelarAlarma(context, item.id)
                return
            }
            if (venc.isAfter(ahora)) {
                val dias = Duration.between(ahora, venc).toDays()
                if (dias < DatosApp.diasAviso(item)) {
                    notificacionPermanente(
                        context, item, "AVISO: ${item.titulo}",
                        DatosApp.textoAviso(item, venc, ahora)
                    )
                }
                fijarAlarma(context, item, venc)
            } else {
                notificacionPermanente(
                    context, item, "VENCIDO: ${item.titulo}",
                    DatosApp.descripcionVencimiento(item)
                )
                fijarAlarma(context, item, ahora.plusMinutes(1))
            }
            return
        }

        // MENSUAL, ANIVERSARIOS, ANUAL
        if (DatosApp.yaConfirmado(item, venc)) {
            val proximo = DatosApp.avanzarCiclo(item, venc)
            fijarAlarma(context, item, proximo)
            limpiarNotificacion(context, item.id)
            return
        }
        if (venc.isAfter(ahora)) {
            val dias = Duration.between(ahora, venc).toDays()
            if (dias < DatosApp.diasAviso(item)) {
                notificacionPermanente(
                    context, item, "AVISO: ${item.titulo}",
                    DatosApp.textoAviso(item, venc, ahora)
                )
            }
            fijarAlarma(context, item, venc)
        } else {
            notificacionPermanente(
                context, item, "VENCIDO: ${item.titulo}",
                DatosApp.descripcionVencimiento(item)
            )
            fijarAlarma(context, item, ahora.plusMinutes(1))
        }
    }

    fun programarTodo(context: Context) {
        val lista = DatosApp.cargar(context)
        for (item in lista) {
            if (!item.activo) continue
            try {
                programar(context, item)
            } catch (e: Exception) {
            }
        }
    }

    fun programarTrabajo(context: Context) {
        try {
            val peticion =
                PeriodicWorkRequestBuilder<TrabajadorRecordatorios>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "chequeo_inges", ExistingPeriodicWorkPolicy.KEEP, peticion
            )
        } catch (e: Exception) {
        }
    }

    fun arrancar(context: Context) {
        crearCanales(context)
        programarTrabajo(context)
        programarTodo(context)
    }

    fun confirmar(context: Context, id: String) {
        try {
            val lista = DatosApp.cargar(context)
            val item = lista.firstOrNull { it.id == id } ?: return
            val venc = DatosApp.vencimientoDe(item) ?: return
            val ciclo = DatosApp.cicloDe(venc, item.grupo)
            val nuevaLista = lista.map {
                if (it.id == id) it.copy(cicloConfirmado = ciclo) else it
            }
            DatosApp.guardar(context, nuevaLista)
            limpiarNotificacion(context, id)
            cancelarAlarma(context, id)
            arrancar(context)
        } catch (e: Exception) {
        }
    }
}

/* -------- receptor de alarmas y del botón YA LO HICE -------- */

class ReceptorAlarma : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val id = intent.getStringExtra(MotorAlarmas.EXTRA_ID) ?: return
            MotorAlarmas.crearCanales(context)
            if (intent.action == MotorAlarmas.ACCION_CONFIRMAR) {
                MotorAlarmas.confirmar(context, id)
                return
            }
            val lista = DatosApp.cargar(context)
            val item = lista.firstOrNull { it.id == id && it.activo } ?: return
            val ahora = LocalDateTime.now()
            val venc = DatosApp.vencimientoDe(item, ahora) ?: return
            if (DatosApp.yaConfirmado(item, venc)) {
                MotorAlarmas.limpiarNotificacion(context, id)
                return
            }
            MotorAlarmas.notificacionAlarma(
                context, item,
                "${DatosApp.textoAlarma(item, venc, ahora)}: ${item.titulo}",
                DatosApp.descripcionVencimiento(item)
            )
            // Repetición: cada 10 minutos; tras las 22:00 pasa a las 08:00 del día siguiente
            var proxima = ahora.plusMinutes(10)
            if (proxima.hour >= 22) {
                proxima = proxima.plusDays(1).with(LocalTime.of(8, 0))
            }
            MotorAlarmas.fijarAlarma(context, item, proxima)
        } catch (e: Exception) {
        }
    }
}

/* -------- receptor cuando se enciende el teléfono -------- */

class ReceptorArranque : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                MotorAlarmas.arrancar(context)
            } catch (e: Exception) {
            }
        }
    }
}

/* -------- chequeo periódico cada 15 minutos -------- */

class TrabajadorRecordatorios(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            MotorAlarmas.arrancar(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.success()
        }
    }
}