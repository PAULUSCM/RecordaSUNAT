package com.example.recordasunat

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Obligacion(
    val id: Long = 0,
    val nombre: String = "",
    val detalle: String = "",
    val vencimientos: Map<Int, Int> = emptyMap(), // mes 1..12 -> día de vencimiento
    val diasAviso: Int = 5,
    val periodoDeclarado: String? = null          // ej: "2025-08"
)

object Store {
    private const val ARCHIVO = "obligaciones.json"
    private val gson = Gson()

    fun cargar(context: Context): MutableList<Obligacion> = try {
        val json = context.openFileInput(ARCHIVO).bufferedReader().use { it.readText() }
        val tipo = object : TypeToken<MutableList<Obligacion>>() {}.type
        gson.fromJson(json, tipo) ?: mutableListOf()
    } catch (e: Exception) { mutableListOf() }

    fun guardar(context: Context, lista: List<Obligacion>) {
        context.openFileOutput(ARCHIVO, Context.MODE_PRIVATE)
            .use { it.write(gson.toJson(lista).toByteArray()) }
    }
}