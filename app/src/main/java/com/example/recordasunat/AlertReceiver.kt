package com.example.recordasunat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate

/**
 * Suena la alarma: avisa con notificación SONORA los recordatorios pendientes
 * del tipo que disparó (SUNAT o GENERAL, viene en el extra "tipo") y
 * reprograma su cadena. Todo blindado contra excepciones.
 */
class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tipo = intent.getStringExtra("tipo") ?: Config.SUNAT
        try {
            if (AlertScheduler.dentroDeVentana(context, tipo)) {
                alertarTipo(context.applicationContext, tipo)
            }
        } catch (_: Exception) { }
        try {
            AlertScheduler.programarSiguiente(context.applicationContext, tipo)
        } catch (_: Exception) { }
    }

    companion object {
        fun alertarTipo(context: Context, tipo: String) {
            try {
                if (!Notifier.permisoConcedido(context)) return
                val hoy = LocalDate.now()
                val lista = Store.cargar(context)
                for (o in lista) {
                    try {
                        if (o.tipo == tipo && Planner.tocaAvisar(o, hoy)) {
                            val (titulo, texto) = Mensajes.de(o, hoy)
                            Notifier.alertaSonora(context, o.id, titulo, texto, Mensajes.accion(o))
                        }
                    } catch (_: Exception) { }
                }
            } catch (_: Exception) { }
        }
    }
}