package com.example.recordasunat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/* ============================================================
   INGES BUILDERS RECORDATORIOS PRO - v4.2
   6 pestañas: MENSUAL / ANIVERSARIOS / ANUAL / GENERAL / AYUDA / VERSION
   - AGREGAR pide solo el nombre; el detalle se llena tocando la tarjeta.
   - MENSUAL: cronograma editable por mes (estilo SUNAT).
   - ANIVERSARIOS y ANUAL: check de repetición anual.
   - VERSION: anuncio, ficha y contacto de INGES BUILDERS SCRL.
   ============================================================ */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TemaInges { PantallaPrincipal() } }
    }
}

@Composable
fun TemaInges(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFFB300),
            onPrimary = Color(0xFF14161B),
            secondary = Color(0xFF4FC3F7),
            background = Color(0xFF14161B),
            onBackground = Color.White,
            surface = Color(0xFF1C1F26),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF23262E),
            onSurfaceVariant = Color(0xFFC9CDD6),
            error = Color(0xFFEF5350),
            onError = Color.White
        ),
        content = contenido
    )
}

fun textoBonitoEtiqueta(nombre: String?): String {
    return when (nombre?.uppercase() ?: "") {
        "SUNAT" -> "SUNAT"
        "SERVICIOS" -> "SERVICIOS"
        "FINANZAS" -> "FINANZAS"
        "HOGAR" -> "HOGAR"
        "TRABAJO" -> "TRABAJO"
        else -> if (nombre.isNullOrBlank()) "OTROS" else nombre
    }
}

fun colorDeEtiqueta(nombre: String?): Color = when (nombre?.uppercase() ?: "") {
    "SUNAT" -> Color(0xFFE53935)
    "SERVICIOS" -> Color(0xFF29B6F6)
    "FINANZAS" -> Color(0xFF66BB6A)
    "HOGAR" -> Color(0xFFFFB74D)
    "TRABAJO" -> Color(0xFFAB47BC)
    else -> Color(0xFF90A4AE)
}

fun textoBonitoTipo(nombre: String?): String {
    return when (nombre?.uppercase() ?: "") {
        "CUMPLEANOS" -> "CUMPLEAÑOS"
        "ANIVERSARIO" -> "ANIVERSARIO"
        else -> if (nombre.isNullOrBlank()) "OTRO" else nombre
    }
}

fun colorDeTipo(nombre: String?): Color = when (nombre?.uppercase() ?: "") {
    "CUMPLEANOS" -> Color(0xFFF06292)
    "ANIVERSARIO" -> Color(0xFFFFB300)
    else -> Color(0xFF90A4AE)
}

fun textoDeMonto(item: ItemRecordatorio?): String {
    val m = item?.monto ?: 0.0
    return if (m != 0.0) String.format(Locale.US, "%.2f", m) else ""
}

