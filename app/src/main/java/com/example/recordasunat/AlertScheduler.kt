package com.example.recordasunat

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Cadena de alarmas exactas, UNA POR TIPO (SUNAT y GENERAL), porque cada
 * tipo tiene su propio horario e intervalo.
 *
 * Arreglo del bug de la v1.2 (con intervalos de 1 hora o más no repetía):
 *  - cada tipo lleva su propio disparo guardado (trigger_SUNAT / trigger_GENERAL);
 *  - si un disparo se pierde (el celular mató la app, Doze, lo que sea),
 *    el worker de respaldo llama a revisar() cada 15 minutos: suena y reprograma;
 *  - todo el cuerpo va en try/catch para que NUNCA bote la app al abrir.
 */
object AlertScheduler {
    private const val REQ_SUNAT = 1001
    private const val REQ_GENERAL = 1002

    fun programarSiguiente(context: Context, tipo: String) {
        try {
            val app = context.applicationContext
            val prefs = app.getSharedPreferences("alarma", Context.MODE_PRIVATE)
            val a = Config.ajustes(app, tipo)
            val stamp = Config.stamp(a)

            val stampPrevio = prefs.getString("stamp_$tipo", null)
            val triggerPrevio = prefs.getLong("trigger_$tipo", 0L)
            val ahoraMs = System.currentTimeMillis()
            // Si la configuración no cambió y el disparo guardado sigue vigente, no tocar.
            if (stampPrevio == stamp && triggerPrevio > ahoraMs + 60_000L) return

            val proximo = siguienteDisparo(a, LocalDateTime.now())
            val proximoMs = proximo.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                app,
                codigoDe(tipo),
                Intent(app, AlertReceiver::class.java).putExtra("tipo", tipo),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setWindow(AlarmManager.RTC_WAKEUP, proximoMs, 10 * 60_000L, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, proximoMs, pi)
            }
            prefs.edit().putLong("trigger_$tipo", proximoMs).putString("stamp_$tipo", stamp).apply()
        } catch (_: Exception) { }
    }

    /**
     * Respaldo: si el disparo guardado quedó en el pasado (se perdió),
     * suena de una vez y reprograma. Lo llama el worker cada 15 minutos.
     */
    fun revisar(context: Context, tipo: String) {
        try {
            val app = context.applicationContext
            val prefs = app.getSharedPreferences("alarma", Context.MODE_PRIVATE)
            val trigger = prefs.getLong("trigger_$tipo", 0L)
            val ahora = System.currentTimeMillis()
            if (trigger > 0 && trigger <= ahora - 2 * 60_000L) {
                if (dentroDeVentana(app, tipo)) AlertReceiver.alertarTipo(app, tipo)
                programarSiguiente(app, tipo)
            }
        } catch (_: Exception) { }
    }

    fun dentroDeVentana(context: Context, tipo: String): Boolean = try {
        val a = Config.ajustes(context, tipo)
        estaEnVentana(LocalDateTime.now(), a)
    } catch (_: Exception) { true }

    private fun codigoDe(tipo: String) = if (tipo == Config.GENERAL) REQ_GENERAL else REQ_SUNAT

    private fun siguienteDisparo(a: Config.Ajustes, ahora: LocalDateTime): LocalDateTime {
        val cand = ahora.plusMinutes(a.intervaloMin.toLong())
        if (estaEnVentana(cand, a)) return cand
        // Si el candidato cae fuera del horario, va al próximo arranque de ventana.
        val hoyIni = LocalDateTime.of(ahora.toLocalDate(), a.inicio)
        return if (ahora.toLocalTime() < a.inicio) hoyIni else hoyIni.plusDays(1)
    }

    private fun estaEnVentana(t: LocalDateTime, a: Config.Ajustes): Boolean {
        val tm = t.toLocalTime()
        return if (a.inicio <= a.fin) tm >= a.inicio && tm <= a.fin
        else tm >= a.inicio || tm <= a.fin   // ventana que cruza medianoche
    }
}