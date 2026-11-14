package com.example.recordasunat

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instalarReporteDeCrash()
        try {
            Notifier.crearCanales(this)
            ReminderWorker.programar(this)
            AlertScheduler.programarSiguiente(this, Config.SUNAT)
            AlertScheduler.programarSiguiente(this, Config.GENERAL)
        } catch (_: Exception) { }
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFD4AF37),
                    onPrimary = Color(0xFF1A1400),
                    background = Color(0xFF121110),
                    onBackground = Color(0xFFEDE6D6),
                    surface = Color(0xFF1C1913),
                    onSurface = Color(0xFFEDE6D6),
                    surfaceVariant = Color(0xFF2B2312),
                    onSurfaceVariant = Color(0xFFD4AF37),
                    error = Color(0xFFCF6679)
                )
            ) { Raiz() }
        }
    }

    /**
     * Red de seguridad: si la app llega a botar, guarda el error en un
     * archivo interno y lo muestra en pantalla (aviso flotante), para poder
     * diagnosticarlo sin computadora.
     */
    private fun instalarReporteDeCrash() {
        try {
            val previo = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { hilo, error ->
                try {
                    val marca = java.text.SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                        .format(java.util.Date())
                    File(filesDir, "crash.txt").writeText(
                        "$marca\n${error.javaClass.name}: ${error.message ?: "(sin mensaje)"}\n\n" +
                            error.stackTraceToString().take(1500)
                    )
                    try {
                        Toast.makeText(
                            applicationContext,
                            "RecordaSUNAT falló: ${error.javaClass.simpleName}: ${error.message?.take(110) ?: ""}",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (_: Exception) { }
                } catch (_: Exception) { }
                previo?.uncaughtException(hilo, error)
            }
        } catch (_: Exception) { }
    }
}

private val MESES_ANIO = listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")
private val ORO = Color(0xFFD4AF37)
private val ROJO_SUAVE = Color(0xFFE57373)

private val POLITICA: List<Pair<String, String>> = listOf(
    "1. Almacenamiento Estricto en el Dispositivo" to
        "Esta aplicación opera bajo un modelo de almacenamiento estrictamente local. Todos los recordatorios, " +
        "fechas de cumpleaños, nombres, notas y configuraciones ingresados por el usuario se guardan únicamente " +
        "en la memoria interna de su dispositivo. El desarrollador no recopila, transmite, almacena en servidores " +
        "externos ni comparte ningún dato personal o información ingresada en la aplicación.",
    "2. Ausencia de Rastreo y Servicios de Terceros" to
        "Esta aplicación no utiliza cookies, identificadores de publicidad, herramientas de analítica ni servicios " +
        "de rastreo de terceros. Su actividad dentro de la aplicación es completamente privada y no se monitorea " +
        "ni se perfila con fines comerciales o estadísticos. Al utilizar la aplicación, usted acepta estas " +
        "condiciones de uso local.",
    "3. Permisos del Sistema" to
        "La aplicación solicita únicamente permisos de notificaciones y alarmas del sistema con el exclusivo " +
        "propósito de alertar al usuario sobre los eventos y recordatorios programados localmente. No se accede a " +
        "la cámara, micrófono, ubicación ni a otros archivos personales del teléfono sin su acción directa.",
    "4. Seguridad y Eliminación de Datos" to
        "Al almacenarse los datos exclusivamente en el dispositivo del usuario, este mantiene el control total y " +
        "la seguridad sobre ellos mediante los mecanismos de protección propios de su teléfono. Puede editar o " +
        "eliminar cualquier recordatorio directamente desde la aplicación, o borrar toda la información de forma " +
        "permanente desinstalando la aplicación o borrando los datos de almacenamiento desde los ajustes de su " +
        "dispositivo.",
    "5. Exención de Responsabilidad por Pérdida de Datos" to
        "Dado que toda la información se almacena exclusivamente de forma local en su dispositivo y no existe " +
        "sincronización ni respaldo en servidores externos del desarrollador, el usuario es el único responsable " +
        "de conservar sus datos. En caso de pérdida, robo, daño del dispositivo, restablecimiento de fábrica o " +
        "desinstalación de la aplicación sin haber realizado una copia de seguridad previa (si dicha función " +
        "estuviera disponible), los recordatorios y fechas almacenadas se perderán de forma permanente y no podrán " +
        "ser recuperados por el desarrollador.",
    "6. Promoción de Servicios Propios" to
        "Esta aplicación puede mostrar espacios informativos o promocionales sobre servicios de ingeniería, " +
        "construcción y estructuras metálicas ofrecidas por el desarrollador. Estos enlaces son estáticos, no " +
        "utilizan cookies de rastreo publicitario y solo redirigen a canales externos de contacto si el usuario " +
        "decide pulsarlos voluntariamente."
)

@Composable
fun Raiz() {
    val ctx = LocalContext.current
    val fondoId = remember { ctx.resources.getIdentifier("fondo", "drawable", ctx.packageName) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (fondoId != 0) {
            Image(
                painter = painterResource(fondoId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.20f,
                modifier = Modifier.matchParentSize()
            )
        }
        PantallaPrincipal()
    }
}

@Composable
fun PantallaPrincipal() {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var tab by remember { mutableStateOf(0) }
    var lista by remember { mutableStateOf(listOf<Obligacion>()) }
    var detalleId by remember { mutableStateOf<Long?>(null) }
    var editorTipo by remember { mutableStateOf<String?>(null) }
    var editarId by remember { mutableStateOf<Long?>(null) }
    var borrarId by remember { mutableStateOf<Long?>(null) }
    var configTipo by remember { mutableStateOf<String?>(null) }
    var elegirAjustes by remember { mutableStateOf(false) }
    var textoCrash by remember { mutableStateOf<String?>(null) }
    var acercaVista by remember { mutableStateOf<String?>(null) }
    var respaldoMsg by remember { mutableStateOf<String?>(null) }

    fun recargar() {
        lista = try { Store.cargar(ctx) } catch (_: Exception) { listOf() }
    }

    fun guardar(nueva: List<Obligacion>) {
        try { Store.guardar(ctx, ArrayList(nueva)) } catch (_: Exception) { }
        lista = nueva
        ReminderWorker.ejecutarAhora(ctx)
        AlertScheduler.programarSiguiente(ctx, Config.SUNAT)
        AlertScheduler.programarSiguiente(ctx, Config.GENERAL)
    }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                recargar()
                textoCrash = try {
                    val f = File(ctx.filesDir, "crash.txt")
                    if (f.exists()) f.readText().take(800) else null
                } catch (_: Exception) { null }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(Unit) { recargar() }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !Notifier.permisoConcedido(ctx)) {
            pedirPermiso.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Respaldo de cumpleaños (solo GENERAL): exportar a un archivo y restaurarlo.
    val exportarRespaldo = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                val texto = generarRespaldo(lista)
                ctx.contentResolver.openOutputStream(uri)?.use { out -> out.write(texto.toByteArray(Charsets.UTF_8)) }
                    ?: throw Exception("no se pudo abrir el destino")
                respaldoMsg = "Respaldo guardado con ${lista.count { it.tipo == "GENERAL" }} cumpleaños. Guárdalo en un lugar seguro (Drive, WhatsApp, correo)."
            } catch (e: Exception) {
                respaldoMsg = "No se pudo guardar el respaldo: ${e.message ?: "error desconocido"}"
            }
        }
    }
    val importarRespaldo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val texto = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw Exception("no se pudo leer el archivo")
                val leidos = parsearRespaldo(texto)
                if (leidos.isEmpty()) {
                    respaldoMsg = "El archivo no contiene cumpleaños válidos."
                } else {
                    val claves = lista.filter { it.tipo == "GENERAL" }.map { claveCumple(it) }.toHashSet()
                    var siguiente = (lista.maxOfOrNull { it.id } ?: 0L) + 1L
                    val nuevos = ArrayList<Obligacion>()
                    for (o in leidos) {
                        val c = claveCumple(o)
                        if (c in claves) continue
                        claves.add(c)
                        nuevos.add(o.copy(id = siguiente))
                        siguiente += 1L
                    }
                    if (nuevos.isEmpty()) {
                        respaldoMsg = "Nada nuevo: los ${leidos.size} cumpleaños del archivo ya estaban guardados."
                    } else {
                        guardar(lista + nuevos)
                        respaldoMsg = "Listo: se agregaron ${nuevos.size} cumpleaños (${leidos.size - nuevos.size} ya existían)."
                    }
                }
            } catch (e: Exception) {
                respaldoMsg = "No se pudo restaurar el respaldo: ${e.message ?: "archivo inválido"}"
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (tab == 0 && detalleId == null) {
                FloatingActionButton(
                    onClick = { tab = 1 },
                    containerColor = ORO,
                    contentColor = Color(0xFF1A1400)
                ) { Icon(Icons.Filled.Add, contentDescription = "Agregar") }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "RecordaSUNAT",
                color = ORO,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("📋 Programadas") })
                Tab(selected = tab == 1, onClick = { tab = 1; detalleId = null }, text = { Text("➕ Agregar") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("ℹ️ Acerca de") })
            }
            when (tab) {
                0 -> {
                    val actual = detalleId?.let { id -> lista.firstOrNull { it.id == id } }
                    if (actual != null) {
                        VistaDetalle(
                            o = actual,
                            onVolver = { detalleId = null },
                            onDeclarar = {
                                val etiqueta = runCatching { Planner.etiquetaVigente(actual, LocalDate.now()) }.getOrNull()
                                guardar(lista.map { if (it.id == actual.id) it.copy(periodoDeclarado = etiqueta) else it })
                                try { Notifier.cancelar(ctx, actual.id) } catch (_: Exception) { }
                            },
                            onDeshacer = {
                                guardar(lista.map { if (it.id == actual.id) it.copy(periodoDeclarado = null) else it })
                            },
                            onEditar = { editarId = actual.id },
                            onEliminar = { borrarId = actual.id }
                        )
                    } else {
                        ListaProgramadas(lista, onAbrir = { detalleId = it }, onIrAgregar = { tab = 1 })
                    }
                }
                1 -> VistaAgregar(
                    totalCumple = lista.count { it.tipo == "GENERAL" },
                    onNuevoSunat = { editorTipo = Config.SUNAT },
                    onNuevoGeneral = { editorTipo = Config.GENERAL },
                    onAjustes = { elegirAjustes = true },
                    onExportar = {
                        if (lista.any { it.tipo == "GENERAL" }) {
                            exportarRespaldo.launch("respaldo_cumpleanos_${LocalDate.now()}.json")
                        } else {
                            respaldoMsg = "Todavía no tienes cumpleaños para respaldar."
                        }
                    },
                    onImportar = { importarRespaldo.launch(arrayOf("*/*")) }
                )
                else -> VistaAcerca(
                    vista = acercaVista,
                    onVolver = { acercaVista = null },
                    onAbrir = { acercaVista = it }
                )
            }
        }
    }

    // ---------- Diálogos ----------
    editorTipo?.let { tipo ->
        DialogoEditor(
            tipo = tipo,
            inicial = null,
            nuevoId = (lista.maxOfOrNull { it.id } ?: 0L) + 1L,
            onGuardar = { nuevo ->
                guardar(lista + nuevo)
                tab = 0
            },
            onCerrar = { editorTipo = null }
        )
    }
    editarId?.let { id ->
        val obj = lista.firstOrNull { it.id == id }
        if (obj != null) {
            DialogoEditor(
                tipo = obj.tipo,
                inicial = obj,
                nuevoId = 0L,
                onGuardar = { editado ->
                    guardar(lista.map { if (it.id == editado.id) editado else it })
                },
                onCerrar = { editarId = null }
            )
        }
    }
    borrarId?.let { id ->
        val obj = lista.firstOrNull { it.id == id }
        if (obj != null) {
            AlertDialog(
                onDismissRequest = { borrarId = null },
                title = { Text("¿Eliminar \"${obj.nombre}\"?") },
                text = { Text("Se borra para siempre. No afecta a los demás recordatorios.") },
                confirmButton = {
                    Button(onClick = {
                        guardar(lista.filterNot { it.id == id })
                        borrarId = null
                        detalleId = null
                    }) { Text("Eliminar") }
                },
                dismissButton = { TextButton(onClick = { borrarId = null }) { Text("Cancelar") } }
            )
        }
    }
    if (elegirAjustes) {
        AlertDialog(
            onDismissRequest = { elegirAjustes = false },
            title = { Text("Ajustes de alarma") },
            text = {
                Column {
                    Text("Cada tipo tiene su propio horario e intervalo:", fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { elegirAjustes = false; configTipo = Config.SUNAT }, modifier = Modifier.fillMaxWidth()) {
                        Text("SUNAT")
                    }
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = { elegirAjustes = false; configTipo = Config.GENERAL }, modifier = Modifier.fillMaxWidth()) {
                        Text("GENERAL")
                    }
                }
            },
            confirmButton = { },
            dismissButton = { TextButton(onClick = { elegirAjustes = false }) { Text("Cerrar") } }
        )
    }
    configTipo?.let { tipo -> DialogoConfig(tipo = tipo, onCerrar = { configTipo = null }) }
    respaldoMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { respaldoMsg = null },
            title = { Text("Respaldo de cumpleaños") },
            text = { Text(msg) },
            confirmButton = { Button(onClick = { respaldoMsg = null }) { Text("OK") } }
        )
    }
    textoCrash?.let { txt ->
        AlertDialog(
            onDismissRequest = { textoCrash = null },
            title = { Text("Registro de error") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 300.dp)) {
                    Text(txt, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    try { File(ctx.filesDir, "crash.txt").delete() } catch (_: Exception) { }
                    textoCrash = null
                }) { Text("Borrar registro") }
            },
            dismissButton = { TextButton(onClick = { textoCrash = null }) { Text("Cerrar") } }
        )
    }
}

