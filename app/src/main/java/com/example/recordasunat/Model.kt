package com.example.recordasunat

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Obligacion(
    var id: Long = 0,
    var nombre: String = "",
    var detalle: String = "",
    var tipo: String = "SUNAT",                    // "SUNAT" (mensual) o "GENERAL"
    var vencimientos: Map<Int, Int> = emptyMap(),  // SUNAT: periodo 1..12 -> día (vence el mes siguiente)
    var diaMes: Int = 1,                           // GENERAL: día
    var mesAnual: Int = 1,                         // GENERAL: mes
    var repetirAnual: Boolean = true,              // GENERAL: se repite cada año (cumpleaños)
    var anioEspecifico: Int? = null,               // GENERAL: año si NO se repite
    var diasAviso: Int = 5,
    var periodoDeclarado: String? = null           // SUNAT: "2026-08" / GENERAL: "2026-07-21"
)

object Store {
    private const val ARCHIVO = "obligaciones.json"
    private val gson = Gson()

    fun cargar(context: Context): MutableList<Obligacion> = try {
        val json = context.openFileInput(ARCHIVO).bufferedReader().use { it.readText() }
        val tipo = object : TypeToken<MutableList<Obligacion>>() {}.type
        val lista: MutableList<Obligacion> = gson.fromJson(json, tipo) ?: mutableListOf()
        // Normaliza registros guardados con la versión anterior
        lista.forEach { o ->
            if (o.tipo == null) o.tipo = "SUNAT"
            if (o.vencimientos == null) o.vencimientos = emptyMap()
            if (o.nombre == null) o.nombre = ""
            if (o.detalle == null) o.detalle = ""
            if (o.diasAviso <= 0) o.diasAviso = 5
            if (o.diaMes !in 1..31) o.diaMes = 1
            if (o.mesAnual !in 1..12) o.mesAnual = 1
        }
        lista
    } catch (e: Exception) { mutableListOf() }

    fun guardar(context: Context, lista: List<Obligacion>) {
        context.openFileOutput(ARCHIVO, Context.MODE_PRIVATE)
            .use { it.write(gson.toJson(lista).toByteArray()) }
    }
}