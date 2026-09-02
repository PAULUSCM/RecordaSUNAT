package com.example.recordasunat

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val hoy = LocalDate.now()
        val prefs = context.getSharedPreferences("estado", Context.MODE_PRIVATE)

        Store.cargar(context).forEach { o ->
            if (!Planner.tocaAvisar(o, hoy)) {
                Notifier.cancelar(context, o.id)
                return@forEach
            }
            val detalle = if (o.detalle.isBlank()) "" else "\n${o.detalle}"

            val (titulo, texto) = when (val est = Planner.estadoDe(o, hoy)) {
                is Planner.Estado.Vencido ->
                    "🔴 ${o.nombre}: ¡VENCIDO hace ${est.diasAtraso} día(s)!" to
                    "Venció el ${Planner.fmt(est.vencimiento)}. Declara YA y toca «YA DECLARÉ».$detalle"
                Planner.Estado.VenceHoy ->
                    "🔴 ${o.nombre}: ¡VENCE HOY!" to
                    "Hoy es el último día para declarar.$detalle"
                is Planner.Estado.PorVencer ->
                    "⏰ ${o.nombre}: vence en ${est.faltan} día(s)" to
                    "Vencimiento: ${Planner.fmt(est.vencimiento)}.$detalle"
                else -> o.nombre to "Vencimiento pendiente.$detalle"
            }

            Notifier.fija(context, o.id, titulo, texto)

            // Sonido de alerta: solo 1 vez por día
            val clave = "alerta_${o.id}"
            if (prefs.getString(clave, null) != hoy.toString()) {
                Notifier.alertaDiaria(context, o.id, titulo, texto)
                prefs.edit().putString(clave, hoy.toString()).apply()
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