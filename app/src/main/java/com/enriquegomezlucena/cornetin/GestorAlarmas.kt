package com.enriquegomezlucena.cornetin

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

object GestorAlarmas {

    // Programa todas las alarmas activas desde Firebase
    fun programarAlarmas(contexto: Context) {
        val db = FirebaseFirestore.getInstance()

        // Primero cargar los meses para asociar 'mes_id' con su nombre
        val mapaMeses = mutableMapOf<String, String>()

        db.collection("meses").get().addOnSuccessListener { mesesDocs ->
            for (doc in mesesDocs) {
                val id = doc.id
                val nombre = doc.getString("nombre") ?: continue // CAMBIO: se usa el campo "nombre"
                mapaMeses[id] = nombre.uppercase() // Guardamos el nombre en mayúsculas
            }

            // Obtener el nombre del mes actual en mayúsculas
            val mesActual =
                Calendar.getInstance().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("es"))
                    ?.uppercase()

            // Ahora cargar los toques activos
            db.collection("toques").whereEqualTo("activo", true).get()
                .addOnSuccessListener { documentos ->
                    for (documento in documentos) {
                        val toque = documento.toObject(Toques::class.java)

                        if (toque.hora.isBlank()) {
                            Log.w(
                                "GestorAlarmas",
                                "Toque manual (sin hora): ${toque.nombre}, se omite"
                            )
                            continue
                        }

                        // Si tiene un mes_id y no coincide con el actual, lo ignoramos
                        val mesId = documento.getString("mes_id") ?: ""
                        val nombreMes = mapaMeses[mesId] ?: ""

                        if (nombreMes.isNotBlank() && nombreMes != mesActual) {
                            Log.i(
                                "GestorAlarmas",
                                "Toque fuera del mes actual ($nombreMes): ${toque.nombre}"
                            )
                            continue
                        }

                        // Eliminar sufijos tipo "(ENERO)" en el campo repetición
                        val repeticionLimpia =
                            toque.repeticion.replace(Regex("\\(.*\\)"), "").trim()

                        // Programar la alarma según la repetición
                        programarPorRepeticion(contexto, toque, repeticionLimpia)
                    }
                }.addOnFailureListener { e ->
                    Log.e("GestorAlarmas", "Error al recuperar los toques: ${e.message}")
                }

        }.addOnFailureListener {
            Log.e("GestorAlarmas", "Error al cargar colección 'meses'")
        }
    }

    // Programación según el campo 'repeticion'
    private fun programarPorRepeticion(contexto: Context, toque: Toques, repeticion: String) {
        val diasSemana = mapOf(
            "L" to Calendar.MONDAY,
            "M" to Calendar.TUESDAY,
            "X" to Calendar.WEDNESDAY,
            "J" to Calendar.THURSDAY,
            "V" to Calendar.FRIDAY,
            "S" to Calendar.SATURDAY,
            "D" to Calendar.SUNDAY
        )

        when (repeticion.uppercase(Locale.ROOT)) {
            "L-D" -> diasSemana.values.forEach { dia -> programarAlarmaEnDia(contexto, toque, dia) }
            "L-V" -> listOf("L", "M", "X", "J", "V").map { diasSemana[it]!! }
                .forEach { dia -> programarAlarmaEnDia(contexto, toque, dia) }

            "S-D", "S-D / FESTIVOS" -> listOf("S", "D").map { diasSemana[it]!! }
                .forEach { dia -> programarAlarmaEnDia(contexto, toque, dia) }

            "L-J" -> listOf("L", "M", "X", "J").map { diasSemana[it]!! }
                .forEach { dia -> programarAlarmaEnDia(contexto, toque, dia) }

            "V" -> programarAlarmaEnDia(contexto, toque, Calendar.FRIDAY)

            "EMERGENCIA" -> Log.i("GestorAlarmas", "Toque de emergencia (manual): ${toque.nombre}")

            else -> {
                Log.w(
                    "GestorAlarmas",
                    "Repetición no reconocida ($repeticion) en toque: ${toque.nombre}"
                )
                programarAlarmaEnDia(
                    contexto, toque, Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                )
            }
        }
    }

    // Programación específica para un día de la semana
    private fun programarAlarmaEnDia(contexto: Context, toque: Toques, diaSemana: Int) {
        try {
            val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
            val hora = formato.parse(toque.hora) ?: return

            val calHora = Calendar.getInstance().apply { time = hora }
            val horaDelDia = calHora.get(Calendar.HOUR_OF_DAY)
            val minutoDelDia = calHora.get(Calendar.MINUTE)

            val calendario = Calendar.getInstance().apply {
                val ahora = Calendar.getInstance()
                set(Calendar.HOUR_OF_DAY, horaDelDia)
                set(Calendar.MINUTE, minutoDelDia)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                while (get(Calendar.DAY_OF_WEEK) != diaSemana || before(ahora)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val intent = Intent(contexto, ReceptorToque::class.java).apply {
                putExtra("sonido", toque.sonido)
            }

            val idAlarma = (toque.nombre + diaSemana.toString()).hashCode()

            val pendingIntent = PendingIntent.getBroadcast(
                contexto,
                idAlarma,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = contexto.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.e("GestorAlarmas", "Falta permiso SCHEDULE_EXACT_ALARM")
                return
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, calendario.timeInMillis, pendingIntent
            )

            // Programar notificación push-up 5 minutos antes
            GestorNotificaciones.programarNotificacion(contexto, toque.nombre, calendario)

            Log.i(
                "GestorAlarmas",
                "Alarma programada: ${toque.nombre} para ${formato.format(calendario.time)} (día $diaSemana)"
            )
        } catch (e: Exception) {
            Log.e("GestorAlarmas", "Error al programar alarma: ${e.message}")
        }
    }
}
