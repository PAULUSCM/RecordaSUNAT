package com.example.recordasunat

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Respaldo cada 15 minutos: refresca la notificación permanente de cada
 * recordatorio pendiente y recupera las cadenas de alarma si un disparo
 * se perdió (esto es lo que arregla los intervalos largos, de 1 hora o más).
 */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        try {
            val context = applicationContext
            val hoy = LocalDate.now()
            val lista = Store.cargar(context)
            for (o in lista) {
                try {
                    if (!Planner.tocaAvisar(o, hoy)) {
                        Notifier.cancelar(context, o.id)
                    } else {
                        val (titulo, texto) = Mensajes.de(o, hoy)
                        Notifier.fija(context, o.id, titulo, texto, Mensajes.accion(o))
                    }
                } catch (_: Exception) { }
            }
            // Rescate de las dos cadenas de alarma (SUNAT y GENERAL).
            AlertScheduler.revisar(context, Config.SUNAT)
            AlertScheduler.revisar(context, Config.GENERAL)
        } catch (_: Exception) { }
        return Result.success()
    }

    companion object {
        fun programar(context: Context) {
            try {
                val pedido = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "recordatorios_sunat", ExistingPeriodicWorkPolicy.KEEP, pedido
                )
            } catch (_: Exception) { }
        }

        fun ejecutarAhora(context: Context) {
            try {
                val pedido = OneTimeWorkRequestBuilder<ReminderWorker>().build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "recordatorios_ahora", ExistingWorkPolicy.REPLACE, pedido
                )
            } catch (_: Exception) { }
        }
    }
}