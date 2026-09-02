package com.example.recordasunat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DeclaredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1L)
        if (id <= 0) return
        val lista = Store.cargar(context)
        val i = lista.indexOfFirst { it.id == id }
        if (i < 0) return
        lista[i] = lista[i].copy(periodoDeclarado = Planner.periodoActual())
        Store.guardar(context, lista)
        Notifier.cancelar(context, id)
    }
}