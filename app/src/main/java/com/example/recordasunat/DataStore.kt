package com.example.recordasunat

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

/* ============================================================
   MODELOS Y DATOS - INGES BUILDERS RECORDATORIOS PRO v4.0
   ============================================================ */

enum class Grupo(val titulo: String) {
    MENSUAL("MENSUAL"),
    ANIVERSARIOS("ANIVERSARIOS"),
    ANUAL("ANUAL"),
    GENERAL("GENERAL")
}

enum class EtiquetaMensual(val texto: String) {
    SUNAT("SUNAT"),
    SERVICIOS("SERVICIOS"),
    FINANZAS("FINANZAS"),
    HOGAR("HOGAR"),
    TRABAJO("TRABAJO"),
    OTROS("OTROS")
}

enum class TipoAniversario(val texto: String) {
    CUMPLEANOS("CUMPLEAÑOS"),
    ANIVERSARIO("ANIVERSARIO"),
    OTRO("OTRO")
}

data class ItemRecordatorio(
    val id: String = UUID.randomUUID().toString(),
    val grupo: String = Grupo.MENSUAL.name,
    val titulo: String = "",
    val etiqueta: String = EtiquetaMensual.OTROS.name,
    val diaMes: Int = 1,
    val mes: Int = 1,
    val fechaGeneral: String = "",
    val horaGeneral: String = "08:00",
    val tipoAniversario: String = TipoAniversario.CUMPLEANOS.name,
    val monto: Double = 0.0,
    val nota: String = "",
    val activo: Boolean = true,
    val cicloConfirmado: String = ""
)

object DatosApp {

    private const val ARCHIVO = "recordatorios_pro.json"
    private const val PREFS = "config_inges"

