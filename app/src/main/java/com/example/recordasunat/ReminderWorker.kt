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

        Store.cargar(context).forEach { o ->
            if (!Planner.tocaAvisar(o, hoy)) {
                Notifier.cancelar(context, o.id)
                return@forEach
            }
            val (titulo, texto) = Mensajes.de(o, hoy)
            Notifier.fija(context, o.id, titulo, texto, Mensajes.accion(o))
        }

        // Respaldo: garantiza que la alarma exacta siempre esté programada
        AlertScheduler.programarSiguiente(context)
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