package com.example.recordasunat

import android.content.Context
import java.time.LocalTime

object Config {
    data class Ajustes(val inicio: LocalTime, val fin: LocalTime,
                       val intervaloSunat: Int, val intervaloGeneral: Int)

    private const val PREFS = "config_alertas"

    fun cargar(context: Context): Ajustes {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ini = parseHora(p.getString("inicio", "08:00") ?: "08:00", LocalTime.of(8, 0))
        val fin = parseHora(p.getString("fin", "22:00") ?: "22:00", LocalTime.of(22, 0))
        val interS = p.getInt("intervaloMin", 120).coerceIn(15, 1440)   // clave antigua = SUNAT
        val interG = p.getInt("intervaloMinGeneral", interS).coerceIn(15, 1440)
        return Ajustes(ini, fin, interS, interG)
    }

    fun guardar(context: Context, inicio: String, fin: String,
                intervaloSunat: Int, intervaloGeneral: Int) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ini = parseHora(inicio, LocalTime.of(8, 0))
        val fin2 = parseHora(fin, LocalTime.of(22, 0))
        p.edit()
            .putString("inicio", "%02d:%02d".format(ini.hour, ini.minute))
            .putString("fin", "%02d:%02d".format(fin2.hour, fin2.minute))
            .putInt("intervaloMin", intervaloSunat.coerceIn(15, 1440))
            .putInt("intervaloMinGeneral", intervaloGeneral.coerceIn(15, 1440))
            .apply()
    }

    private fun parseHora(texto: String, defecto: LocalTime): LocalTime {
        val partes = texto.trim().split(":")
        val h = partes.getOrNull(0)?.trim()?.toIntOrNull() ?: return defecto
        val m = partes.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        if (h !in 0..23 || m !in 0..59) return defecto
        return LocalTime.of(h, m)
    }
}