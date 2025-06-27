package com.enriquegomezlucena.cornetin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.PowerManager
import android.util.Log
import androidx.core.net.toUri

// BroadcastReceiver que se activa cuando el AlarmManager lanza una alarma programada
class ReceptorToque : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val nombreSonido = intent.getStringExtra("sonido") ?: return

        try {
            // WakeLock para evitar suspensión durante la reproducción
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "Cornetin::ReproduccionToqueWakeLock"
            )
            wakeLock.acquire(10_000L)

            // Crear y configurar MediaPlayer según el origen del sonido
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                )

                if (nombreSonido.startsWith("content://")) {
                    // Reproducción desde almacenamiento externo
                    setDataSource(context, nombreSonido.toUri())
                } else {
                    // Reproducción desde res/raw
                    val idSonido = obtenerIdRecursoSonido(nombreSonido)
                    if (idSonido == 0) {
                        Log.w("ReceptorToque", "Sonido '$nombreSonido' no encontrado en res/raw")
                        wakeLock.release()
                        return
                    }

                    context.resources.openRawResourceFd(idSonido)?.use { afd ->
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    }
                }

                prepare()
                setOnCompletionListener {
                    it.release()
                    wakeLock.release()
                }
                start()
            }

            Log.i("ReceptorToque", "Reproduciendo sonido: $nombreSonido")

        } catch (e: Exception) {
            Log.e("ReceptorToque", "Error al reproducir sonido: ${e.message}")
        }
    }

    // Mapeo seguro del nombre del sonido a su recurso en res/raw
    private fun obtenerIdRecursoSonido(nombre: String): Int {
        return when (nombre.lowercase().replace(".mp3", "")) {
            "asamblea" -> R.raw.asamblea
            "bandera" -> R.raw.bandera
            "bandera_arriado" -> R.raw.bandera_arriado
            "diana" -> R.raw.diana
            "fajina" -> R.raw.fajina
            "generala" -> R.raw.generala
            "llamada" -> R.raw.llamada
            "marcha" -> R.raw.marcha
            "oracion" -> R.raw.oracion
            "retreta" -> R.raw.retreta
            "silencio" -> R.raw.silencio
            else -> 0
        }
    }
}
