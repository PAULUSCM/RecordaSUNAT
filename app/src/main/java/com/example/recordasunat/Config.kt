package com.example.recordasunat

import android.content.Context
import java.time.LocalTime

/**
 * Ajustes de alarma POR TIPO: SUNAT y GENERAL tienen cada uno su propio
 * horario (inicio - fin) y su propio intervalo de repicado en minutos.
 *
 * Todo blindado: si lo guardado está dañado, vacío o es de una versión
 * vieja, se usan valores por defecto SIN explotar.
 */
object Config {
    data class Ajustes(val inicio: LocalTime, val fin: LocalTime, val intervaloMin: Int)

    private const val PREFS = "config_alertas"
    const val SUNAT = "SUNAT"
    const val GENERAL = "GENERAL"

    fun ajustes(context: Context, tipo: String): Ajustes {
        val def = Ajustes(LocalTime.of(8, 0), LocalTime.of(22, 0), 120)
        return try {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            // Lee el valor nuevo (ej: SUNAT_inicio); si no existe, migra el
            // viejo de la v1.2 (inicio / fin / intervalo sin prefijo).
            fun leido(campo: String): String? =
                (p.all["${tipo}_$campo"] ?: p.all[campo])?.toString()
            val ini = horaDe(leido("inicio"), def.inicio)
            val fin = horaDe(leido("fin"), def.fin)
            val itv = (leido("intervalo")?.trim()?.toIntOrNull() ?: 120).coerceIn(15, 1440)
            Ajustes(ini, fin, itv)
        } catch (_: Exception) { def }
    }

    fun guardar(context: Context, tipo: String, a: Ajustes) {
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("${tipo}_inicio", a.inicio.toString())
                .putString("${tipo}_fin", a.fin.toString())
                .putInt("${tipo}_intervalo", a.intervaloMin)
                .apply()
        } catch (_: Exception) { }
    }

    // Firma de la configuración: sirve para saber si cambió sin parsear nada.
    fun stamp(a: Ajustes): String = "${a.inicio}|${a.fin}|${a.intervaloMin}"

    // Convierte "8", "8:5", "08:05" a hora. Jamás lanza excepción.
    fun horaDe(s: String?, def: LocalTime): LocalTime = try {
        when {
            s == null || s.isBlank() -> def
            s.contains(":") -> {
                val partes = s.trim().split(":")
                val h = partes[0].trim().toIntOrNull()?.coerceIn(0, 23) ?: def.hour
                val m = partes.getOrNull(1)?.trim()?.toIntOrNull()?.coerceIn(0, 59) ?: 0
                LocalTime.of(h, m)
            }
            else -> {
                val h = s.trim().toIntOrNull()?.coerceIn(0, 23) ?: def.hour
                LocalTime.of(h, 0)
            }
        }
    } catch (_: Exception) { def }
}