@Composable
fun PantallaPrincipal() {
    val context = LocalContext.current
    var lista by remember { mutableStateOf<List<ItemRecordatorio>>(emptyList()) }
    var indicePestana by remember { mutableStateOf(0) }
    var dialogoGrupo by remember { mutableStateOf<String?>(null) }
    var itemEditar by remember { mutableStateOf<ItemRecordatorio?>(null) }
    var itemBorrar by remember { mutableStateOf<ItemRecordatorio?>(null) }

    fun recargar() { lista = DatosApp.cargar(context) }

    val launcherPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            Toast.makeText(context, "Notificaciones activadas. En la pestana AYUDA toca PROBAR NOTIFICACION", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "No se activo. Ve a Ajustes > Aplicaciones > INGES BUILDERS RECORDATORIOS PRO > Notificaciones", Toast.LENGTH_LONG).show()
        }
    }

    fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            launcherPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Toast.makeText(context, "Las notificaciones ya estan activadas en este celular", Toast.LENGTH_SHORT).show()
        }
    }

    val launcherExportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                val texto = DatosApp.exportarAniversarios(context)
                context.contentResolver.openOutputStream(uri)?.use { salida -> salida.write(texto.toByteArray(Charsets.UTF_8)) }
                Toast.makeText(context, "Respaldo guardado donde elegiste", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo guardar el respaldo", Toast.LENGTH_LONG).show()
            }
        }
    }

    val launcherImportar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val texto = context.contentResolver.openInputStream(uri)?.use { entrada -> entrada.readBytes().toString(Charsets.UTF_8) } ?: ""
                val r = DatosApp.importarAniversarios(context, texto)
                recargar()
                MotorAlarmas.arrancar(context)
                val mensaje = when {
                    r.agregados <= 0 && r.omitidos <= 0 -> "El archivo no tiene aniversarios que se reconozcan"
                    r.agregados <= 0 -> "No se agregó ninguno: los ${r.omitidos} estaban repetidos"
                    r.omitidos > 0 -> "Se agregaron ${r.agregados} aniversarios (${r.omitidos} repetidos se omitieron)"
                    else -> "Listo: se agregaron ${r.agregados} aniversarios"
                }
                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo leer el archivo", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        MotorAlarmas.arrancar(context)
        recargar()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            launcherPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = Color(0xFF14161B),
        floatingActionButton = {
            if (indicePestana < 4) {
                ExtendedFloatingActionButton(
                    onClick = {
                        dialogoGrupo = when (indicePestana) {
                            0 -> Grupo.MENSUAL.name
                            1 -> Grupo.ANIVERSARIOS.name
                            2 -> Grupo.ANUAL.name
                            else -> Grupo.GENERAL.name
                        }
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(textoAgregar(indicePestana), fontWeight = FontWeight.Bold) },
                    containerColor = Color(0xFFFFB300),
                    contentColor = Color(0xFF14161B)
                )
            }
        }
    ) { paddingInterno ->
        Column(Modifier.padding(paddingInterno)) {
            Encabezado()
            ScrollableTabRow(
                selectedTabIndex = indicePestana,
                containerColor = Color(0xFF1C1F26),
                contentColor = Color.White,
                edgePadding = 0.dp
            ) {
                listOf("MENSUAL", "ANIVERSARIOS", "ANUAL", "GENERAL", "AYUDA", "VERSION").forEachIndexed { i, nombre ->
                    Tab(
                        selected = indicePestana == i,
                        onClick = { indicePestana = i },
                        text = {
                            Text(
                                nombre,
                                fontSize = 10.sp,
                                fontWeight = if (indicePestana == i) FontWeight.Bold else FontWeight.Normal,
                                color = if (indicePestana == i) Color.White else Color(0xFF9AA0B0),
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            if (indicePestana == 4) {
                PantallaAyuda(onActivarNotificaciones = { pedirPermisoNotificaciones() })
            } else if (indicePestana == 5) {
                PantallaVersion()
            } else {
                val grupoActual = when (indicePestana) {
                    0 -> Grupo.MENSUAL
                    1 -> Grupo.ANIVERSARIOS
                    2 -> Grupo.ANUAL
                    else -> Grupo.GENERAL
                }
                val itemsPestana = lista.filter { it.grupo.equals(grupoActual.name, ignoreCase = true) }
                Column {
                    if (grupoActual == Grupo.ANIVERSARIOS) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(onClick = { launcherExportar.launch("respaldo_aniversarios_inges.json") }, modifier = Modifier.weight(1f)) {
                                Text("RESPALDAR", color = Color(0xFF4FC3F7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(onClick = { launcherImportar.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) {
                                Text("IMPORTAR", color = Color(0xFF66BB6A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (itemsPestana.isEmpty()) {
                        EstadoVacio(textoVacio(indicePestana))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(itemsPestana, key = { it.id }) { item ->
                                TarjetaRecordatorio(
                                    item = item,
                                    onEditar = { itemEditar = item },
                                    onBorrar = { itemBorrar = item },
                                    onConfirmar = {
                                        MotorAlarmas.confirmar(context, item.id)
                                        recargar()
                                        Toast.makeText(context, "Confirmado. El ciclo se reinició solo", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (dialogoGrupo != null) {
        DialogoFormulario(
            grupoNombre = dialogoGrupo!!,
            item = null,
            onGuardar = { nuevo ->
                val actual = DatosApp.cargar(context).toMutableList()
                actual.add(nuevo)
                DatosApp.guardar(context, actual)
                MotorAlarmas.programar(context, nuevo)
                recargar()
                dialogoGrupo = null
                Toast.makeText(context, "Creado. Toca la tarjeta para completar los detalles", Toast.LENGTH_LONG).show()
            },
            onCerrar = { dialogoGrupo = null }
        )
    }

    if (itemEditar != null) {
        DialogoFormulario(
            grupoNombre = itemEditar!!.grupo,
            item = itemEditar,
            onGuardar = { actualizado ->
                val actual = DatosApp.cargar(context).toMutableList()
                val posicion = actual.indexOfFirst { it.id == actualizado.id }
                if (posicion >= 0) actual[posicion] = actualizado
                DatosApp.guardar(context, actual)
                if (actualizado.activo) MotorAlarmas.programar(context, actualizado) else MotorAlarmas.arrancar(context)
                recargar()
                itemEditar = null
            },
            onCerrar = { itemEditar = null }
        )
    }

    if (itemBorrar != null) {
        AlertDialog(
            onDismissRequest = { itemBorrar = null },
            containerColor = Color(0xFF1C1F26),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFDDDFE5),
            title = { Text("¿Borrar recordatorio?", fontWeight = FontWeight.Bold) },
            text = { Text("Se va a borrar \"${itemBorrar!!.titulo}\" y se apagan sus alarmas y notificaciones. Esta acción no se deshace.") },
            confirmButton = {
                Button(
                    onClick = {
                        val objetivo = itemBorrar
                        if (objetivo != null) {
                            val actual = DatosApp.cargar(context).filterNot { it.id == objetivo.id }.toMutableList()
                            DatosApp.guardar(context, actual)
                            MotorAlarmas.arrancar(context)
                            recargar()
                        }
                        itemBorrar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350), contentColor = Color.White)
                ) { Text("BORRAR") }
            },
            dismissButton = { TextButton(onClick = { itemBorrar = null }) { Text("CANCELAR", color = Color(0xFF9AA0B0)) } }
        )
    }
}

fun textoAgregar(indice: Int): String = when (indice) {
    0 -> "AGREGAR MENSUAL"
    1 -> "AGREGAR ANIVERSARIO"
    2 -> "AGREGAR ANUAL"
    else -> "AGREGAR GENERAL"
}

fun textoVacio(indice: Int): String = when (indice) {
    0 -> "Aún no hay recordatorios MENSUALES. Toca AGREGAR MENSUAL, pon el nombre (ej: Declarar IGV) y luego toca la tarjeta para poner su cronograma del año."
    1 -> "Aún no hay ANIVERSARIOS. Toca AGREGAR ANIVERSARIO, pon el nombre (ej: Cumpleaños de mamá) y luego toca la tarjeta para poner su fecha."
    2 -> "Aún no hay recordatorios ANUALES. Toca AGREGAR ANUAL, pon el nombre y luego toca la tarjeta para poner su mes y día."
    else -> "Aún no hay recordatorios de fecha puntual. Toca AGREGAR GENERAL, pon el nombre y luego toca la tarjeta para poner fecha y hora."
}

@Composable
fun Encabezado() {
    Column(
        Modifier.fillMaxWidth().background(Color(0xFF14161B)).padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text("INGES BUILDERS", color = Color(0xFFFFB300), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("RECORDATORIOS PRO", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Tus vencimientos bajo control", color = Color(0xFF9AA0B0), fontSize = 12.sp)
    }
}

@Composable
fun PantallaVersion() {
    val context = LocalContext.current
    var msgCorreo by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("INGES BUILDERS", color = Color(0xFFFFB300), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("RECORDATORIOS PRO", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Versión 4.2 - 2026", color = Color(0xFF9AA0B0), fontSize = 13.sp)
        }

        TarjetaAyuda("ANUNCIO DE INGES BUILDERS", listOf(
            "Somos una empresa especializada en construcción, consultoría de obras y soluciones en estructuras metálicas. Nuestro equipo multidisciplinario de especialistas te acompaña desde el cálculo y diseño estructural hasta la fabricación y ejecución en campo, asegurando eficiencia y garantía en cada proyecto."
        ))

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1F26)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Especialidad principal", color = Color(0xFFFFB300), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Diseño, fabricación y montaje de estructuras metálicas (naves industriales, coberturas, puentes, edificaciones).",
                    color = Color(0xFFDDDFE5), fontSize = 13.sp
                )
                Spacer(Modifier.height(6.dp))
                Text("Servicios complementarios", color = Color(0xFFFFB300), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Ejecución de obras civiles, elaboración de expedientes técnicos y supervisión/consultoría de ingeniería.",
                    color = Color(0xFFDDDFE5), fontSize = 13.sp
                )
            }
        }

        Button(
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:ingesbuilders@gmail.com")))
                    msgCorreo = null
                } catch (e: Exception) {
                    msgCorreo = "No encontré una app de correo instalada. Escríbenos a: ingesbuilders@gmail.com"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color(0xFF14161B))
        ) {
            Icon(Icons.Filled.Email, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Escribir al Correo", fontWeight = FontWeight.Bold)
        }
        Text(
            "ingesbuilders@gmail.com",
            color = Color(0xFFFFB300), fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        msgCorreo?.let {
            Text(
                it, color = Color(0xFFEF5350), fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1F26)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LineaVersion("Version", "4.2 - 2026")
                LineaVersion("Autor", "Hugo Cerquin M")
                LineaVersion("Empresa", "INGES BUILDERS SCRL.")
                LineaVersion("Ciudad", "CAJAMARCA, PERU")
                LineaVersion("App", "INGES BUILDERS RECORDATORIOS PRO")
            }
        }

        TarjetaAyuda("CONDICIONES DE USO", listOf(
            "1) Esta app es una herramienta de apoyo personal: te recuerda a tiempo, pero atender cada obligación es tu responsabilidad.",
            "2) No reemplaza los canales oficiales de SUNAT, bancos ni ninguna entidad. Verifica siempre las fechas y montos oficiales.",
            "3) Tus datos se guardan únicamente en este celular; la app no los envía a internet.",
            "4) Si cambias de celular o desinstalas la app se pierden los recordatorios, salvo el respaldo de ANIVERSARIOS que hayas guardado con el botón RESPALDAR.",
            "5) INGES BUILDERS no se responsabiliza por multas, recargos o perjuicios causados por obligaciones no atendidas a tiempo."
        ))

        Text(
            "COPYRIGHT © 2026 — INGES BUILDERS SCRL. Todos los derechos reservados.",
            color = Color(0xFF9AA0B0), fontSize = 12.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun LineaVersion(clave: String, valor: String) {
    Row {
        Text("$clave:", color = Color(0xFF9AA0B0), fontSize = 14.sp, modifier = Modifier.width(95.dp))
        Text(valor, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EstadoVacio(mensaje: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("AÚN NO HAY NADA AQUÍ", color = Color(0xFF9AA0B0), fontWeight = FontWeight.Bold)
        Text(
            mensaje,
            color = Color(0xFF6B7280),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp)
        )
    }
}

@Composable
fun ChipColor(texto: String, fondo: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .widthIn(max = 220.dp)
    ) {
        Text(texto, color = Color(0xFF14161B), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun ChipSelector(texto: String, colorBase: Color, seleccionado: Boolean, alTocar: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (seleccionado) colorBase else Color(0xFF23262E))
            .border(1.dp, if (seleccionado) Color.White else Color(0xFF3A3F4B), RoundedCornerShape(16.dp))
            .clickable { alTocar() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(texto, color = if (seleccionado) Color(0xFF14161B) else Color(0xFFC9CDD6), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    numero: Boolean = false,
    apoyo: String = ""
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        placeholder = {
            if (apoyo.isNotEmpty()) Text(apoyo, color = Color(0xFF6B7280))
        },
        keyboardOptions = KeyboardOptions(keyboardType = if (numero) KeyboardType.Number else KeyboardType.Text),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFFFFB300),
            focusedBorderColor = Color(0xFFFFB300),
            unfocusedBorderColor = Color(0xFF3A3F4B),
            focusedLabelColor = Color(0xFFFFB300),
            unfocusedLabelColor = Color(0xFF9AA0B0),
            focusedPlaceholderColor = Color(0xFF6B7280),
            unfocusedPlaceholderColor = Color(0xFF6B7280)
        )
    )
}

@Composable
fun CeldaDiaMes(nombre: String, valor: String, alCambiar: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(nombre, color = Color(0xFF9AA0B0), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = valor,
            onValueChange = { v ->
                if (v.length <= 2 && v.all { c -> c.isDigit() }) alCambiar(v)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFFFB300),
                focusedBorderColor = Color(0xFFFFB300),
                unfocusedBorderColor = Color(0xFF3A3F4B)
            )
        )
    }
}

@Composable
fun TarjetaRecordatorio(
    item: ItemRecordatorio,
    onEditar: () -> Unit,
    onBorrar: () -> Unit,
    onConfirmar: () -> Unit
) {
    val ahora = LocalDateTime.now()
    val vencimiento = DatosApp.vencimientoDe(item)
    val confirmado = vencimiento != null && DatosApp.yaConfirmado(item, vencimiento)
    val apagado = !item.activo
    val esMensualItem = item.grupo.equals(Grupo.MENSUAL.name, ignoreCase = true)

    val textoConfirmado = if (
        item.grupo.equals(Grupo.ANUAL.name, ignoreCase = true) ||
        item.grupo.equals(Grupo.ANIVERSARIOS.name, ignoreCase = true)
    ) {
        "Confirmado. Se volverá a activar solo para el próximo año."
    } else {
        "Ya confirmaste este ciclo. Te avisamos de nuevo en el siguiente."
    }

    val textoEstado: String
    val colorEstado: Color
    if (apagado) {
        textoEstado = "REPETICIÓN APAGADA"
        colorEstado = Color(0xFF9AA0B0)
    } else if (confirmado) {
        textoEstado = "CONFIRMADO ESTE CICLO"
        colorEstado = Color(0xFF66BB6A)
    } else if (vencimiento == null) {
        if (esMensualItem && DatosApp.diasPorMesDe(item)[ahora.monthValue - 1] <= 0) {
            textoEstado = "ESTE MES NO APLICA - toca la tarjeta si quieres cambiar el cronograma"
        } else {
            textoEstado = "FALTA COMPLETAR DATOS - toca la tarjeta"
        }
        colorEstado = Color(0xFFFFB300)
    } else {
        val dias = Duration.between(ahora, vencimiento).toDays()
        if (dias < 0) {
            textoEstado = "VENCIDO"
            colorEstado = Color(0xFFEF5350)
        } else if (dias == 0L) {
            textoEstado = "VENCE HOY"
            colorEstado = Color(0xFFFFB300)
        } else {
            textoEstado = "Vence en $dias días"
            colorEstado = Color(0xFF4FC3F7)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEditar() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1F26))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.titulo, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onEditar) { Text("EDITAR", color = Color(0xFF4FC3F7), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                TextButton(onClick = onBorrar) { Text("BORRAR", color = Color(0xFFEF5350), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
            if (esMensualItem) {
                ChipColor(texto = textoBonitoEtiqueta(item.etiqueta), fondo = colorDeEtiqueta(item.etiqueta))
            }
            if (item.grupo.equals(Grupo.ANIVERSARIOS.name, ignoreCase = true)) {
                ChipColor(texto = textoBonitoTipo(item.tipoAniversario), fondo = colorDeTipo(item.tipoAniversario))
            }
            Text(DatosApp.descripcionVencimiento(item), color = Color(0xFFC9CDD6), fontSize = 13.sp)
            Text(textoEstado, color = colorEstado, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (item.monto != null && item.monto != 0.0) {
                Text("Monto: " + String.format(Locale.US, "S/ %.2f", item.monto), color = Color(0xFFC9CDD6), fontSize = 13.sp)
            }
            if (!item.nota.isNullOrEmpty()) Text(item.nota, color = Color(0xFF9AA0B0), fontSize = 13.sp)
            if (apagado) {
                Text("Repetición apagada: no volverá a sonar hasta que la actives en EDITAR.", color = Color(0xFF9AA0B0), fontSize = 12.sp)
            } else if (confirmado) {
                Text(textoConfirmado, color = Color(0xFF66BB6A), fontSize = 12.sp)
            } else if (vencimiento != null) {
                Button(
                    onClick = onConfirmar,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White)
                ) {
                    Text(DatosApp.textoBotonConfirmar(item), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DialogoFormulario(
    grupoNombre: String,
    item: ItemRecordatorio?,
    onGuardar: (ItemRecordatorio) -> Unit,
    onCerrar: () -> Unit
) {
    val esEdicion = item != null
    val esMensual = grupoNombre.equals(Grupo.MENSUAL.name, ignoreCase = true)
    val esAniversarios = grupoNombre.equals(Grupo.ANIVERSARIOS.name, ignoreCase = true)
    val esAnual = grupoNombre.equals(Grupo.ANUAL.name, ignoreCase = true)
    val esGeneral = grupoNombre.equals(Grupo.GENERAL.name, ignoreCase = true)

    var titulo by remember { mutableStateOf(item?.titulo ?: "") }
    var etiquetaSel by remember { mutableStateOf((item?.etiqueta ?: EtiquetaMensual.OTROS.name).uppercase()) }
    var tipoSel by remember { mutableStateOf((item?.tipoAniversario ?: TipoAniversario.CUMPLEANOS.name).uppercase().replace("Ñ", "N")) }
    var diaTexto by remember { mutableStateOf(item?.diaMes?.toString() ?: "15") }
    var mesIndice by remember { mutableStateOf(((item?.mes ?: 1) - 1).coerceIn(0, 11)) }
    var fechaTexto by remember { mutableStateOf(item?.fechaGeneral ?: "") }
    var horaTexto by remember { mutableStateOf(item?.horaGeneral ?: "08:00") }
    var montoTexto by remember { mutableStateOf(textoDeMonto(item)) }
    var notaTexto by remember { mutableStateOf(item?.nota ?: "") }
    var repiteAnual by remember { mutableStateOf(item?.activo ?: true) }
    var mensajeError by remember { mutableStateOf("") }
    var diasGrid by remember {
        mutableStateOf(
            if (item != null && item.grupo.equals(Grupo.MENSUAL.name, ignoreCase = true))
                DatosApp.diasPorMesDe(item).map { if (it > 0) it.toString() else "" }
            else List(12) { "" }
        )
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = Color(0xFF1C1F26),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFDDDFE5),
        title = { Text(if (esEdicion) "EDITAR ${grupoNombre.uppercase()}" else "AGREGAR ${grupoNombre.uppercase()}", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()).heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (mensajeError.isNotEmpty()) {
                    Text(mensajeError, color = Color(0xFFEF5350), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                CampoTexto(titulo, { titulo = it }, "Nombre del recordatorio", apoyo = "Ej: IGV - PDT 621")

                if (!esEdicion) {
                    Text(
                        "Solo escribe el nombre. Después tocas la tarjeta en la lista para completar los detalles: cronograma, etiqueta, monto, hora y nota.",
                        color = Color(0xFF9AA0B0),
                        fontSize = 12.sp
                    )
                } else {
                    if (esMensual) {
                        Text("Etiqueta:", color = Color(0xFF9AA0B0), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChipSelector("SUNAT", colorDeEtiqueta("SUNAT"), etiquetaSel == EtiquetaMensual.SUNAT.name) { etiquetaSel = EtiquetaMensual.SUNAT.name }
                            ChipSelector("SERVICIOS", colorDeEtiqueta("SERVICIOS"), etiquetaSel == EtiquetaMensual.SERVICIOS.name) { etiquetaSel = EtiquetaMensual.SERVICIOS.name }
                            ChipSelector("FINANZAS", colorDeEtiqueta("FINANZAS"), etiquetaSel == EtiquetaMensual.FINANZAS.name) { etiquetaSel = EtiquetaMensual.FINANZAS.name }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChipSelector("HOGAR", colorDeEtiqueta("HOGAR"), etiquetaSel == EtiquetaMensual.HOGAR.name) { etiquetaSel = EtiquetaMensual.HOGAR.name }
                            ChipSelector("TRABAJO", colorDeEtiqueta("TRABAJO"), etiquetaSel == EtiquetaMensual.TRABAJO.name) { etiquetaSel = EtiquetaMensual.TRABAJO.name }
                            ChipSelector("OTROS", colorDeEtiqueta("OTROS"), etiquetaSel == EtiquetaMensual.OTROS.name) { etiquetaSel = EtiquetaMensual.OTROS.name }
                        }
                    }

                    if (esAniversarios) {
                        Text("Tipo:", color = Color(0xFF9AA0B0), fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChipSelector("CUMPLEAÑOS", colorDeTipo("CUMPLEANOS"), tipoSel == TipoAniversario.CUMPLEANOS.name) { tipoSel = TipoAniversario.CUMPLEANOS.name }
                            ChipSelector("ANIVERSARIO", colorDeTipo("ANIVERSARIO"), tipoSel == TipoAniversario.ANIVERSARIO.name) { tipoSel = TipoAniversario.ANIVERSARIO.name }
                            ChipSelector("OTRO", colorDeTipo("OTRO"), tipoSel == TipoAniversario.OTRO.name) { tipoSel = TipoAniversario.OTRO.name }
                        }
                    }

                    if (esMensual) {
                        Text("DÍA DE CADA MES (vacío o 0 = ese mes no aplica):", color = Color(0xFF9AA0B0), fontSize = 12.sp)
                        val mesesCortos = listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SET", "OCT", "NOV", "DIC")
                        for (fila in 0 until 3) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (col in 0 until 4) {
                                    val i = fila * 4 + col
                                    CeldaDiaMes(
                                        nombre = mesesCortos[i],
                                        valor = diasGrid[i],
                                        modifier = Modifier.weight(1f)
                                    ) { v -> diasGrid = diasGrid.toMutableList().also { it[i] = v } }
                                }
                            }
                        }
                        Text(
                            "Ejemplo: si tu IGV vence el 12 casi todo el año pero en octubre vence el 18, deja 12 en todos los meses y escribe 18 solo en OCT.",
                            color = Color(0xFF6B7280),
                            fontSize = 11.sp
                        )
                    }

                    if (esAniversarios || esAnual) {
                        CampoTexto(diaTexto, { diaTexto = it }, "Día del mes (1 a 31)", numero = true)
                        Text("Mes:", color = Color(0xFF9AA0B0), fontSize = 12.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { mesIndice = if (mesIndice == 0) 11 else mesIndice - 1 }) {
                                Text("<", color = Color(0xFF4FC3F7), fontWeight = FontWeight.Bold)
                            }
                            Text(DatosApp.MESES[mesIndice], color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { mesIndice = if (mesIndice == 11) 0 else mesIndice + 1 }) {
                                Text(">", color = Color(0xFF4FC3F7), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (esMensual) {
                        CampoTexto(montoTexto, { montoTexto = it }, "Monto en soles (opcional)", numero = false, apoyo = "Ej: 550.00")
                    }

                    if (esGeneral) {
                        CampoTexto(fechaTexto, { fechaTexto = it }, "Fecha", apoyo = "Ej: 15/03/2026")
                    }

                    CampoTexto(horaTexto, { horaTexto = it }, "Hora de la alarma", apoyo = "Ej: 08:00")
                    Text(
                        "La alarma sonará a esta hora el día del vencimiento.",
                        color = Color(0xFF6B7280),
                        fontSize = 11.sp
                    )

                    if (esAnual || esAniversarios) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = repiteAnual,
                                onCheckedChange = { repiteAnual = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF66BB6A),
                                    checkmarkColor = Color(0xFF14161B),
                                    uncheckedColor = Color(0xFF3A3F4B)
                                )
                            )
                            Text("Se repite cada año (automático)", color = Color.White, fontSize = 13.sp)
                        }
                        Text(
                            "Al confirmar con el botón, se vuelve a activar solo para el próximo año. Si lo desmarcas, deja de sonar.",
                            color = Color(0xFF9AA0B0),
                            fontSize = 11.sp
                        )
                    }

                    CampoTexto(notaTexto, { notaTexto = it }, "Nota (opcional)", apoyo = "Ej: usar clave del banco")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tituloLimpio = titulo.trim()
                    val horaValida: LocalTime? = try {
                        if (horaTexto.trim().isEmpty()) {
                            LocalTime.of(8, 0)
                        } else {
                            val hp = horaTexto.trim().split(":")
                            LocalTime.of(
                                hp[0].trim().toIntOrNull() ?: -1,
                                (hp.getOrNull(1) ?: "0").trim().toIntOrNull() ?: -1
                            )
                        }
                    } catch (e: Exception) { null }

                    val diasGridNum = diasGrid.map { (it.toIntOrNull() ?: 0).coerceIn(0, 31) }

                    var fechaISO = ""
                    var fechaOk = true
                    if (esGeneral && esEdicion) {
                        val p = fechaTexto.trim().split("/")
                        if (p.size == 3) {
                            var anio = p[2].trim().toIntOrNull() ?: -1
                            if (anio in 0..99) anio += 2000
                            val f = try {
                                LocalDate.of(anio, p[1].trim().toIntOrNull() ?: -1, p[0].trim().toIntOrNull() ?: -1)
                            } catch (e: Exception) { null }
                            if (f == null) fechaOk = false else fechaISO = f.toString()
                        } else fechaOk = false
                    }

                    val dia = diaTexto.trim().toIntOrNull()

                    when {
                        tituloLimpio.isEmpty() ->
                            mensajeError = "Escribe un nombre para el recordatorio."
                        !esEdicion -> {
                            val mesActual = LocalDate.now().monthValue
                            onGuardar(
                                ItemRecordatorio(
                                    id = System.currentTimeMillis().toString(),
                                    grupo = grupoNombre,
                                    titulo = tituloLimpio,
                                    etiqueta = EtiquetaMensual.OTROS.name,
                                    diaMes = if (esGeneral) 1 else 15,
                                    mes = if (esGeneral) 1 else mesActual,
                                    fechaGeneral = "",
                                    horaGeneral = "08:00",
                                    tipoAniversario = if (esAniversarios) TipoAniversario.CUMPLEANOS.name else TipoAniversario.OTRO.name,
                                    monto = 0.0,
                                    nota = "",
                                    activo = true,
                                    cicloConfirmado = ""
                                )
                            )
                        }
                        horaValida == null ->
                            mensajeError = "Escribe la hora como 09:30."
                        esGeneral && !fechaOk ->
                            mensajeError = "Escribe la fecha como 15/03/2026."
                        esMensual && diasGridNum.none { it > 0 } ->
                            mensajeError = "Pon al menos un día (1 a 31) en algún mes del cronograma."
                        !esGeneral && !esMensual && (dia == null || dia < 1 || dia > 31) ->
                            mensajeError = "El día debe ser un número de 1 a 31."
                        else -> {
                            val base = item!!
                            val actualizado = base.copy(
                                titulo = tituloLimpio,
                                etiqueta = if (esMensual) etiquetaSel else base.etiqueta,
                                diaMes = when {
                                    esMensual -> diasGridNum.first { it > 0 }
                                    esGeneral -> 1
                                    else -> dia!!
                                },
                                mes = if (esGeneral) 1 else if (esMensual) base.mes else mesIndice + 1,
                                diasMes = if (esMensual) diasGridNum.joinToString(",") else base.diasMes,
                                fechaGeneral = if (esGeneral) fechaISO else base.fechaGeneral,
                                horaGeneral = horaValida!!.toString(),
                                tipoAniversario = if (esAniversarios) tipoSel else base.tipoAniversario,
                                monto = if (esMensual) (montoTexto.trim().replace(",", ".").toDoubleOrNull() ?: 0.0) else base.monto,
                                nota = notaTexto.trim(),
                                activo = if (esAnual || esAniversarios) repiteAnual else base.activo,
                                cicloConfirmado = ""
                            )
                            onGuardar(actualizado)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color(0xFF14161B))
            ) { Text(if (esEdicion) "GUARDAR CAMBIOS" else "GUARDAR", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("CANCELAR", color = Color(0xFF9AA0B0)) } }
    )
}