package com.example.recordasunat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
        setContent { MaterialTheme { PantallaPrincipal() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal() {
    val context = LocalContext.current
    var lista by remember { mutableStateOf(Store.cargar(context)) }
    var editando by remember { mutableStateOf<Obligacion?>(null) }
    var creando by remember { mutableStateOf(false) }
    val hoy = LocalDate.now()

    fun guardar(l: MutableList<Obligacion>) {
        lista = l
        Store.guardar(context, l)
        ReminderWorker.ejecutarAhora(context)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Recordatorios de declaraciones") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { creando = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
    ) { padding ->
        if (lista.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Toca + para agregar una obligación\ny su cronograma mensual.", Modifier.padding(24.dp))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp)) {
                items(lista, key = { it.id }) { o ->
                    TarjetaObligacion(
                        o = o, hoy = hoy,
                        onDeclarar = { guardar(lista.apply {
                            val i = indexOfFirst { it.id == o.id }
                            this[i] = this[i].copy(periodoDeclarado = Planner.periodoActual(hoy)) }) },
                        onDeshacer = { guardar(lista.apply {
                            val i = indexOfFirst { it.id == o.id }
                            this[i] = this[i].copy(periodoDeclarado = null) }) },
                        onEditar = { editando = o },
                        onEliminar = {
                            Notifier.cancelar(context, o.id)
                            guardar(lista.apply { removeAll { it.id == o.id } })
                        }
                    )
                }
            }
        }
    }

    if (creando) DialogoEditor(null,
        onGuardar = { guardar(lista.apply { add(it.copy(id = System.currentTimeMillis())) }); creando = false },
        onCancelar = { creando = false })

    editando?.let { actual ->
        DialogoEditor(actual,
            onGuardar = { guardar(lista.apply {
                val i = indexOfFirst { it.id == actual.id }
                this[i] = it.copy(id = actual.id) }); editando = null },
            onCancelar = { editando = null })
    }
}

@Composable
fun TarjetaObligacion(o: Obligacion, hoy: LocalDate,
                      onDeclarar: () -> Unit, onDeshacer: () -> Unit,
                      onEditar: () -> Unit, onEliminar: () -> Unit) {
    val est = Planner.estadoDe(o, hoy)
    val (color, textoEstado) = when (est) {
        is Planner.Estado.Declarado -> Color(0xFF2E7D32) to "✅ Declarado (periodo ${est.periodo})"
        is Planner.Estado.Vencido   -> Color(0xFFC62828) to "🔴 ¡VENCIDO hace ${est.diasAtraso} día(s)! Venció el ${Planner.fmt(est.vencimiento)}"
        Planner.Estado.VenceHoy     -> Color(0xFFC62828) to "🔴 ¡VENCE HOY!"
        is Planner.Estado.PorVencer -> Color(0xFFEF6C00) to "⏰ Vence en ${est.faltan} día(s) (${Planner.fmt(est.vencimiento)})"
        Planner.Estado.NoAplica     -> Color(0xFF757575) to "Sin vencimiento este mes"
    }

    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(o.nombre, style = MaterialTheme.typography.titleMedium)
                    if (o.detalle.isNotBlank()) Text(o.detalle, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onEditar) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
                IconButton(onClick = onEliminar) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
            }
            Text(textoEstado, color = color, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            when (est) {
                is Planner.Estado.Declarado -> TextButton(onClick = onDeshacer) { Text("Deshacer") }
                Planner.Estado.NoAplica -> {}
                else -> Button(onClick = onDeclarar) { Text("Ya declaré este mes") }
            }
        }
    }
}

@Composable
fun DialogoEditor(inicial: Obligacion?, onGuardar: (Obligacion) -> Unit, onCancelar: () -> Unit) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var detalle by remember { mutableStateOf(inicial?.detalle ?: "") }
    var diasAviso by remember { mutableStateOf((inicial?.diasAviso ?: 5).toString()) }
    var error by remember { mutableStateOf(false) }
    var dias by remember {
        mutableStateOf((1..12).associateWith { inicial?.vencimientos?.get(it)?.toString() ?: "" })
    }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (inicial == null) "Nueva obligación" else "Editar obligación") },
        confirmButton = {
            TextButton(onClick = {
                val mapa = dias.mapNotNull { (mes, txt) ->
                    txt.trim().toIntOrNull()?.let { mes to it.coerceIn(1, 31) }
                }.toMap()
                if (nombre.isNotBlank() && mapa.isNotEmpty())
                    onGuardar(Obligacion(nombre = nombre.trim(), detalle = detalle.trim(),
                        vencimientos = mapa, diasAviso = diasAviso.trim().toIntOrNull() ?: 5))
                else error = true
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (error) Text("Pon un nombre y al menos un día de vencimiento.",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(nombre, { nombre = it },
                    label = { Text("Nombre (ej: IGV mensual)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(detalle, { detalle = it },
                    label = { Text("Detalle (opcional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(diasAviso, { diasAviso = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("¿Cuántos días antes avisar?") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Día de vencimiento por mes (vacío = ese mes no aplica):",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                MESES.chunked(3).forEachIndexed { i, fila ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        fila.forEachIndexed { j, mes ->
                            val mesNum = i * 3 + j + 1
                            OutlinedTextField(
                                value = dias[mesNum] ?: "",
                                onValueChange = { v ->
                                    dias = dias + (mesNum to v.filter { it.isDigit() }.take(2))
                                },
                                label = { Text(mes) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f))
                        }
                        if (fila.size < 3) Spacer(Modifier.weight((3 - fila.size).toFloat()))
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    listOf(12,13,13,12,13,12,13,12,13,13,12,12).forEachIndexed { idx, d ->
                        dias = dias + (idx + 1 to d.toString())
                    }
                }) { Text("Cargar cronograma IGV típico") }
                Text("⚠ Verifica siempre el cronograma oficial de SUNAT (cambia cada año y depende del último dígito de tu RUC).",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    )
}