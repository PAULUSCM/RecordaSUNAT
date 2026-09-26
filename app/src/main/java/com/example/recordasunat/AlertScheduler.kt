package com.example.recordasunat

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

object AlertScheduler {
    private const val REQUEST = 1001

    fun programarSiguiente(context: Context) {
        val prefs = context.getSharedPreferences("alarma", Context.MODE_PRIVATE)
        val ajustes = Config.cargar(context)
        val stamp = "${ajustes.inicio}|${ajustes.fin}|${ajustes.intervaloMin}"
        val ahoraMs = System.currentTimeMillis()
        val triggerPrevio = prefs.getLong("trigger", 0L)
        val stampPrevio = prefs.getString("stamp", null)

        // Si ya hay una alarma futura válida con la misma configuración, no tocar
        // (evita que las revisiones periódicas retrasen la alarma indefinidamente)
        if (stampPrevio == stamp && triggerPrevio > ahoraMs + 60_000L) return

        val proximo = siguienteDisparo(ajustes, LocalDateTime.now())
        val triggerAt = proximo.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, REQUEST,
            Intent(context, AlertReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val exacta = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exacta) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        else am.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 10 * 60_000L, pi)

        prefs.edit().putLong("trigger", triggerAt).putString("stamp", stamp).apply()
    }

    /** Próximo momento dentro de la ventana horaria, respetando el intervalo */
    private fun siguienteDisparo(a: Config.Ajustes, ahora: LocalDateTime): LocalDateTime {
        val hoyIni = ahora.toLocalDate().atTime(a.inicio)
        val cruza = a.fin <= a.inicio
        fun enVentana(t: LocalDateTime): Boolean {
            val tt = t.toLocalTime()
            return if (!cruza) tt >= a.inicio && tt <= a.fin else tt >= a.inicio || tt <= a.fin
        }
        val cand = ahora.plusMinutes(a.intervaloMin.toLong())
        if (enVentana(cand)) return cand
        return if (cand < hoyIni) hoyIni else hoyIni.plusDays(1)
    }
}