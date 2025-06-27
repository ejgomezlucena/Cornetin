package com.enriquegomezlucena.cornetin

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CrearToqueActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    // Registro para selector de archivo con permiso persistente
    private val selectorIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        findViewById<EditText>(R.id.edtSonido).setText(uri.toString())
                        Toast.makeText(this, "Sonido externo seleccionado", Toast.LENGTH_SHORT).show()
                    } catch (_: SecurityException) {
                        Toast.makeText(this, "No se pudo conceder permiso de lectura", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_toque)

        val edtNombre = findViewById<EditText>(R.id.edtNombre)
        val edtHora = findViewById<EditText>(R.id.edtHora)
        val edtRepeticion = findViewById<EditText>(R.id.edtRepeticion)
        val spinnerCategoria = findViewById<Spinner>(R.id.spinnerCategoria)
        val spinnerMes = findViewById<Spinner>(R.id.spinnerMes)
        val edtSonido = findViewById<EditText>(R.id.edtSonido)
        val chkActivo = findViewById<CheckBox>(R.id.chkActivo)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarToque)
        val btnVolver = findViewById<Button>(R.id.btnVolverCrear)
        val btnSeleccionarSonido = findViewById<Button>(R.id.btnSeleccionarSonido)

        val mapaCategorias = mutableMapOf<String, String>()
        val mapaMeses = mutableMapOf<String, String>()

        db.collection("categorias").get().addOnSuccessListener { docs ->
            val nombres = docs.mapNotNull {
                val nombre = it.getString("nombre")?.uppercase()
                if (nombre != null) mapaCategorias[nombre] = it.id
                nombre
            }.sorted()

            val adapter = ArrayAdapter(this, R.layout.item_spinner_texto, nombres)
            adapter.setDropDownViewResource(R.layout.item_spinner_texto)
            spinnerCategoria.adapter = adapter
        }

        db.collection("meses").get().addOnSuccessListener { docs ->
            val nombres = mutableListOf("")
            docs.forEach {
                val nombre = it.getString("nombre")?.replaceFirstChar { c -> c.uppercaseChar() }
                if (!nombre.isNullOrBlank()) {
                    mapaMeses[nombre.lowercase()] = it.id
                    nombres.add(nombre)
                }
            }

            val adapter =
                ArrayAdapter(this, R.layout.item_spinner_texto, nombres.sortedBy { it.lowercase() })
            adapter.setDropDownViewResource(R.layout.item_spinner_texto)
            spinnerMes.adapter = adapter
        }

        edtHora.setOnClickListener {
            val ahora = Calendar.getInstance()
            TimePickerDialog(
                this, { _, h, m ->
                    edtHora.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m))
                }, ahora.get(Calendar.HOUR_OF_DAY), ahora.get(Calendar.MINUTE), true
            ).show()
        }

        btnSeleccionarSonido.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/*"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            }
            selectorIntent.launch(intent)
        }

        btnGuardar.setOnClickListener {
            val nombre = edtNombre.text.toString().trim()
            val hora = edtHora.text.toString().trim()
            val repeticion = edtRepeticion.text.toString().trim()
            val categoriaNombre = spinnerCategoria.selectedItem?.toString()?.uppercase() ?: ""
            val mesSeleccionado = spinnerMes.selectedItem?.toString()?.lowercase() ?: ""
            val sonido = edtSonido.text.toString().trim()
            val activo = chkActivo.isChecked

            if (nombre.isEmpty() || sonido.isEmpty()) {
                Toast.makeText(this, "Nombre y sonido son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (hora.isNotEmpty() && !hora.matches(Regex("^[0-2][0-9]:[0-5][0-9]$"))) {
                Toast.makeText(this, "Formato de hora incorrecto. Usa HH:mm", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val categoriaId = mapaCategorias[categoriaNombre]
            val mesId = mapaMeses[mesSeleccionado] ?: ""

            if (categoriaId == null) {
                Toast.makeText(this, "Categoría no válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val repeticionLimpia = repeticion.replace(Regex("\\s*\\(.*?\\)"), "").trim()

            val nuevoToque = hashMapOf(
                "nombre" to nombre,
                "hora" to hora,
                "repeticion" to repeticionLimpia,
                "categoria_id" to categoriaId,
                "mes_id" to mesId,
                "sonido" to if (sonido.startsWith("content://")) sonido else sonido.replace(".mp3", "").lowercase(),
                "activo" to activo
            )

            db.collection("toques").add(nuevoToque).addOnSuccessListener {
                Toast.makeText(this, "Toque guardado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Error al guardar: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        btnVolver.setOnClickListener { finish() }
    }
}
