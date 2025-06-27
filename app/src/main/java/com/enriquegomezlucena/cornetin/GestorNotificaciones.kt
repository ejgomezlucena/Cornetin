package com.enriquegomezlucena.cornetin

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

// Clase que se encarga de programar una notificación push-up 5 minutos antes del toque
object GestorNotificaciones {

    // Esta función lanza una notificación justo antes del toque usando WorkManager
    fun programarNotificacion(contexto: Context, nombreToque: String, horaToque: Calendar) {
        val ahora = Calendar.getInstance()

        // Calculamos la hora en la que debe salir la notificación (5 minutos antes del toque)
        val horaNotificacion = horaToque.clone() as Calendar
        horaNotificacion.add(Calendar.MINUTE, -5)

        // Calculamos cuántos milisegundos faltan desde ahora hasta la notificación
        val delayMillis = horaNotificacion.timeInMillis - ahora.timeInMillis

        // Si el momento ya pasó, no programamos nada
        if (delayMillis <= 0) {
            Log.w("GestorNotificaciones", "Ya ha pasado la hora de notificar $nombreToque.")
            return
        }

        // Preparamos los datos que recibirá el Worker (en este caso, el nombre del toque)
        val datos = Data.Builder().putString("nombre_toque", nombreToque).build()

        // Creamos una tarea única que se ejecutará después del retraso calculado
        val request = OneTimeWorkRequestBuilder<NotificacionWorker>().setInitialDelay(
            delayMillis, TimeUnit.MILLISECONDS
        ).setInputData(datos)
            .addTag("notificacion_$nombreToque") // Etiqueta para identificarla si es necesario
            .build()

        // Enviamos la tarea al sistema para que la ejecute en segundo plano cuando llegue el momento
        WorkManager.getInstance(contexto.applicationContext).enqueue(request)

        Log.i(
            "GestorNotificaciones",
            "Notificación programada para $nombreToque a las ${horaNotificacion.time}"
        )
    }
}
