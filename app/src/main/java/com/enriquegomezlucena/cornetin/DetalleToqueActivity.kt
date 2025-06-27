package com.enriquegomezlucena.cornetin

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

// Esta actividad muestra el detalle completo de un toque individual
class DetalleToqueActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_toque)

        // Obtener los datos del toque recibidos por Intent
        val nombre = intent.getStringExtra("nombre") ?: "Sin nombre"
        val hora = intent.getStringExtra("hora") ?: "Sin hora"
        val repeticion = intent.getStringExtra("repeticion") ?: "Sin repetición"
        val sonidoBruto = intent.getStringExtra("sonido") ?: ""

        // Referencias a las vistas
        val txtNombre = findViewById<TextView>(R.id.txtDetalleNombre)
        val txtHora = findViewById<TextView>(R.id.txtDetalleHora)
        val txtRepeticion = findViewById<TextView>(R.id.txtDetalleRepeticion)
        val btnVolver = findViewById<Button>(R.id.btnVolver)
        val btnReproducir = findViewById<Button>(R.id.btnReproducir)

        // Mostrar los datos
        txtNombre.text = nombre
        txtHora.text = "Hora: $hora"
        txtRepeticion.text = "Repetición: $repeticion"

        // Botón para volver atrás
        btnVolver.setOnClickListener {
            finish()
        }

        // Botón para reproducir el sonido
        btnReproducir.setOnClickListener {
            if (sonidoBruto.isNotBlank()) {
                reproducirSonido(sonidoBruto)
            } else {
                Toast.makeText(this, "Este toque no tiene sonido asignado.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función que reproduce un sonido desde res/raw o una URI externa
    @SuppressLint("DiscouragedApi")
    private fun reproducirSonido(sonido: String) {
        mediaPlayer?.release()
        mediaPlayer = null

        try {
            val contexto = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                this.createAttributionContext("ReproduccionCornetin")
            } else {
                this
            }

            val uri = sonido.toUri()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )

                if (uri.scheme == "content") {
                    // Reproducir desde almacenamiento externo
                    setDataSource(contexto, uri)
                } else {
                    // Reproducir desde res/raw
                    val nombreSonido = sonido.replace(".mp3", "").lowercase()
                    val recursoId = resources.getIdentifier(nombreSonido, "raw", packageName)
                    if (recursoId == 0) {
                        Toast.makeText(this@DetalleToqueActivity, "Sonido no encontrado: $nombreSonido", Toast.LENGTH_SHORT).show()
                        return
                    }
                    val afd = resources.openRawResourceFd(recursoId) ?: return
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                }

                prepare()
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                start()
            }

        } catch (e: Exception) {
            Log.e("DetalleToqueActivity", "Error al reproducir: ${e.message}")
            Toast.makeText(this, "Error al reproducir el sonido", Toast.LENGTH_LONG).show()
        }
    }

    // Liberar el mediaPlayer al salir
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
