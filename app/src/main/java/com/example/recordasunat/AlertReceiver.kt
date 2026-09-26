package com.example.recordasunat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalTime

class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ajustes = Config.cargar(context)
        val ahora = LocalTime.now()
        val enVentana = if (ajustes.inicio <= ajustes.fin)
            ahora >= ajustes.inicio && ahora <= ajustes.fin
        else
            ahora >= ajustes.inicio || ahora <= ajustes.fin

        if (enVentana) {
            val hoy = LocalDate.now()
            Store.cargar(context)
                .filter { Planner.tocaAvisar(it, hoy) }
                .forEach { o ->
                    val (titulo, texto) = Mensajes.de(o, hoy)
                    Notifier.alertaSonora(context, o.id, titulo, texto, Mensajes.accion(o))
                }
        }
        // Reprograma la siguiente alarma para mantener la cadena
        AlertScheduler.programarSiguiente(context)
    }
}