package com.example.recordasunat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate

class DeclaredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1L)
        if (id <= 0) return
        val lista = Store.cargar(context)
        val i = lista.indexOfFirst { it.id == id }
        if (i < 0) return
        val etiqueta = Planner.etiquetaVigente(lista[i], LocalDate.now()) ?: return
        lista[i] = lista[i].copy(periodoDeclarado = etiqueta)
        Store.guardar(context, lista)
        Notifier.cancelar(context, id)
    }
}