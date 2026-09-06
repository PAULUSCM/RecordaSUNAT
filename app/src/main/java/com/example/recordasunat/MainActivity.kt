package com.example.recordasunat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.time.LocalDate

private val MESES = listOf("Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")

class MainActivity : ComponentActivity() {

    private val pedirPermiso =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifier.crearCanales(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED)
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)

        ReminderWorker.programar(this)
        ReminderWorker.ejecutarAhora(this)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Raiz() } }
    }
}

@Composable
fun Raiz() {
    val ctx = LocalContext.current
    val fondoId = remember { ctx.resources.getIdentifier("fondo", "drawable", ctx.packageName) }
    Box(Modifier.fillMaxSize()) {
        if (fondoId != 0) {
            Image(painterResource(fondoId), contentDescription = null,
                contentScale = ContentScale.Crop, alpha = 0.20f,
                modifier = Modifier.fillMaxSize())
        }
        PantallaPrincipal()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lista by remember { mutableStateOf(Store.cargar(context)) }
    var mostrarSelector by remember { mutableStateOf(false) }
    var nuevoTipo by remember { mutableStateOf<String?>(null) }
    var editando by remember { mutableStateOf<Obligacion?>(null) }
    var eliminando by remember { mutableStateOf<Obligacion?>(null) }
    var configurando by remember { mutableStateOf(false) }
    val hoy = LocalDate.now()

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, ev ->
            if (ev == Lifecycle.Event.ON_RESUME) lista = Store.cargar(context)
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    fun guardar(nueva: List<Obligacion>) {
        val l = nueva.toMutableList()
        lista = l
        Store.guardar(context, l)
        ReminderWorker.ejecutarAhora(context)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Mis recordatorios") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = { configurando = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes de alertas")
                    }
                })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarSelector = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
    ) { padding ->
        if (lista.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Toca + para agregar un recordatorio.", Modifier.padding(24.dp))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp)) {
                items(lista, key = { it.id }) { o ->
                    TarjetaObligacion(
                        o = o, hoy = hoy,
                        onAtender = { guardar(lista.map {
                            if (it.id == o.id) it.copy(periodoDeclarado = Planner.etiquetaVigente(it, hoy)) else it }) },
                        onDeshacer = { guardar(lista.map {
                            if (it.id == o.id) it.copy(periodoDeclarado = null) else it }) },
                        onEditar = { editando = o },
                        onEliminar = { eliminando = o }
                    )
                }
            }
        }
    }

    if (mostrarSelector) DialogoTipo(
        onElegir = { mostrarSelector = false; nuevoTipo = it },
        onCancelar = { mostrarSelector = false })

    nuevoTipo?.let { t ->
        DialogoEditor(tipo = t, inicial = null,
            onGuardar = { nueva -> guardar(lista + nueva.copy(id = System.currentTimeMillis())); nuevoTipo = null },
            onCancelar = { nuevoTipo = null })
    }

    editando?.let { actual ->
        DialogoEditor(tipo = actual.tipo, inicial = actual,
            onGuardar = { nueva -> guardar(lista.map {
                if (it.id == actual.id) nueva.copy(id = actual.id) else it }); editando = null },
            onCancelar = { editando = null })
    }

    eliminando?.let { victima ->
        AlertDialog(
            onDismissRequest = { eliminando = null },
            title = { Text("¿Eliminar recordatorio?") },
            text = { Text("Se eliminará \"${victima.nombre}\" y todos sus avisos. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    Notifier.cancelar(context, victima.id)
                    guardar(lista.filter { it.id != victima.id })
                    eliminando = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { eliminando = null }) { Text("Cancelar") } })
    }

    if (configurando) DialogoConfig(onCerrar = { configurando = false })
}

