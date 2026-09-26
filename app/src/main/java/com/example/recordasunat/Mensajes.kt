package com.example.recordasunat

import java.time.LocalDate

object Mensajes {
    fun accion(o: Obligacion): String =
        if (o.tipo == "GENERAL") "YA LO HICE ✓" else "YA DECLARÉ ✓"

    fun de(o: Obligacion, hoy: LocalDate): Pair<String, String> {
        val detalle = if (o.detalle.isBlank()) "" else "\n${o.detalle}"
        val esGeneral = o.tipo == "GENERAL"
        return when (val est = Planner.estadoDe(o, hoy)) {
            is Planner.Estado.Vencido ->
                if (esGeneral)
                    "🔴 ${o.nombre}: ¡la fecha ya pasó!" to
                    "Era el ${Planner.fmt(est.vencimiento)}. Toca «YA LO HICE».$detalle"
                else
                    "🔴 ${o.nombre}: ¡VENCIDO hace ${est.diasAtraso} día(s)!" to
                    "Venció el ${Planner.fmt(est.vencimiento)}. Declara YA.$detalle"
            Planner.Estado.VenceHoy ->
                if (esGeneral) "🔴 ${o.nombre}: ¡ES HOY!" to "Hoy es el día.$detalle"
                else "🔴 ${o.nombre}: ¡VENCE HOY!" to "Hoy es el último día para declarar.$detalle"
            is Planner.Estado.PorVencer ->
                "⏰ ${o.nombre}: ${if (esGeneral) "falta(n)" else "vence en"} ${est.faltan} día(s)" to
                "Fecha: ${Planner.fmt(est.vencimiento)}.$detalle"
            else -> o.nombre to "Pendiente.$detalle"
        }
    }
}