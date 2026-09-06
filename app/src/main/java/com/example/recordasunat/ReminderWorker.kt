package com.example.recordasunat

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val hoy = LocalDate.now()
        val prefs = context.getSharedPreferences("estado", Context.MODE_PRIVATE)
        val ajustes = Config.cargar(context)
        val ahora = LocalTime.now()
        val enVentana = if (ajustes.inicio <= ajustes.fin)
            ahora >= ajustes.inicio && ahora <= ajustes.fin
        else
            ahora >= ajustes.inicio || ahora <= ajustes.fin   // ventana que cruza medianoche
        val ahoraMs = System.currentTimeMillis()
        val intervaloMs = ajustes.intervaloMin * 60_000L

        Store.cargar(context).forEach { o ->
            if (!Planner.tocaAvisar(o, hoy)) {
                Notifier.cancelar(context, o.id)
                return@forEach
            }
            val detalle = if (o.detalle.isBlank()) "" else "\n${o.detalle}"
            val esGeneral = o.tipo == "GENERAL"
            val accion = if (esGeneral) "YA LO HICE ✓" else "YA DECLARÉ ✓"

            val (titulo, texto) = when (val est = Planner.estadoDe(o, hoy)) {
                is Planner.Estado.Vencido ->
                    if (esGeneral)
                        "🔴 ${o.nombre}: ¡la fecha ya pasó!" to
                        "Era el ${Planner.fmt(est.vencimiento)}. Toca «YA LO HICE» cuando lo atiendas.$detalle"
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

            // Notificación fija (silenciosa, permanente)
            Notifier.fija(context, o.id, titulo, texto, accion)

            // Alerta sonora: dentro del horario y respetando el intervalo
            if (enVentana) {
                val clave = "ultimaAlerta_${o.id}"
                if (ahoraMs - prefs.getLong(clave, 0L) >= intervaloMs) {
                    Notifier.alertaSonora(context, o.id, titulo, texto, accion)
                    prefs.edit().putLong(clave, ahoraMs).apply()
                }
            }
        }
        return Result.success()
    }

    companion object {
        fun programar(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "recordatorios_sunat",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build())
        }
        fun ejecutarAhora(context: Context) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<ReminderWorker>().build())
        }
    }
}