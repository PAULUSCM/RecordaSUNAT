package com.example.recordasunat

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Planner {
    private val FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    fun fmt(f: LocalDate) = f.format(FMT)

    fun periodoActual(hoy: LocalDate = LocalDate.now()): String =
        "%04d-%02d".format(hoy.year, hoy.monthValue)

    fun vencimientoDelMes(o: Obligacion, hoy: LocalDate = LocalDate.now()): LocalDate? {
        val dia = o.vencimientos[hoy.monthValue] ?: return null
        return LocalDate.of(hoy.year, hoy.monthValue, dia.coerceAtMost(hoy.lengthOfMonth()))
    }

    sealed class Estado {
        object NoAplica : Estado()
        data class Declarado(val periodo: String) : Estado()
        data class PorVencer(val faltan: Long, val vencimiento: LocalDate) : Estado()
        object VenceHoy : Estado()
        data class Vencido(val diasAtraso: Long, val vencimiento: LocalDate) : Estado()
    }

    fun estadoDe(o: Obligacion, hoy: LocalDate = LocalDate.now()): Estado {
        val venc = vencimientoDelMes(o, hoy) ?: return Estado.NoAplica
        if (o.periodoDeclarado == periodoActual(hoy)) return Estado.Declarado(o.periodoDeclarado!!)
        return when {
            hoy.isAfter(venc) -> Estado.Vencido(hoy.toEpochDay() - venc.toEpochDay(), venc)
            hoy == venc       -> Estado.VenceHoy
            else              -> Estado.PorVencer(venc.toEpochDay() - hoy.toEpochDay(), venc)
        }
    }

    /** Corresponde avisar si estamos dentro de los N días previos,
        el mismo día, o ya venció — y aún no se marcó como declarado. */
    fun tocaAvisar(o: Obligacion, hoy: LocalDate = LocalDate.now()): Boolean {
        val venc = vencimientoDelMes(o, hoy) ?: return false
        if (o.periodoDeclarado == periodoActual(hoy)) return false
        return !hoy.isBefore(venc.minusDays(o.diasAviso.toLong()))
    }
}