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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/* ============================================================
   MOTOR DE ALARMAS Y NOTIFICACIONES - v4.4
   - CONFIGURACIÓN DE ALARMA POR PESTAÑA (ConfigAlarmas): una
     alarma distinta para MENSUAL, ANIVERSARIOS, ANUAL y GENERAL,
     cada una con: días antes, hora de inicio, hora de fin e
     intervalo en minutos. Por defecto: 5 días antes (7 en
     ANIVERSARIOS y 1 en GENERAL), de 08:00 a 20:00, cada 60 min.
   - La alarma suena SOLO en los días y en el horario configurados:
     desde N días antes hasta el día del vencimiento, entre la hora
     de inicio y la de fin, repitiendo cada X minutos hasta confirmar.
   - Lo ya VENCIDO no suena y no se queda en notificaciones: en la
     zona de notificaciones solo aparecen recordatorios vigentes.
   - Botón de confirmación dinámico: YA DECLARÉ / YA PAGUÉ / YA LO HICE
   ============================================================ */

/* -------- configuración de alarma por pestaña (4 tipos) -------- */

data class ConfigAlarma(
    val diasAntes: Int,
    val horaInicio: String,
    val horaFin: String,
    val intervaloMin: Int
)

object ConfigAlarmas {

    private const val PREFS = "config_alarmas_inges"

    fun porDefecto(grupo: String): ConfigAlarma = when (grupo.uppercase()) {
        Grupo.ANIVERSARIOS.name -> ConfigAlarma(7, "08:00", "20:00", 60)
        Grupo.GENERAL.name -> ConfigAlarma(1, "08:00", "20:00", 30)
        else -> ConfigAlarma(5, "08:00", "20:00", 60)
    }

    fun leer(context: Context, grupo: String): ConfigAlarma {
        val d = porDefecto(grupo)
        return try {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ConfigAlarma(
                p.getInt("dias_" + grupo, d.diasAntes),
                p.getString("inicio_" + grupo, d.horaInicio) ?: d.horaInicio,
                p.getString("fin_" + grupo, d.horaFin) ?: d.horaFin,
                p.getInt("intervalo_" + grupo, d.intervaloMin)
            )
        } catch (e: Exception) {
            d
        }
    }