@Composable
fun ListaProgramadas(lista: List<Obligacion>, onAbrir: (Long) -> Unit, onIrAgregar: () -> Unit) {
    val hoy = LocalDate.now()
    val sunat = lista.filter { it.tipo != "GENERAL" }.sortedBy { it.nombre.lowercase() }
    val general = lista.filter { it.tipo == "GENERAL" }.sortedBy { it.nombre.lowercase() }

    if (lista.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Aún no tienes recordatorios", fontSize = 17.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Toca el botón ➕ (o la pestaña Agregar) para crear el primero: el cronograma SUNAT del año o fechas propias como cumpleaños.",
                fontSize = 14.sp, color = Color(0xFFB9B2A2), textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Button(onClick = onIrAgregar) { Text("➕ Agregar uno ahora") }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        item { Cintillo("📅 MENSUAL SUNAT", sunat.size) }
        if (sunat.isEmpty()) {
            item { TextoVacio("Nada del cronograma SUNAT todavía.") }
        } else {
            items(sunat) { o -> FilaNombre(o, hoy, onAbrir) }
        }
        item { Spacer(Modifier.height(10.dp)); Cintillo("⭐ GENERAL", general.size) }
        if (general.isEmpty()) {
            item { TextoVacio("Nada de fechas propias todavía.") }
        } else {
            items(general) { o -> FilaNombre(o, hoy, onAbrir) }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun Cintillo(texto: String, total: Int) {
    Surface(color = Color(0xFF2B2312), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            "$texto  ($total)",
            color = ORO,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun TextoVacio(msg: String) {
    Text(msg, fontSize = 13.sp, color = Color(0xFF8D8779), modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp))
}

private fun colorEstado(o: Obligacion, hoy: LocalDate): Color {
    return try {
        val venc = Planner.vencimientoVigente(o, hoy)
        val etiqueta = Planner.etiquetaVigente(o, hoy)
        val atendido = o.periodoDeclarado != null && o.periodoDeclarado == etiqueta
        when {
            atendido -> Color(0xFF66BB6A)
            venc != null && Planner.tocaAvisar(o, hoy) -> ROJO_SUAVE
            venc != null -> Color(0xFFFFB74D)
            else -> Color(0xFF757575)
        }
    } catch (_: Exception) { Color(0xFF757575) }
}

@Composable
fun FilaNombre(o: Obligacion, hoy: LocalDate, onAbrir: (Long) -> Unit) {
    Surface(
        onClick = { onAbrir(o.id) },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Box(Modifier.size(10.dp).background(colorEstado(o, hoy), CircleShape))
            Spacer(Modifier.width(10.dp))
            Text(o.nombre.ifBlank { "(sin nombre)" }, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("›", color = ORO, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun VistaDetalle(
    o: Obligacion,
    onVolver: () -> Unit,
    onDeclarar: () -> Unit,
    onDeshacer: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val hoy = LocalDate.now()
    val venc = runCatching { Planner.vencimientoVigente(o, hoy) }.getOrNull()
    val etiqueta = runCatching { Planner.etiquetaVigente(o, hoy) }.getOrNull()
    val atendido = o.periodoDeclarado != null && o.periodoDeclarado == etiqueta
    val fechaTxt = if (venc != null) runCatching { Planner.fmt(venc) }.getOrNull() ?: venc.toString() else ""

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TextButton(onClick = onVolver) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = ORO)
            Spacer(Modifier.width(6.dp))
            Text("Volver a la lista", color = ORO)
        }
        Spacer(Modifier.height(4.dp))
        Text(o.nombre.ifBlank { "(sin nombre)" }, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(if (o.tipo == "GENERAL") "⭐ GENERAL" else "📅 MENSUAL SUNAT", color = ORO, fontSize = 13.sp)
        if (o.detalle.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(o.detalle, fontSize = 14.sp, color = Color(0xFFCFC8B8))
        }
        Spacer(Modifier.height(14.dp))

        val parEstado: Pair<String, Color> = when {
            atendido -> "AL DÍA — marcaste \"${o.periodoDeclarado}\" como declarado." to Color(0xFF66BB6A)
            venc != null && hoy > venc -> {
                val atraso = try { ChronoUnit.DAYS.between(venc, hoy) } catch (_: Exception) { 0L }
                "VENCIDO hace $atraso día(s) — venció el $fechaTxt." to ROJO_SUAVE
            }
            venc != null && hoy == venc -> "¡VENCE HOY! ($fechaTxt)." to Color(0xFFFFB74D)
            venc != null -> {
                val faltan = try { ChronoUnit.DAYS.between(hoy, venc) } catch (_: Exception) { 0L }
                "Vence el $fechaTxt (faltan $faltan día(s))." to Color(0xFFFFB74D)
            }
            else -> "Sin vencimiento aplicable ahora mismo." to Color(0xFF9E9E9E)
        }
        Surface(color = Color(0xFF241F16), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                parEstado.first,
                color = parEstado.second,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(12.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Te avisamos desde ${o.diasAviso} día(s) antes, con sonido, hasta que marques que ya lo hiciste.",
            fontSize = 12.sp, color = Color(0xFF9C968A)
        )
        Spacer(Modifier.height(18.dp))

        if (!atendido) {
            Button(onClick = onDeclarar, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(runCatching { Mensajes.accion(o) }.getOrNull() ?: "YA DECLARÉ ✓")
            }
        } else {
            OutlinedButton(onClick = onDeshacer, modifier = Modifier.fillMaxWidth()) {
                Text("Deshacer (volver a pendiente)")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEditar, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = ORO)
                Spacer(Modifier.width(4.dp))
                Text("Editar")
            }
            OutlinedButton(onClick = onEliminar, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = ROJO_SUAVE)
                Spacer(Modifier.width(4.dp))
                Text("Eliminar", color = ROJO_SUAVE)
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun VistaAgregar(
    totalCumple: Int,
    onNuevoSunat: () -> Unit,
    onNuevoGeneral: () -> Unit,
    onAjustes: () -> Unit,
    onExportar: () -> Unit,
    onImportar: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))
        Text("Elige el tipo de recordatorio", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ORO)
        Spacer(Modifier.height(12.dp))

        Surface(onClick = onNuevoSunat, color = Color(0xFF2B2312), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("📅 SUNAT", color = ORO, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Declaraciones mensuales (IGV, RENTA, etc.). Ingresa el cronograma del año: período → día de vencimiento.",
                    fontSize = 13.sp, color = Color(0xFFCFC8B8)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(onClick = onNuevoGeneral, color = Color(0xFF2B2312), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("⭐ GENERAL", color = ORO, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Fechas propias: cumpleaños, pagos de servicios, trámites, etc.",
                    fontSize = 13.sp, color = Color(0xFFCFC8B8)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("💾 Respaldo de cumpleaños", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ORO)
        Spacer(Modifier.height(4.dp))
        Text(
            "Guarda tus cumpleaños en un archivo para no perderlos si cambias de teléfono, y recupéralos cuando quieras.",
            fontSize = 13.sp, color = Color(0xFF9C968A), textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExportar, modifier = Modifier.weight(1f)) {
                Text("📁 Exportar")
            }
            OutlinedButton(onClick = onImportar, modifier = Modifier.weight(1f)) {
                Text("📂 Restaurar")
            }
        }
        Text(
            if (totalCumple == 1) "Tienes 1 cumpleaño guardado." else "Tienes $totalCumple cumpleaños guardados.",
            fontSize = 12.sp, color = Color(0xFF9C968A), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )

        Spacer(Modifier.height(24.dp))
        Text("Alarmas", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ORO)
        Spacer(Modifier.height(4.dp))
        Text(
            "Cada tipo tiene su propio horario e intervalo de repicado.",
            fontSize = 13.sp, color = Color(0xFF9C968A), textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onAjustes, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Settings, contentDescription = null, tint = ORO)
            Spacer(Modifier.width(6.dp))
            Text("Ajustes de alarma")
        }
        Spacer(Modifier.height(80.dp))
    }
}

// ==================== PESTAÑA ACERCA DE ====================

@Composable
fun VistaAcerca(vista: String?, onVolver: () -> Unit, onAbrir: (String) -> Unit) {
    when (vista) {
        "DEV" -> PantallaDesarrollador(onVolver)
        "VER" -> PantallaVersion(onVolver)
        "POL" -> PantallaPrivacidad(onVolver)
        else -> MenuAcerca(onAbrir)
    }
}

@Composable
fun MenuAcerca(onAbrir: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))
        Text("Acerca de la aplicación", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ORO)
        Spacer(Modifier.height(12.dp))

        Surface(onClick = { onAbrir("DEV") }, color = Color(0xFF2B2312), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("🏗 INGES BUILDERS", color = ORO, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Quiénes desarrollan esta app: la empresa, sus servicios y el correo de contacto.",
                    fontSize = 13.sp, color = Color(0xFFCFC8B8)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(onClick = { onAbrir("VER") }, color = Color(0xFF2B2312), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("📌 Version", color = ORO, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Version 3.0 - 2026 — datos del autor y la empresa.",
                    fontSize = 13.sp, color = Color(0xFFCFC8B8)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(onClick = { onAbrir("POL") }, color = Color(0xFF2B2312), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("🔒 Acerca de", color = ORO, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Política de Privacidad: cómo esta app guarda y protege tu información.",
                    fontSize = 13.sp, color = Color(0xFFCFC8B8)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("RecordaSUNAT", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ORO)
        Text(
            "Desarrollado por INGES BUILDERS SCRL. — Cajamarca, Perú",
            fontSize = 12.sp, color = Color(0xFF9C968A), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun PantallaDesarrollador(onVolver: () -> Unit) {
    val ctx = LocalContext.current
    var msgCorreo by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TextButton(onClick = onVolver) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = ORO)
            Spacer(Modifier.width(6.dp))
            Text("Volver", color = ORO)
        }
        Text("Desarrollado por", fontSize = 13.sp, color = Color(0xFF9C968A))
        Text("INGES BUILDERS", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = ORO)
        Spacer(Modifier.height(10.dp))
        Text(
            "Somos una empresa especializada en construcción, consultoría de obras y soluciones en estructuras metálicas. Nuestro equipo multidisciplinario de especialistas te acompaña desde el cálculo y diseño estructural hasta la fabricación y ejecución en campo, asegurando eficiencia y garantía en cada proyecto.",
            fontSize = 14.sp, color = Color(0xFFE3DCCE), lineHeight = 20.sp
        )
        Spacer(Modifier.height(12.dp))
        Surface(color = Color(0xFF241F16), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Especialidad principal", color = ORO, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Diseño, fabricación y montaje de estructuras metálicas (naves industriales, coberturas, puentes, edificaciones).",
                    fontSize = 13.sp, color = Color(0xFFCFC8B8), lineHeight = 18.sp
                )
                Spacer(Modifier.height(10.dp))
                Text("Servicios complementarios", color = ORO, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Ejecución de obras civiles, elaboración de expedientes técnicos y supervisión/consultoría de ingeniería.",
                    fontSize = 13.sp, color = Color(0xFFCFC8B8), lineHeight = 18.sp
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                try {
                    ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:ingesbuilders@gmail.com")))
                    msgCorreo = null
                } catch (_: Exception) {
                    msgCorreo = "No encontré una app de correo instalada. Escríbenos a: ingesbuilders@gmail.com"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Email, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Escribir al Correo")
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "ingesbuilders@gmail.com",
            fontSize = 13.sp, color = ORO, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        msgCorreo?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = ROJO_SUAVE, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun PantallaVersion(onVolver: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TextButton(onClick = onVolver) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = ORO)
            Spacer(Modifier.width(6.dp))
            Text("Volver", color = ORO)
        }
        Text("Version", fontSize = 13.sp, color = Color(0xFF9C968A))
        Text("3.0 - 2026", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ORO)
        Spacer(Modifier.height(16.dp))
        Surface(color = Color(0xFF241F16), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LineaVersion("Version", "3.0 - 2026")
                LineaVersion("Autor", "Hugo Cerquin M")
                LineaVersion("Empresa", "INGES BUILDERS SCRL.")
                LineaVersion("Ciudad", "CAJAMARCA, PERU")
                LineaVersion("App", "RecordaSUNAT")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "COPYRIGHT © 2026 — INGES BUILDERS SCRL. Todos los derechos reservados.",
            fontSize = 12.sp, color = Color(0xFF9C968A), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun LineaVersion(clave: String, valor: String) {
    Row {
        Text("$clave:", color = Color(0xFF9C968A), fontSize = 14.sp, modifier = Modifier.width(90.dp))
        Text(valor, color = Color(0xFFEDE6D6), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PantallaPrivacidad(onVolver: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TextButton(onClick = onVolver) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = ORO)
            Spacer(Modifier.width(6.dp))
            Text("Volver", color = ORO)
        }
        Text("Acerca de", fontSize = 13.sp, color = Color(0xFF9C968A))
        Text("Política de Privacidad", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ORO)
        Spacer(Modifier.height(10.dp))
        for ((titulo, cuerpo) in POLITICA) {
            Surface(color = Color(0xFF241F16), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(titulo, color = ORO, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(cuerpo, fontSize = 13.sp, color = Color(0xFFCFC8B8), lineHeight = 19.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(70.dp))
    }
}

@Composable
fun DialogoEditor(
    tipo: String,
    inicial: Obligacion?,
    nuevoId: Long,
    onGuardar: (Obligacion) -> Unit,
    onCerrar: () -> Unit
) {
    val esSunat = tipo != "GENERAL"
    val clave = inicial?.id ?: -1L
    var nombre by remember(clave) { mutableStateOf(inicial?.nombre ?: "") }
    var detalle by remember(clave) { mutableStateOf(inicial?.detalle ?: "") }
    var avisoTxt by remember(clave) { mutableStateOf((inicial?.diasAviso ?: 5).toString()) }
    val diasMes = remember(clave) {
        mutableStateListOf<String>().apply {
            for (m in 1..12) add(try { inicial?.vencimientos?.get(m)?.toString() ?: "" } catch (_: Exception) { "" })
        }
    }
    var diaTxt by remember(clave) { mutableStateOf((inicial?.diaMes ?: 1).toString()) }
    var mesTxt by remember(clave) { mutableStateOf((inicial?.mesAnual ?: 1).toString()) }
    var repite by remember(clave) { mutableStateOf(inicial?.repetirAnual ?: true) }
    var anioTxt by remember(clave) { mutableStateOf((inicial?.anioEspecifico ?: LocalDate.now().year).toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    BackHandler { onCerrar() }

    fun confirmar() {
        val nom = nombre.trim()
        if (nom.isEmpty()) {
            error = "Ponle un nombre al recordatorio."
            return
        }
        val aviso = avisoTxt.trim().toIntOrNull()?.coerceIn(1, 30) ?: 5
        var ven: Map<Int, Int> = emptyMap()
        var dia = 0; var mes = 0
        var anio: Int? = null
        var repiteAnual = false
        if (esSunat) {
            val mapa = linkedMapOf<Int, Int>()
            for (m in 1..12) {
                val d = diasMes[m - 1].trim().toIntOrNull()
                if (d != null && d in 1..31) mapa[m] = d
            }
            ven = mapa
        } else {
            dia = (diaTxt.trim().toIntOrNull() ?: 0).coerceIn(1, 31)
            mes = (mesTxt.trim().toIntOrNull() ?: 0).coerceIn(1, 12)
            repiteAnual = repite
            if (!repite) anio = anioTxt.trim().toIntOrNull()
            if (diaTxt.trim().isEmpty() || mesTxt.trim().isEmpty()) {
                error = "Revisa el día y el mes."
                return
            }
        }
        val obj = Obligacion(
            id = inicial?.id ?: nuevoId,
            nombre = nom,
            detalle = detalle.trim(),
            tipo = tipo,
            vencimientos = ven,
            diaMes = dia,
            mesAnual = mes,
            repetirAnual = repiteAnual,
            anioEspecifico = anio,
            diasAviso = aviso,
            periodoDeclarado = inicial?.periodoDeclarado
        )
        onGuardar(obj)
        onCerrar()
    }

    // Diálogo propio en vez de AlertDialog: el desplazamiento siempre funciona
    // y los 12 meses entran de una sola vista en una grilla de 3 filas x 4 columnas.
    Box(
        Modifier.fillMaxSize()
            .background(Color(0x99000000))
            .pointerInput(Unit) { detectTapGestures { } }
            .systemBarsPadding()
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF1C1913),
            contentColor = Color(0xFFEDE6D6),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (inicial == null) "Nuevo ($tipo)" else "Editar: ${inicial.nombre.ifBlank { "(sin nombre)" }}",
                    color = ORO, fontWeight = FontWeight.Bold, fontSize = 17.sp
                )
                Spacer(Modifier.height(10.dp))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre") },
                        placeholder = { Text(if (esSunat) "ej: IGV MENSUAL, RENTA, ETC" else "ej: Cumpleaños, Pago de Servicios, etc") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = detalle,
                        onValueChange = { detalle = it },
                        label = { Text("Detalle (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))

                    if (esSunat) {
                        Text(
                            "Escribe el DÍA en que vences cada período (se declara el mes anterior: ej: AGO con 12 → vence el 12 de setiembre). Deja vacío lo que no aplique.",
                            fontSize = 12.sp, color = Color(0xFF9C968A)
                        )
                        Spacer(Modifier.height(8.dp))
                        for (fila in 0 until 3) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (k in 0 until 4) {
                                    val m = fila * 4 + k + 1
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            MESES_ANIO[m - 1],
                                            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ORO,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = diasMes[m - 1],
                                            onValueChange = { v -> if (v.length <= 2 && v.all { it.isDigit() }) diasMes[m - 1] = v },
                                            placeholder = { Text("—", fontSize = 12.sp, color = Color(0xFF6B665C), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, textAlign = TextAlign.Center),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = diaTxt,
                                onValueChange = { v -> if (v.length <= 2 && v.all { it.isDigit() }) diaTxt = v },
                                label = { Text("Día") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = mesTxt,
                                onValueChange = { v -> if (v.length <= 2 && v.all { it.isDigit() }) mesTxt = v },
                                label = { Text("Mes (1-12)") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = repite, onCheckedChange = { repite = it })
                            Text("Se repite cada año", fontSize = 14.sp)
                        }
                        if (!repite) {
                            OutlinedTextField(
                                value = anioTxt,
                                onValueChange = { v -> if (v.length <= 4 && v.all { it.isDigit() }) anioTxt = v },
                                label = { Text("Año (ej: 2026)") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = avisoTxt,
                        onValueChange = { v -> if (v.length <= 2 && v.all { it.isDigit() }) avisoTxt = v },
                        label = { Text("Avisar con (días de anticipación)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = ROJO_SUAVE, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onCerrar, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(onClick = { confirmar() }, modifier = Modifier.weight(1f)) { Text("Guardar") }
                }
            }
        }
    }
}

@Composable
fun DialogoConfig(tipo: String, onCerrar: () -> Unit) {
    val ctx = LocalContext.current
    val actuales = remember(tipo) { Config.ajustes(ctx, tipo) }
    var iniTxt by remember { mutableStateOf(actuales.inicio.toString()) }
    var finTxt by remember { mutableStateOf(actuales.fin.toString()) }
    var itvTxt by remember { mutableStateOf(actuales.intervaloMin.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    BackHandler { onCerrar() }

    fun confirmar() {
        val ini = validarHora(iniTxt)
        val fin = validarHora(finTxt)
        val itv = itvTxt.trim().toIntOrNull()?.coerceIn(15, 1440)
        if (ini == null || fin == null || itv == null) {
            error = "Revisa las horas (ej: 08:00) y el intervalo (número de minutos)."
            return
        }
        Config.guardar(ctx, tipo, Config.Ajustes(ini, fin, itv))
        AlertScheduler.programarSiguiente(ctx, tipo)
        onCerrar()
    }

    Box(
        Modifier.fillMaxSize()
            .background(Color(0x99000000))
            .pointerInput(Unit) { detectTapGestures { } }
            .systemBarsPadding()
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF1C1913),
            contentColor = Color(0xFFEDE6D6),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Alarma — $tipo", color = ORO, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    Text(
                        "Mientras haya algo pendiente, la alarma SUENA cada cierto tiempo entre estas horas. Cambias solo el bloque de $tipo; el otro tipo conserva lo suyo.",
                        fontSize = 12.sp, color = Color(0xFF9C968A)
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = iniTxt, onValueChange = { iniTxt = it },
                        label = { Text("Hora de inicio (ej: 08:00)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = finTxt, onValueChange = { finTxt = it },
                        label = { Text("Hora de fin (ej: 22:00)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = itvTxt,
                        onValueChange = { v -> if (v.length <= 4 && v.all { it.isDigit() }) itvTxt = v },
                        label = { Text("Intervalo en minutos (ej: 30 o 120)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Mínimo 15 minutos. Funciona con cualquier intervalo, también de 1 hora o más.",
                        fontSize = 12.sp, color = Color(0xFF9C968A)
                    )
                    error?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = ROJO_SUAVE, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = {
                        try {
                            if (Build.VERSION.SDK_INT >= 31) {
                                ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
                            } else {
                                error = "Tu Android ya permite alarmas exactas."
                            }
                        } catch (_: Exception) { error = "No se pudo abrir el ajuste." }
                    }) { Text("Permitir alarmas exactas (recomendado)") }
                    TextButton(onClick = {
                        try { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                        catch (_: Exception) { error = "No se pudo abrir el ajuste." }
                    }) { Text("Quitar optimización de batería") }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onCerrar, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(onClick = { confirmar() }, modifier = Modifier.weight(1f)) { Text("Guardar") }
                }
            }
        }
    }
}

private fun validarHora(s: String): LocalTime? = try {
    val p = s.trim().split(":")
    if (p.size > 2) null
    else {
        val h = p[0].trim().toIntOrNull() ?: return null
        val m = if (p.size == 2) (p[1].trim().toIntOrNull() ?: return null) else 0
        LocalTime.of(h.coerceIn(0, 23), m.coerceIn(0, 59))
    }
} catch (_: Exception) { null }

// ==================== RESPALDO DE CUMPLEAÑOS (solo GENERAL) ====================

/** Clave anti-duplicados: mismo nombre + misma fecha = mismo cumpleaño. */
private fun claveCumple(o: Obligacion): String {
    return o.nombre.trim().lowercase() + "|" + o.diaMes + "|" + o.mesAnual + "|" + (o.anioEspecifico ?: 0)
}

/** Arma el archivo de respaldo SOLO con los recordatorios de tipo GENERAL (cumpleaños). */
private fun generarRespaldo(lista: List<Obligacion>): String {
    val cumple = lista.filter { it.tipo == "GENERAL" }.sortedBy { it.nombre.lowercase() }
    val arr = JSONArray()
    for (o in cumple) {
        val ob = JSONObject()
        ob.put("nombre", o.nombre)
        ob.put("detalle", o.detalle)
        ob.put("diaMes", o.diaMes)
        ob.put("mesAnual", o.mesAnual)
        ob.put("repetirAnual", o.repetirAnual)
        if (o.anioEspecifico != null) ob.put("anio", o.anioEspecifico)
        ob.put("diasAviso", o.diasAviso)
        if (o.periodoDeclarado != null) ob.put("periodoDeclarado", o.periodoDeclarado)
        arr.put(ob)
    }
    val raiz = JSONObject()
    raiz.put("app", "RecordaSUNAT")
    raiz.put("tipoRespaldo", "CUMPLEANOS")
    raiz.put("fecha", LocalDate.now().toString())
    raiz.put("cantidad", cumple.size)
    raiz.put("cumpleanos", arr)
    return raiz.toString(2)
}

/** Lee el archivo de respaldo y devuelve la lista de cumpleaños encontrados. */
private fun parsearRespaldo(texto: String): List<Obligacion> {
    val raiz = JSONObject(texto)
    val arr = raiz.optJSONArray("cumpleanos") ?: return emptyList()
    val leidos = ArrayList<Obligacion>()
    for (i in 0 until arr.length()) {
        val ob = arr.getJSONObject(i)
        val nombre = if (ob.isNull("nombre")) "" else ob.optString("nombre").trim()
        if (nombre.isEmpty()) continue
        val detalle = if (ob.isNull("detalle")) "" else ob.optString("detalle")
        leidos.add(
            Obligacion(
                id = 0L,
                nombre = nombre,
                detalle = detalle,
                tipo = "GENERAL",
                vencimientos = emptyMap(),
                diaMes = ob.optInt("diaMes", 1).coerceIn(1, 31),
                mesAnual = ob.optInt("mesAnual", 1).coerceIn(1, 12),
                repetirAnual = ob.optBoolean("repetirAnual", true),
                anioEspecifico = if (ob.has("anio") && !ob.isNull("anio")) ob.optInt("anio") else null,
                diasAviso = ob.optInt("diasAviso", 5).coerceIn(1, 30),
                periodoDeclarado = if (ob.has("periodoDeclarado") && !ob.isNull("periodoDeclarado")) ob.optString("periodoDeclarado") else null
            )
        )
    }
    return leidos
}