package com.example.recordasunat

import java.time.LocalDate

object Planner {
    fun fmt(f: LocalDate): String = "%02d/%02d/%04d".format(f.dayOfMonth, f.monthValue, f.year)

    fun periodoDe(f: LocalDate): String = "%04d-%02d".format(f.year, f.monthValue)

    /** Fecha de vencimiento que se vigila HOY (null = no aplica) */
    fun vencimientoVigente(o: Obligacion, hoy: LocalDate = LocalDate.now()): LocalDate? {
        return if (o.tipo == "GENERAL") {
            if (o.mesAnual !in 1..12 || o.diaMes !in 1..31) return null
            if (o.repetirAnual) {
                val base = LocalDate.of(hoy.year, o.mesAnual, 1)
                base.withDayOfMonth(o.diaMes.coerceAtMost(base.lengthOfMonth()))
            } else {
                val anio = o.anioEspecifico ?: return null
                if (anio < 2000) return null
                val base = LocalDate.of(anio, o.mesAnual, 1)
                base.withDayOfMonth(o.diaMes.coerceAtMost(base.lengthOfMonth()))
            }
        } else {
            // SUNAT: la fila del cronograma es el PERIODO; el vencimiento cae el MES SIGUIENTE.
            // Ej: periodo 08 -> vence el día X de septiembre.
            val periodo = hoy.minusMonths(1)
            val dia = o.vencimientos[periodo.monthValue] ?: return null
            val base = LocalDate.of(periodo.year, periodo.monthValue, 1).plusMonths(1)
            base.withDayOfMonth(dia.coerceAtMost(base.lengthOfMonth()))
        }
    }

    /** Etiqueta de la ocurrencia vigilada. SUNAT: "2026-08" / GENERAL: "2026-07-21" */
    fun etiquetaVigente(o: Obligacion, hoy: LocalDate = LocalDate.now()): String? {
        return if (o.tipo == "GENERAL") vencimientoVigente(o, hoy)?.toString()
        else periodoDe(hoy.minusMonths(1))
    }

    sealed class Estado {
        object NoAplica : Estado()
        data class Atendido(val etiqueta: String) : Estado()
        data class PorVencer(val faltan: Long, val vencimiento: LocalDate) : Estado()
        object VenceHoy : Estado()
        data class Vencido(val diasAtraso: Long, val vencimiento: LocalDate) : Estado()
    }

    fun estadoDe(o: Obligacion, hoy: LocalDate = LocalDate.now()): Estado {
        val venc = vencimientoVigente(o, hoy) ?: return Estado.NoAplica
        val etiqueta = etiquetaVigente(o, hoy) ?: return Estado.NoAplica
        if (o.periodoDeclarado == etiqueta) return Estado.Atendido(etiqueta)
        return when {
            hoy.isAfter(venc) -> Estado.Vencido(hoy.toEpochDay() - venc.toEpochDay(), venc)
            hoy == venc -> Estado.VenceHoy
            else -> Estado.PorVencer(venc.toEpochDay() - hoy.toEpochDay(), venc)
        }
    }

    fun tocaAvisar(o: Obligacion, hoy: LocalDate = LocalDate.now()): Boolean {
        val venc = vencimientoVigente(o, hoy) ?: return false
        if (o.periodoDeclarado == etiquetaVigente(o, hoy)) return false
        return !hoy.isBefore(venc.minusDays(o.diasAviso.toLong()))
    }
}