    fun guardar(context: Context, grupo: String, c: ConfigAlarma) {
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt("dias_" + grupo, c.diasAntes)
                .putString("inicio_" + grupo, c.horaInicio)
                .putString("fin_" + grupo, c.horaFin)
                .putInt("intervalo_" + grupo, c.intervaloMin)
                .apply()
        } catch (e: Exception) {
        }
    }

    // Acepta "08:00", "8:00" o "8" (minuto 0). Devuelve null si es inválida.
    fun parseHora(texto: String): LocalTime? {
        return try {
            val p = texto.trim().split(":")
            val h = p.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
            val m = p.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            LocalTime.of(h, m)
        } catch (e: Exception) {
            null
        }
    }

    fun textoResumen(c: ConfigAlarma): String {
        val dias = if (c.diasAntes == 1) "1 día" else "${c.diasAntes} días"
        return "suena $dias antes, de ${c.horaInicio} a ${c.horaFin}, cada ${c.intervaloMin} min"
    }
}

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
                description = "Aviso fijo mientras el recordatorio está vigente"
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
                description = "Suena solo dentro del horario configurado, hasta que confirmas"
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

        if (!item.activo) {
            limpiarNotificacion(context, item.id)
            cancelarAlarma(context, item.id)
            return
        }

        val venc = DatosApp.vencimientoDe(item, ahora)
        if (venc == null) {
            // Sin fecha utilizable (GENERAL sin fecha, mes sin día configurado):
            // se apaga el aviso y no se toca ninguna alarma ya programada.
            limpiarNotificacion(context, item.id)
            return
        }

        val cfg = ConfigAlarmas.leer(context, item.grupo)
        val inicio = ConfigAlarmas.parseHora(cfg.horaInicio) ?: LocalTime.of(8, 0)
        val fin = ConfigAlarmas.parseHora(cfg.horaFin) ?: LocalTime.of(20, 0)
        val intervalo = cfg.intervaloMin.coerceAtLeast(1).toLong()

        if (DatosApp.yaConfirmado(item, venc)) {
            limpiarNotificacion(context, item.id)
            if (item.grupo == Grupo.GENERAL.name) {
                cancelarAlarma(context, item.id)
                return
            }
            // Ciclo confirmado: se programa ya el primer timbre del SIGUIENTE ciclo.
            val proximo = DatosApp.avanzarCiclo(item, venc)
            val diaProx = proximo.toLocalDate()
            val primerDiaProx = diaProx.minusDays(cfg.diasAntes.coerceAtLeast(0).toLong())
            val siguiente = proximoTimbre(ahora, primerDiaProx, diaProx, inicio, fin, intervalo)
            fijarAlarma(context, item, siguiente ?: proximo)
            return
        }

        val diaVenc = venc.toLocalDate()
        val primerDia = diaVenc.minusDays(cfg.diasAntes.coerceAtLeast(0).toLong())
        val hoy = ahora.toLocalDate()
        val enVentana = !hoy.isBefore(primerDia) && !hoy.isAfter(diaVenc)

        // En la zona de notificaciones SOLO aparecen los vigentes (dentro de
        // los días de aviso). Lo vencido se limpia y no suena.
        if (enVentana) {
            notificacionPermanente(
                context, item, "AVISO: ${item.titulo}",
                DatosApp.textoAviso(item, venc, ahora)
            )
        } else {
            limpiarNotificacion(context, item.id)
        }

        // Solo suena dentro de la ventana configurada (días y horario).
        val siguiente = proximoTimbre(ahora, primerDia, diaVenc, inicio, fin, intervalo)
        if (siguiente != null) {
            fijarAlarma(context, item, siguiente)
        } else {
            cancelarAlarma(context, item.id)
        }
    }

    // Primer momento >= ahora en que la alarma debe sonar: cada día entre
    // primerDia y ultimoDia (inclusive), entre la hora de inicio y la de fin,
    // alineado a los múltiplos del intervalo contados desde la hora de inicio.
    // Si ya se pasó todo (vencido o ventana terminada) devuelve null: no suena más.
    fun proximoTimbre(
        ahora: LocalDateTime,
        primerDia: LocalDate,
        ultimoDia: LocalDate,
        inicio: LocalTime,
        fin: LocalTime,
        intervaloMin: Long
    ): LocalDateTime? {
        var dia = if (ahora.toLocalDate().isBefore(primerDia)) primerDia else ahora.toLocalDate()
        while (!dia.isAfter(ultimoDia)) {
            val base = dia.atTime(inicio)
            var candidato = base
            if (dia == ahora.toLocalDate() && ahora.isAfter(base)) {
                val minutos = Duration.between(base, ahora).toMinutes()
                candidato = base.plusMinutes((minutos / intervaloMin + 1) * intervaloMin)
            }
            while (!candidato.toLocalTime().isAfter(fin)) {
                if (candidato.isAfter(ahora)) return candidato
                candidato = candidato.plusMinutes(intervaloMin)
            }
            dia = dia.plusDays(1)
        }
        return null
    }

    fun programarTodo(context: Context) {
        val lista = DatosApp.cargar(context)
        for (item in lista) {
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

            // Título correcto según el momento (ahora también suena días antes).
            val titulo = when {
                venc.toLocalDate() == ahora.toLocalDate() -> "VENCE HOY"
                venc.isAfter(ahora) -> "VENCE PRONTO"
                else -> "VENCIDO"
            }
            MotorAlarmas.notificacionAlarma(
                context, item,
                "$titulo: ${item.titulo}",
                DatosApp.descripcionVencimiento(item)
            )

            // Siguiente timbre DENTRO de la ventana configurada; si ya se pasó
            // todo (se acabó el día del vencimiento), no vuelve a sonar.
            val cfg = ConfigAlarmas.leer(context, item.grupo)
            val inicio = ConfigAlarmas.parseHora(cfg.horaInicio) ?: LocalTime.of(8, 0)
            val fin = ConfigAlarmas.parseHora(cfg.horaFin) ?: LocalTime.of(20, 0)
            val diaVenc = venc.toLocalDate()
            val primerDia = diaVenc.minusDays(cfg.diasAntes.coerceAtLeast(0).toLong())
            val siguiente = MotorAlarmas.proximoTimbre(
                ahora, primerDia, diaVenc, inicio, fin,
                cfg.intervaloMin.coerceAtLeast(1).toLong()
            )
            if (siguiente != null) {
                MotorAlarmas.fijarAlarma(context, item, siguiente)
            } else {
                MotorAlarmas.cancelarAlarma(context, item.id)
            }
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