    val MESES = arrayOf("enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "setiembre", "octubre", "noviembre", "diciembre")

    private val HORA_ALARMA: LocalTime = LocalTime.of(8, 0)

    /* ---------- almacenamiento ---------- */

    fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun cargar(context: Context): MutableList<ItemRecordatorio> {
        return try {
            val f = File(context.filesDir, ARCHIVO)
            if (!f.exists() || f.readText().isBlank()) mutableListOf()
            else Gson().fromJson(f.readText(), Array<ItemRecordatorio>::class.java)
                .toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun guardar(context: Context, lista: List<ItemRecordatorio>) {
        try {
            File(context.filesDir, ARCHIVO).writeText(Gson().toJson(lista))
        } catch (e: Exception) {
        }
    }

    /* ---------- fechas y ciclos ---------- */

    fun fechaSegura(anio: Int, mes: Int, dia: Int): LocalDate {
        val ultimoDia = YearMonth.of(anio, mes).lengthOfMonth()
        return LocalDate.of(anio, mes, dia.coerceIn(1, ultimoDia))
    }

    // Vencimiento del ciclo ACTUAL (puede estar en el pasado si no confirmó)
    fun vencimientoDe(item: ItemRecordatorio, ahora: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        return when (item.grupo) {
            Grupo.MENSUAL.name ->
                fechaSegura(ahora.year, ahora.monthValue, item.diaMes).atTime(HORA_ALARMA)
            Grupo.ANIVERSARIOS.name, Grupo.ANUAL.name ->
                fechaSegura(ahora.year, item.mes, item.diaMes).atTime(HORA_ALARMA)
            Grupo.GENERAL.name -> {
                try {
                    val f = LocalDate.parse(item.fechaGeneral, DateTimeFormatter.ISO_LOCAL_DATE)
                    val h = try {
                        LocalTime.parse(item.horaGeneral, DateTimeFormatter.ofPattern("HH:mm"))
                    } catch (e: Exception) {
                        HORA_ALARMA
                    }
                    f.atTime(h)
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }

    fun avanzarCiclo(item: ItemRecordatorio, venc: LocalDateTime): LocalDateTime {
        return when (item.grupo) {
            Grupo.MENSUAL.name -> {
                val sig = venc.plusMonths(1)
                fechaSegura(sig.year, sig.monthValue, item.diaMes).atTime(HORA_ALARMA)
            }
            Grupo.ANIVERSARIOS.name, Grupo.ANUAL.name -> {
                val sig = venc.plusYears(1)
                fechaSegura(sig.year, item.mes, item.diaMes).atTime(HORA_ALARMA)
            }
            else -> venc
        }
    }

    fun cicloDe(venc: LocalDateTime, grupo: String): String = when (grupo) {
        Grupo.MENSUAL.name -> "%04d-%02d".format(venc.year, venc.monthValue)
        Grupo.GENERAL.name -> "%04d-%02d-%02d".format(venc.year, venc.monthValue, venc.dayOfMonth)
        else -> "%04d".format(venc.year)
    }

    fun yaConfirmado(item: ItemRecordatorio, venc: LocalDateTime): Boolean =
        item.cicloConfirmado.isNotEmpty() && item.cicloConfirmado == cicloDe(venc, item.grupo)

    fun diasAviso(item: ItemRecordatorio): Long =
        if (item.grupo == Grupo.ANIVERSARIOS.name) 7L else 5L

    /* ---------- textos ---------- */

    fun descripcionVencimiento(item: ItemRecordatorio): String {
        return when (item.grupo) {
            Grupo.MENSUAL.name -> "Día ${item.diaMes} de cada mes"
            Grupo.ANIVERSARIOS.name ->
                "Cada año: ${item.diaMes} de ${MESES[(item.mes - 1).coerceIn(0, 11)]}"
            Grupo.ANUAL.name ->
                "Cada año: ${item.diaMes} de ${MESES[(item.mes - 1).coerceIn(0, 11)]}"
            Grupo.GENERAL.name -> "${item.fechaGeneral} a las ${item.horaGeneral}"
            else -> ""
        }
    }

    fun textoAviso(item: ItemRecordatorio, venc: LocalDateTime, ahora: LocalDateTime): String {
        val dias = Duration.between(ahora, venc).toDays()
        val cuando = when (item.grupo) {
            Grupo.MENSUAL.name -> "día ${item.diaMes}"
            Grupo.GENERAL.name -> venc.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            else -> "${venc.dayOfMonth} de ${MESES[(venc.monthValue - 1).coerceIn(0, 11)]}"
        }
        return if (dias <= 0L) "Vence hoy ($cuando)"
        else "Vence en $dias día${if (dias == 1L) "" else "s"} ($cuando)"
    }

    fun textoAlarma(item: ItemRecordatorio, venc: LocalDateTime, ahora: LocalDateTime): String {
        return if (!venc.isAfter(ahora) && Duration.between(venc, ahora).toDays() < 1L)
            "VENCE HOY" else "VENCIDO"
    }

    fun textoBotonConfirmar(item: ItemRecordatorio): String {
        if (item.grupo == Grupo.MENSUAL.name) {
            return when (item.etiqueta) {
                EtiquetaMensual.SUNAT.name -> "YA DECLARÉ"
                EtiquetaMensual.SERVICIOS.name -> "YA PAGUÉ"
                else -> "YA LO HICE"
            }
        }
        return "YA LO HICE"
    }

    /* ---------- respaldo de ANIVERSARIOS ---------- */

    fun exportarAniversarios(context: Context): String {
        val lista = cargar(context).filter {
            it.grupo == Grupo.ANIVERSARIOS.name && it.activo
        }
        val raiz = JsonObject()
        raiz.addProperty("app", "INGES BUILDERS RECORDATORIOS PRO")
        raiz.addProperty("tipo", "ANIVERSARIOS")
        raiz.addProperty("version", 4)
        val arr = JsonArray()
        lista.forEach { arr.add(Gson().toJsonTree(it)) }
        raiz.add("items", arr)
        return Gson().toJson(raiz)
    }

    // Importador flexible: entiende el respaldo nuevo (items) y el respaldo
    // antiguo de cumpleaños (array o objeto con varios nombres de campos)
    fun importarAniversarios(context: Context, texto: String): Int {
        var agregados = 0
        try {
            val raiz: JsonElement = Gson().fromJson(texto, JsonElement::class.java)
            val arr: JsonArray = when {
                raiz.isJsonArray -> raiz.asJsonArray
                raiz.isJsonObject -> {
                    val o = raiz.asJsonObject
                    val claves = arrayOf(
                        "items", "aniversarios", "cumpleanos", "cumpleaños",
                        "recordatorios", "datos", "lista"
                    )
                    var encontrado: JsonArray? = null
                    for (c in claves) {
                        val v = o.get(c)
                        if (v != null && v.isJsonArray) {
                            encontrado = v.asJsonArray
                            break
                        }
                    }
                    encontrado ?: JsonArray()
                }
                else -> JsonArray()
            }
            val lista = cargar(context)
            for (elemento in arr) {
                try {
                    if (!elemento.isJsonObject) continue
                    val o = elemento.asJsonObject
                    val titulo = campo(
                        o, "titulo", "nombre", "name", "title",
                        "texto", "descripcion", "detalle"
                    ) ?: continue
                    val par = diaMesDe(o) ?: continue
                    if (par.second !in 1..12 || par.first !in 1..31) continue
                    val tipoBruto = campo(o, "tipoAniversario", "tipo", "type")
                    val tipo = when (tipoBruto?.trim()?.uppercase()) {
                        "CUMPLEAÑOS", "CUMPLEANOS", "CUMPLE", "BIRTHDAY" ->
                            TipoAniversario.CUMPLEANOS.name
                        "ANIVERSARIO", "ANNIVERSARY" -> TipoAniversario.ANIVERSARIO.name
                        null -> TipoAniversario.CUMPLEANOS.name
                        else -> TipoAniversario.OTRO.name
                    }
                    lista.add(
                        ItemRecordatorio(
                            id = UUID.randomUUID().toString(),
                            grupo = Grupo.ANIVERSARIOS.name,
                            titulo = titulo,
                            tipoAniversario = tipo,
                            diaMes = par.first,
                            mes = par.second,
                            nota = campo(o, "nota", "note", "comentario", "observacion") ?: ""
                        )
                    )
                    agregados++
                } catch (e: Exception) {
                }
            }
            if (agregados > 0) guardar(context, lista)
        } catch (e: Exception) {
        }
        return agregados
    }

    private fun campo(o: JsonObject, vararg claves: String): String? {
        for (c in claves) {
            val v = o.get(c)
            if (v != null && v.isJsonPrimitive) {
                val s = v.asString.trim()
                if (s.isNotEmpty()) return s
            }
        }
        return null
    }

    private fun diaMesDe(o: JsonObject): Pair<Int, Int>? {
        val diaTexto = campo(o, "dia", "diaMes", "day")
        val mesTexto = campo(o, "mes", "month")
        if (diaTexto != null && mesTexto != null) {
            val d = diaTexto.toIntOrNull()
            val m = mesTexto.toIntOrNull()
            if (d != null && m != null) return Pair(d, m)
        }
        for (clave in arrayOf("fecha", "date", "fechaISO", "fechaAniversario", "fechaVencimiento")) {
            val f = campo(o, clave) ?: continue
            try {
                if (f.length >= 10 && f.contains("-")) {
                    val d = f.substring(8, 10).toIntOrNull()
                    val m = f.substring(5, 7).toIntOrNull()
                    if (d != null && m != null) return Pair(d, m)
                } else if (f.length >= 10 && f.contains("/")) {
                    val partes = f.split("/")
                    if (partes.size >= 3) {
                        val d = partes[0].toIntOrNull()
                        val m = partes[1].toIntOrNull()
                        if (d != null && m != null) return Pair(d, m)
                    }
                }
            } catch (e: Exception) {
            }
        }
        return null
    }
}