@Composable
fun TarjetaObligacion(o: Obligacion, hoy: LocalDate,
                      onAtender: () -> Unit, onDeshacer: () -> Unit,
                      onEditar: () -> Unit, onEliminar: () -> Unit) {
    val est = Planner.estadoDe(o, hoy)
    val esGeneral = o.tipo == "GENERAL"
    val (color, textoEstado) = when (est) {
        is Planner.Estado.Atendido -> Color(0xFF2E7D32) to
            if (esGeneral) "✅ Atendido (ocurrencia ${est.etiqueta})"
            else "✅ Declarado (periodo ${est.etiqueta})"
        is Planner.Estado.Vencido -> Color(0xFFC62828) to
            if (esGeneral) "🔴 ¡La fecha ya pasó! Era el ${Planner.fmt(est.vencimiento)} (hace ${est.diasAtraso} día(s))"
            else "🔴 ¡VENCIDO hace ${est.diasAtraso} día(s)! Venció el ${Planner.fmt(est.vencimiento)}"
        Planner.Estado.VenceHoy -> Color(0xFFC62828) to "🔴 ¡VENCE HOY!"
        is Planner.Estado.PorVencer -> Color(0xFFEF6C00) to
            "⏰ ${if (esGeneral) "Falta(n)" else "Vence en"} ${est.faltan} día(s) (${Planner.fmt(est.vencimiento)})"
        Planner.Estado.NoAplica -> Color(0xFF9E9E9E) to "Sin vencimiento este mes"
    }

    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(o.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(if (esGeneral) "GENERAL" else "MENSUAL SUNAT",
                        style = MaterialTheme.typography.labelSmall, color = Color(0xFFD4AF37))
                    if (o.detalle.isNotBlank()) Text(o.detalle, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onEditar) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
                IconButton(onClick = onEliminar) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
            }
            Text(textoEstado, color = color, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            when (est) {
                is Planner.Estado.Atendido -> TextButton(onClick = onDeshacer) { Text("Deshacer") }
                Planner.Estado.NoAplica -> {}
                else -> Button(onClick = onAtender) {
                    Text(if (esGeneral) "Ya lo hice" else "Ya declaré este mes") }
            }
        }
    }
}

