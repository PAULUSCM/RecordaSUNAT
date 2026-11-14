package com.example.recordasunat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PantallaAyuda(onActivarNotificaciones: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TarjetaAyuda("¿QUÉ HACE ESTA APP?", listOf(
            "Guardas aquí tus vencimientos y la app te avisa ANTES de que venzan: 5 días antes en la mayoría, y 7 días antes en cumpleaños y aniversarios.",
            "El aviso se queda fijo en tus notificaciones y cuando llega el día (a las 8 de la mañana) suena una alarma que se repite cada 10 minutos hasta que pulses el botón de confirmación. Después de las 10 de la noche se calma y vuelve a sonar al día siguiente a las 8 am.",
            "Al confirmar, la app apaga la alarma y reinicia el ciclo sola para el siguiente mes o año. No tienes que hacer nada más."
        ))

        TarjetaAyuda("PESTAÑA MENSUAL (se repite cada mes)", listOf(
            "Para todo lo que pagas o declaras cada mes. Ponle una etiqueta de color para encontrarlo rápido:",
            "SUNAT: IGV - PDT 621 (se declara entre el día 12 y 18 según el calendario SUNAT), detracciones, PLAME, pago a cuenta.",
            "SERVICIOS: luz, agua, internet, celular, gas, TV por cable.",
            "FINANZAS: tarjeta de crédito, préstamos, cuota del auto, AFP.",
            "HOGAR: alquiler, colegiatura, empleada del hogar, mantenimiento del edificio.",
            "TRABAJO: planilla, reportes mensuales, cuotas de socios.",
            "OTROS: cualquier otro pago mensual tuyo."
        ))

        TarjetaAyuda("PESTAÑA ANIVERSARIOS (aviso 7 días antes)", listOf(
            "Para cumpleaños y aniversarios que no quieres olvidar: cumpleaños de mamá (14 de mayo), aniversario de bodas (3 de setiembre), aniversario de tu empresa (12 de agosto).",
            "Aquí el aviso llega 7 días antes, para que tengas tiempo de comprar el regalo o reservar el restaurante.",
            "Usa los botones RESPALDAR e IMPORTAR de esta pestaña para guardar tus aniversarios en un archivo y recuperarlos si cambias de celular. Este respaldo es solo de la pestaña ANIVERSARIOS."
        ))

        TarjetaAyuda("PESTAÑA ANUAL (una vez al año)", listOf(
            "Para obligaciones y trámites que se repiten una vez al año:",
            "Declaración Jurada anual de renta (marzo a mayo según el cronograma SUNAT).",
            "Impuesto predial (se puede pagar en enero, a veces con descuento).",
            "SOAT (en la fecha de tu póliza), revisión técnica vehicular, renovación de licencia de conducir, renovación de documentos o mantenimientos anuales."
        ))

        TarjetaAyuda("PESTAÑA GENERAL (fechas puntuales)", listOf(
            "Para citas y eventos de UNA SOLA VEZ, con fecha y hora: reunión con cliente el 20/03 a las 10:00, cita médica, trámite en el banco, entrega de un proyecto, pago de una cuota inicial.",
            "Estos no se repiten: cuando confirmas que ya lo hiciste, se apagan sus alarmas y el aviso desaparece."
        ))

        TarjetaAyuda("CÓMO CONFIRMAR QUE YA LO HICISTE", listOf(
            "Cuando llega el día del vencimiento, en la tarjeta aparece un botón que cambia según el tipo:",
            "YA DECLARÉ para lo de SUNAT, YA PAGUÉ para servicios y pagos, YA LO HICE para todo lo demás.",
            "Al pulsarlo la alarma se apaga y el ciclo vuelve a empezar solo para el siguiente periodo. También puedes confirmarlo desde la propia notificación."
        ))

        TarjetaAyuda("MUY IMPORTANTE: ACTIVA LAS NOTIFICACIONES", listOf(
            "Las notificaciones NO se activan solas al instalar. Hazlo una vez, toma 1 minuto:",
            "1) Toca el botón ACTIVAR NOTIFICACIONES de abajo y acepta el permiso.",
            "2) Si el aviso no aparece: Ajustes > Aplicaciones > INGES BUILDERS RECORDATORIOS PRO > Notificaciones > activa todas.",
            "3) Ajustes > Batería > esta app > elige Sin restricciones. Si el celular la duerme para ahorrar batería, las alarmas no suenan.",
            "4) En Xiaomi, Redmi o Poco: activa además Inicio automático. En Samsung: permite que la app trabaje en segundo plano.",
            "5) Toca PROBAR NOTIFICACIÓN: si te llega la notificación de prueba, todo quedó listo."
        ))

        Button(
            onClick = onActivarNotificaciones,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color(0xFF14161B))
        ) {
            Text("ACTIVAR NOTIFICACIONES", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { MotorAlarmas.notificacionPrueba(context) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White)
        ) {
            Text("PROBAR NOTIFICACIÓN", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun TarjetaAyuda(titulo: String, parrafos: List<String>) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1F26)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(titulo, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            parrafos.forEach { Text(it, color = Color(0xFFDDDFE5), fontSize = 13.sp) }
        }
    }
}