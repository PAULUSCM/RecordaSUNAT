package com.example.recordasunat

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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

@Composable
fun PantallaPrincipal() {
    val context = LocalContext.current
    var lista by remember { mutableStateOf<List<ItemRecordatorio>>(emptyList()) }
    var indicePestana by remember { mutableStateOf(0) }
    var dialogoGrupo by remember { mutableStateOf<Grupo?>(null) }
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
                val n = DatosApp.importarAniversarios(context, texto)
                recargar()
                MotorAlarmas.arrancar(context)
                Toast.makeText(context, if (n > 0) "Listo: se agregaron $n aniversarios" else "El archivo no tiene aniversarios que se reconozcan", Toast.LENGTH_LONG).show()
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
                            0 -> Grupo.MENSUAL
                            1 -> Grupo.ANIVERSARIOS
                            2 -> Grupo.ANUAL
                            else -> Grupo.GENERAL
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
            TabRow(
                selectedTabIndex = indicePestana,
                containerColor = Color(0xFF1C1F26),
                contentColor = Color.White
            ) {
                listOf("MENSUAL", "ANIVERSARIOS", "ANUAL", "GENERAL", "AYUDA").forEachIndexed { i, nombre ->
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
            } else {
                val grupoActual = when (indicePestana) {
                    0 -> Grupo.MENSUAL
                    1 -> Grupo.ANIVERSARIOS
                    2 -> Grupo.ANUAL
                    else -> Grupo.GENERAL
                }
                val itemsPestana = lista.filter { it.grupo == grupoActual }
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
            grupo = dialogoGrupo!!,
            item = null,
            onGuardar = { nuevo ->
                val actual = DatosApp.cargar(context).toMutableList()
                actual.add(nuevo)
                DatosApp.guardar(context, actual)
                MotorAlarmas.programar(context, nuevo)
                recargar()
                dialogoGrupo = null
            },
            onCerrar = { dialogoGrupo = null }
        )
    }

    if (itemEditar != null) {
        DialogoFormulario(
            grupo = itemEditar!!.grupo,
            item = itemEditar,
            onGuardar = { actualizado ->
                val actual = DatosApp.cargar(context).toMutableList()
                val posicion = actual.indexOfFirst { it.id == actualizado.id }
                if (posicion >= 0) actual[posicion] = actualizado
                DatosApp.guardar(context, actual)
                MotorAlarmas.programar(context, actualizado)
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
    0 -> "Aún no hay recordatorios MENSUALES. Toca AGREGAR MENSUAL para poner tu IGV, luz, agua, internet, alquiler, tarjeta, etc."
    1 -> "Aún no hay ANIVERSARIOS. Toca AGREGAR ANIVERSARIO para poner cumpleaños y aniversarios (te avisamos 7 días antes)."
    2 -> "Aún no hay recordatorios ANUALES. Toca AGREGAR ANUAL para la DJ de renta, predial, SOAT, revisión técnica, etc."
    else -> "Aún no hay recordatorios de fecha puntual. Toca AGREGAR GENERAL para reuniones, citas y trámites con día y hora."
}

fun colorEtiqueta(e: EtiquetaMensual): Color = when (e) {
    EtiquetaMensual.SUNAT -> Color(0xFFE53935)
    EtiquetaMensual.SERVICIOS -> Color(0xFF29B6F6)
    EtiquetaMensual.FINANZAS -> Color(0xFF66BB6A)
    EtiquetaMensual.HOGAR -> Color(0xFFFFB74D)
    EtiquetaMensual.TRABAJO -> Color(0xFFAB47BC)
    EtiquetaMensual.OTROS -> Color(0xFF90A4AE)
}

fun colorTipo(t: TipoAniversario): Color = when (t) {
    TipoAniversario.CUMPLEANOS -> Color(0xFFF06292)
    TipoAniversario.ANIVERSARIO -> Color(0xFFFFB300)
    TipoAniversario.OTRO -> Color(0xFF90A4AE)
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
fun TarjetaRecordatorio(
    item: ItemRecordatorio,
    onEditar: () -> Unit,
    onBorrar: () -> Unit,
    onConfirmar: () -> Unit
) {
    val vencimiento = DatosApp.vencimientoDe(item)
    val confirmado = DatosApp.yaConfirmado(item, vencimiento)
    val dias = Duration.between(LocalDateTime.now(), vencimiento).toDays()

    val textoEstado = when {
        confirmado -> "CONFIRMADO ESTE CICLO"
        dias < 0 -> "VENCIDO"
        dias == 0L -> "VENCE HOY"
        else -> "Vence en $dias días"
    }
    val colorEstado = when {
        confirmado -> Color(0xFF66BB6A)
        dias < 0 -> Color(0xFFEF5350)
        else -> Color(0xFFFFB300)
    }

    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1F26)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.titulo, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onEditar) { Text("EDITAR", color = Color(0xFF4FC3F7), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                TextButton(onClick = onBorrar) { Text("BORRAR", color = Color(0xFFEF5350), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
            if (item.grupo == Grupo.MENSUAL) {
                ChipColor(texto = item.etiqueta?.texto ?: "OTROS", fondo = colorEtiqueta(item.etiqueta ?: EtiquetaMensual.OTROS))
            }
            if (item.grupo == Grupo.ANIVERSARIOS) {
                ChipColor(texto = item.tipoAniversario?.texto ?: "OTRO", fondo = colorTipo(item.tipoAniversario ?: TipoAniversario.OTRO))
            }
            Text(DatosApp.descripcionVencimiento(item), color = Color(0xFFC9CDD6), fontSize = 13.sp)
            Text(textoEstado, color = colorEstado, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            if (!item.monto.isNullOrEmpty()) Text("Monto: ${item.monto}", color = Color(0xFFC9CDD6), fontSize = 13.sp)
            if (!item.nota.isNullOrEmpty()) Text(item.nota, color = Color(0xFF9AA0B0), fontSize = 13.sp)
            if (!confirmado) {
                Button(
                    onClick = onConfirmar,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White)
                ) {
                    Text(DatosApp.textoBotonConfirmar(item), fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Ya confirmaste este ciclo. Te avisamos de nuevo en el siguiente.", color = Color(0xFF66BB6A), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun DialogoFormulario(
    grupo: Grupo,
    item: ItemRecordatorio?,
    onGuardar: (ItemRecordatorio) -> Unit,
    onCerrar: () -> Unit
) {
    val esEdicion = item != null

    var titulo by remember { mutableStateOf(item?.titulo ?: "") }
    var etiquetaSel by remember { mutableStateOf(item?.etiqueta ?: EtiquetaMensual.SUNAT) }
    var tipoSel by remember { mutableStateOf(item?.tipoAniversario ?: TipoAniversario.CUMPLEANOS) }
    var diaTexto by remember { mutableStateOf(item?.diaMes?.toString() ?: "15") }
    var mesIndice by remember { mutableStateOf(((item?.mes ?: 1) - 1).coerceIn(0, 11)) }
    var fechaTexto by remember { mutableStateOf(item?.fechaGeneral ?: "") }
    var horaTexto by remember { mutableStateOf(item?.horaGeneral ?: "08:00") }
    var montoTexto by remember { mutableStateOf(item?.monto ?: "") }
    var notaTexto by remember { mutableStateOf(item?.nota ?: "") }
    var mensajeError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = Color(0xFF1C1F26),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFDDDFE5),
        title = { Text(if (esEdicion) "EDITAR ${grupo.titulo}" else "AGREGAR ${grupo.titulo}", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()).heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (mensajeError.isNotEmpty()) {
                    Text(mensajeError, color = Color(0xFFEF5350), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                CampoTexto(titulo, { titulo = it }, "Título", apoyo = "Ej: IGV - PDT 621")

                if (grupo == Grupo.MENSUAL) {
                    Text("Etiqueta:", color = Color(0xFF9AA0B0), fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChipSelector("SUNAT", colorEtiqueta(EtiquetaMensual.SUNAT), etiquetaSel == EtiquetaMensual.SUNAT) { etiquetaSel = EtiquetaMensual.SUNAT }
                        ChipSelector("SERVICIOS", colorEtiqueta(EtiquetaMensual.SERVICIOS), etiquetaSel == EtiquetaMensual.SERVICIOS) { etiquetaSel = EtiquetaMensual.SERVICIOS }
                        ChipSelector("FINANZAS", colorEtiqueta(EtiquetaMensual.FINANZAS), etiquetaSel == EtiquetaMensual.FINANZAS) { etiquetaSel = EtiquetaMensual.FINANZAS }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChipSelector("HOGAR", colorEtiqueta(EtiquetaMensual.HOGAR), etiquetaSel == EtiquetaMensual.HOGAR) { etiquetaSel = EtiquetaMensual.HOGAR }
                        ChipSelector("TRABAJO", colorEtiqueta(EtiquetaMensual.TRABAJO), etiquetaSel == EtiquetaMensual.TRABAJO) { etiquetaSel = EtiquetaMensual.TRABAJO }
                        ChipSelector("OTROS", colorEtiqueta(EtiquetaMensual.OTROS), etiquetaSel == EtiquetaMensual.OTROS) { etiquetaSel = EtiquetaMensual.OTROS }
                    }
                }

                if (grupo == Grupo.ANIVERSARIOS) {
                    Text("Tipo:", color = Color(0xFF9AA0B0), fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChipSelector("CUMPLEAÑOS", colorTipo(TipoAniversario.CUMPLEANOS), tipoSel == TipoAniversario.CUMPLEANOS) { tipoSel = TipoAniversario.CUMPLEANOS }
                        ChipSelector("ANIVERSARIO", colorTipo(TipoAniversario.ANIVERSARIO), tipoSel == TipoAniversario.ANIVERSARIO) { tipoSel = TipoAniversario.ANIVERSARIO }
                        ChipSelector("OTRO", colorTipo(TipoAniversario.OTRO), tipoSel == TipoAniversario.OTRO) { tipoSel = TipoAniversario.OTRO }
                    }
                }

                if (grupo == Grupo.MENSUAL || grupo == Grupo.ANIVERSARIOS || grupo == Grupo.ANUAL) {
                    CampoTexto(diaTexto, { diaTexto = it }, "Día del mes (1 a 31)", numero = true)
                    if (grupo != Grupo.MENSUAL) {
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
                }

                if (grupo == Grupo.MENSUAL) {
                    CampoTexto(montoTexto, { montoTexto = it }, "Monto (opcional)", apoyo = "Ej: S/ 550.00")
                }

                if (grupo == Grupo.GENERAL) {
                    CampoTexto(fechaTexto, { fechaTexto = it }, "Fecha", apoyo = "Ej: 15/03/2026")
                    CampoTexto(horaTexto, { horaTexto = it }, "Hora del aviso", apoyo = "Ej: 09:30")
                }

                CampoTexto(notaTexto, { notaTexto = it }, "Nota (opcional)", apoyo = "Ej: usar clave del banco")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tituloLimpio = titulo.trim()
                    if (tituloLimpio.isNotEmpty()) {
                        val dia = diaTexto.trim().toIntOrNull()
                        if (grupo != Grupo.GENERAL && (dia == null || dia < 1 || dia > 31)) {
                            mensajeError = "El día debe ser un número de 1 a 31."
                        } else {
                            var fechaISO = ""
                            var fechaOk = true
                            if (grupo == Grupo.GENERAL) {
                                val p = fechaTexto.trim().split("/")
                                if (p.size == 3) {
                                    var anio = p[2].trim().toIntOrNull() ?: -1
                                    if (anio in 0..99) anio += 2000
                                    val f = try {
                                        LocalDate.of(anio, p[1].trim().toIntOrNull() ?: -1, p[0].trim().toIntOrNull() ?: -1)
                                    } catch (e: Exception) { null }
                                    if (f == null) {
                                        fechaOk = false
                                        mensajeError = "Escribe la fecha como 15/03/2026."
                                    } else {
                                        fechaISO = f.toString()
                                    }
                                } else {
                                    fechaOk = false
                                    mensajeError = "Escribe la fecha como 15/03/2026."
                                }
                            }
                            if (fechaOk) {
                                var horaFinal = "08:00"
                                var horaOk = true
                                if (grupo == Grupo.GENERAL && horaTexto.trim().isNotEmpty()) {
                                    val hp = horaTexto.trim().split(":")
                                    val h = try {
                                        LocalTime.of(hp[0].trim().toIntOrNull() ?: -1, (hp.getOrNull(1) ?: "0").trim().toIntOrNull() ?: -1)
                                    } catch (e: Exception) { null }
                                    if (h == null) {
                                        horaOk = false
                                        mensajeError = "Escribe la hora como 09:30."
                                    } else {
                                        horaFinal = h.toString()
                                    }
                                }
                                if (horaOk) {
                                    val nuevo = if (item != null) {
                                        item.copy(
                                            titulo = tituloLimpio,
                                            etiqueta = if (grupo == Grupo.MENSUAL) etiquetaSel else EtiquetaMensual.OTROS,
                                            diaMes = if (grupo == Grupo.GENERAL) 1 else dia!!,
                                            mes = if (grupo == Grupo.GENERAL) 1 else mesIndice + 1,
                                            fechaGeneral = if (grupo == Grupo.GENERAL) fechaISO else "",
                                            horaGeneral = if (grupo == Grupo.GENERAL) horaFinal else "",
                                            tipoAniversario = if (grupo == Grupo.ANIVERSARIOS) tipoSel else TipoAniversario.OTRO,
                                            monto = if (grupo == Grupo.MENSUAL) montoTexto.trim() else "",
                                            nota = notaTexto.trim(),
                                            cicloConfirmado = ""
                                        )
                                    } else {
                                        ItemRecordatorio(
                                            id = System.currentTimeMillis(),
                                            grupo = grupo,
                                            titulo = tituloLimpio,
                                            etiqueta = if (grupo == Grupo.MENSUAL) etiquetaSel else EtiquetaMensual.OTROS,
                                            diaMes = if (grupo == Grupo.GENERAL) 1 else dia!!,
                                            mes = if (grupo == Grupo.GENERAL) 1 else mesIndice + 1,
                                            fechaGeneral = if (grupo == Grupo.GENERAL) fechaISO else "",
                                            horaGeneral = if (grupo == Grupo.GENERAL) horaFinal else "",
                                            tipoAniversario = if (grupo == Grupo.ANIVERSARIOS) tipoSel else TipoAniversario.OTRO,
                                            monto = if (grupo == Grupo.MENSUAL) montoTexto.trim() else "",
                                            nota = notaTexto.trim(),
                                            activo = true,
                                            cicloConfirmado = ""
                                        )
                                    }
                                    onGuardar(nuevo)
                                }
                            }
                        }
                    } else {
                        mensajeError = "Escribe un título."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color(0xFF14161B))
            ) { Text(if (esEdicion) "GUARDAR CAMBIOS" else "GUARDAR", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("CANCELAR", color = Color(0xFF9AA0B0)) } }
    )
}