@Composable
fun DialogoTipo(onElegir: (String) -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nuevo recordatorio") },
        text = {
            Column {
                Button(onClick = { onElegir("SUNAT") }, modifier = Modifier.fillMaxWidth()) {
                    Text("📅 Mensual tipo SUNAT") }
                Spacer(Modifier.height(8.dp))
                Text("Cronograma por periodo (IGV, planillas, etc.). Avisa cada mes hasta que marques «Ya declaré».",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onElegir("GENERAL") }, modifier = Modifier.fillMaxWidth()) {
                    Text("⭐ General") }
                Spacer(Modifier.height(8.dp))
                Text("Cumpleaños, reuniones, pagos puntuales... por fecha, con repetición anual opcional. Mismo sistema de alertas.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } })
}

@Composable
fun DialogoEditor(tipo: String, inicial: Obligacion?, onGuardar: (Obligacion) -> Unit, onCancelar: () -> Unit) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var detalle by remember { mutableStateOf(inicial?.detalle ?: "") }
    var diasAviso by remember { mutableStateOf((inicial?.diasAviso ?: 5).toString()) }
    var error by remember { mutableStateOf(false) }
    var dias by remember { mutableStateOf((1..12).associateWith { inicial?.vencimientos?.get(it)?.toString() ?: "" }) }
    var diaMes by remember { mutableStateOf((inicial?.diaMes ?: 21).toString()) }
    var mesAnual by remember { mutableStateOf((inicial?.mesAnual ?: 7).toString()) }
    var repetir by remember { mutableStateOf(inicial?.repetirAnual ?: true) }
    var anio by remember { mutableStateOf(inicial?.anioEspecifico?.toString() ?: LocalDate.now().year.toString()) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (inicial == null)
            if (tipo == "GENERAL") "Nuevo recordatorio general" else "Nueva obligación SUNAT"
            else "Editar recordatorio") },
        confirmButton = {
            TextButton(onClick = {
                if (tipo == "SUNAT") {
                    val mapa = dias.mapNotNull { (mes, txt) ->
                        txt.trim().toIntOrNull()?.let { mes to it.coerceIn(1, 31) }
                    }.toMap()
                    if (nombre.isNotBlank() && mapa.isNotEmpty())
                        onGuardar(Obligacion(nombre = nombre.trim(), detalle = detalle.trim(), tipo = "SUNAT",
                            vencimientos = mapa, diasAviso = diasAviso.trim().toIntOrNull()?.coerceIn(1, 30) ?: 5))
                    else error = true
                } else {
                    val d = diaMes.trim().toIntOrNull()
                    val m = mesAnual.trim().toIntOrNull()
                    val a = if (repetir) LocalDate.now().year else anio.trim().toIntOrNull()
                    if (nombre.isNotBlank() && d != null && d in 1..31 && m != null && m in 1..12 &&
                        (repetir || (a != null && a >= 2000)))
                        onGuardar(Obligacion(nombre = nombre.trim(), detalle = detalle.trim(), tipo = "GENERAL",
                            diaMes = d, mesAnual = m, repetirAnual = repetir,
                            anioEspecifico = if (repetir) null else a,
                            diasAviso = diasAviso.trim().toIntOrNull()?.coerceIn(1, 30) ?: 5))
                    else error = true
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (error) Text("Revisa: nombre obligatorio y fechas válidas.",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(nombre, { nombre = it },
                    label = { Text("Nombre (ej: IGV mensual, Cumpleaños de Ana)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(detalle, { detalle = it },
                    label = { Text("Detalle (opcional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(diasAviso, { diasAviso = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("¿Cuántos días antes empezar a avisar?") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                if (tipo == "SUNAT") {
                    Text("Día de vencimiento por PERIODO (el vencimiento cae el mes siguiente; vacío = ese periodo no aplica):",
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    MESES.chunked(3).forEachIndexed { i, fila ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            fila.forEachIndexed { j, mes ->
                                val mesNum = i * 3 + j + 1
                                OutlinedTextField(
                                    value = dias[mesNum] ?: "",
                                    onValueChange = { v -> dias = dias + (mesNum to v.filter { it.isDigit() }.take(2)) },
                                    label = { Text(mes) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f))
                            }
                            if (fila.size < 3) Spacer(Modifier.weight((3 - fila.size).toFloat()))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { (1..12).forEach { m -> dias = dias + (m to "12") } }) {
                        Text("Cargar plantilla IGV (día 12) — ajusta a tu dígito RUC") }
                    Text("⚠ El cronograma oficial de SUNAT cambia cada año y según el último dígito del RUC. Verifícalo.",
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(diaMes, { diaMes = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Día") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f))
                        OutlinedTextField(mesAnual, { mesAnual = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Mes (1-12)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = repetir, onCheckedChange = { repetir = it })
                        Text("Se repite todos los años (cumpleaños)")
                    }
                    if (!repetir) {
                        OutlinedTextField(anio, { anio = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Año (ej. 2026)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        })
}

@Composable
fun DialogoConfig(onCerrar: () -> Unit) {
    val context = LocalContext.current
    val actual = Config.cargar(context)
    var inicio by remember { mutableStateOf("%02d:%02d".format(actual.inicio.hour, actual.inicio.minute)) }
    var fin by remember { mutableStateOf("%02d:%02d".format(actual.fin.hour, actual.fin.minute)) }
    var intervalo by remember { mutableStateOf(actual.intervaloMin.toString()) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Ajustes de alertas sonoras") },
        text = {
            Column {
                Text("Mientras un recordatorio siga pendiente, sonará dentro de este horario, repitiéndose cada cierto tiempo.",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(inicio, { inicio = it }, label = { Text("Inicio (HH:MM)") },
                        singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(fin, { fin = it }, label = { Text("Fin (HH:MM)") },
                        singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(intervalo, { intervalo = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("Repetir cada (minutos, mín. 15)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text("Ej.: inicio 08:00, fin 22:00, cada 120 min → suena a las 8:00, 10:00, 12:00... hasta las 22:00.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                Config.guardar(context, inicio, fin, intervalo.trim().toIntOrNull() ?: 120)
                onCerrar()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } })
}