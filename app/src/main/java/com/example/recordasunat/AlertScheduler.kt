package com.example.recordasunat

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object AlertScheduler {

    fun programarTodo(context: Context) {
        programarSiguiente(context, "SUNAT")
        programarSiguiente(context, "GENERAL")
    }

    fun programarSiguiente(context: Context, tipo: String) {
        val prefs = context.getSharedPreferences("alarma", Context.MODE_PRIVATE)
        val ajustes = Config.cargar(context)
        val intervalo = if (tipo == "GENERAL") ajustes.intervaloGeneral else ajustes.intervaloSunat
        val clave = tipo.lowercase()
        val stamp = "${ajustes.inicio}|${ajustes.fin}|$intervalo"
        val ahoraMs = System.currentTimeMillis()
        val triggerPrevio = prefs.getLong("trigger_$clave", 0L)
        val stampPrevio = prefs.getString("stamp_$clave", null)

        // Si ya hay una alarma futura válida con la misma config, no reprogramar
        if (stampPrevio == stamp && triggerPrevio > ahoraMs + 60_000L) return

        val proximo = siguienteDisparo(ajustes.inicio, ajustes.fin, intervalo, LocalDateTime.now())
        val triggerAt = proximo.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val am = context.getSystemService(AlarmManager::class.java)
        val requestCode = if (tipo == "GENERAL") 1002 else 1001
        val pi = PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, AlertReceiver::class.java).putExtra("tipo", tipo),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val abrir = PendingIntent.getActivity(
            context, 1003,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // setAlarmClock: exacta siempre, no se difiere en Doze y no requiere permisos extra
        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, abrir), pi)

        prefs.edit().putLong("trigger_$clave", triggerAt).putString("stamp_$clave", stamp).apply()
    }

    /** Próximo momento dentro de la ventana horaria, respetando el intervalo */
    private fun siguienteDisparo(inicio: LocalTime, fin: LocalTime, intervaloMin: Int,
                                 ahora: LocalDateTime): LocalDateTime {
        val hoyIni = ahora.toLocalDate().atTime(inicio)
        val cruza = fin <= inicio
        fun enVentana(t: LocalDateTime): Boolean {
            val tt = t.toLocalTime()
            return if (!cruza) tt >= inicio && tt <= fin else tt >= inicio || tt <= fin
        }
        val cand = ahora.plusMinutes(intervaloMin.toLong())
        if (enVentana(cand)) return cand
        return if (cand < hoyIni) hoyIni else hoyIni.plusDays(1)
    }
}