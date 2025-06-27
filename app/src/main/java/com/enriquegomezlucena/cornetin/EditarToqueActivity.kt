package com.enriquegomezlucena.cornetin

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class EditarToqueActivity : AppCompatActivity() {
    // Referencia a las vistas de la interfaz

    private lateinit var edtNombre: EditText
    private lateinit var edtHora: EditText
    private lateinit var edtRepeticion: EditText
    private lateinit var spinnerCategoria: Spinner
    private lateinit var spinnerMes: Spinner
    private lateinit var edtSonido: EditText
    private lateinit var chkActivo: CheckBox
    private lateinit var btnActualizar: Button
    private lateinit var btnVolver: Button
    private lateinit var btnSeleccionarSonido: Button

    // Instancia de la base de datos Firestore

    private val db = FirebaseFirestore.getInstance()
    private lateinit var toqueId: String
    private val mapaCategorias = mutableMapOf<String, String>()
    private val mapaMeses = mutableMapOf<String, String>()
//
    private val selectorIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        edtSonido.setText(uri.toString())
                        Toast.makeText(this, "Sonido externo seleccionado", Toast.LENGTH_SHORT)
                            .show()
                    } catch (_: SecurityException) {
                        Toast.makeText(
                            this, "No se pudo conceder permiso de lectura", Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_toque)

        edtNombre = findViewById(R.id.edtNombreEditar)
        edtHora = findViewById(R.id.edtHoraEditar)
        edtRepeticion = findViewById(R.id.edtRepeticionEditar)
        spinnerCategoria = findViewById(R.id.spinnerCategoriaEditar)
        spinnerMes = findViewById(R.id.spinnerMesEditar)
        edtSonido = findViewById(R.id.edtSonidoEditar)
        chkActivo = findViewById(R.id.chkActivoEditar)
        btnActualizar = findViewById(R.id.btnActualizarToque)
        btnVolver = findViewById(R.id.btnVolverEditar)
        btnSeleccionarSonido = findViewById(R.id.btnSeleccionarSonidoEditar)

        toqueId = intent.getStringExtra("id_toque") ?: ""
        if (toqueId.isEmpty()) {
            Toast.makeText(this, "ID del toque no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cargarCategorias()
        cargarMeses()
        cargarDatosToque()

        edtHora.setOnClickListener {
            val ahora = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                edtHora.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m))
            }, ahora.get(Calendar.HOUR_OF_DAY), ahora.get(Calendar.MINUTE), true).show()
        }

        btnSeleccionarSonido.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/*"
                flags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            }
            selectorIntent.launch(intent)
        }

        btnActualizar.setOnClickListener { actualizarToque() }
        btnVolver.setOnClickListener { finish() }
    }

    private fun cargarCategorias() {
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
    }

    private fun cargarMeses() {
        db.collection("meses").get().addOnSuccessListener { docs ->
            val nombres = mutableListOf("")
            docs.forEach {
                val nombre = it.getString("nombre") ?: return@forEach
                val capitalizado = nombre.replaceFirstChar { c -> c.uppercaseChar() }
                mapaMeses[nombre.lowercase()] = it.id
                nombres.add(capitalizado)
            }

            val adapter =
                ArrayAdapter(this, R.layout.item_spinner_texto, nombres.sortedBy { it.lowercase() })
            adapter.setDropDownViewResource(R.layout.item_spinner_texto)
            spinnerMes.adapter = adapter
        }
    }

    private fun cargarDatosToque() {
        db.collection("toques").document(toqueId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val toque = doc.toObject(Toques::class.java)
                toque?.let {
                    edtNombre.setText(it.nombre)
                    edtHora.setText(it.hora)
                    edtRepeticion.setText(it.repeticion)
                    edtSonido.setText(it.sonido)
                    chkActivo.isChecked = it.activo

                    db.collection("categorias").document(it.categoria_id).get()
                        .addOnSuccessListener { catDoc ->
                            val nombreCat = catDoc.getString("nombre")?.uppercase()
                            val indexCat = (0 until spinnerCategoria.count).firstOrNull {
                                spinnerCategoria.getItemAtPosition(it) == nombreCat
                            } ?: 0
                            spinnerCategoria.setSelection(indexCat)
                        }

                    if (it.mes_id.isNotEmpty()) {
                        db.collection("meses").document(it.mes_id).get()
                            .addOnSuccessListener { mesDoc ->
                                val nombreMes = mesDoc.getString("nombre")
                                    ?.replaceFirstChar { c -> c.uppercaseChar() }
                                val indexMes = (0 until spinnerMes.count).firstOrNull {
                                    spinnerMes.getItemAtPosition(it) == nombreMes
                                } ?: 0
                                spinnerMes.setSelection(indexMes)
                            }
                    }
                }
            }
        }
    }

    private fun actualizarToque() {
        val nombre = edtNombre.text.toString().trim()
        val hora = edtHora.text.toString().trim()
        val repeticion = edtRepeticion.text.toString().trim().replace(Regex("\\s*\\(.*?\\)"), "")
        val categoriaNombre = spinnerCategoria.selectedItem?.toString()?.uppercase() ?: ""
        val mesNombre = spinnerMes.selectedItem?.toString()?.lowercase() ?: ""
        val sonido = edtSonido.text.toString().trim()
        val activo = chkActivo.isChecked

        if (nombre.isEmpty() || sonido.isEmpty()) {
            Toast.makeText(this, "El nombre y el sonido son obligatorios", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (hora.isNotEmpty() && !hora.matches(Regex("^[0-2][0-9]:[0-5][0-9]$"))) {
            Toast.makeText(this, "Formato de hora incorrecto. Usa HH:mm", Toast.LENGTH_LONG).show()
            return
        }

        val categoriaId = mapaCategorias[categoriaNombre]
        if (categoriaId == null) {
            Toast.makeText(this, "Categoría no válida", Toast.LENGTH_SHORT).show()
            return
        }

        val mesId = mapaMeses[mesNombre] ?: ""

        val datosActualizados = mapOf(
            "nombre" to nombre,
            "hora" to hora,
            "repeticion" to repeticion,
            "categoria_id" to categoriaId,
            "mes_id" to mesId,
            "sonido" to if (sonido.startsWith("content://")) sonido else sonido.replace(".mp3", "")
                .lowercase(),
            "activo" to activo
        )

        db.collection("toques").document(toqueId).update(datosActualizados).addOnSuccessListener {
            Toast.makeText(this, "Toque actualizado correctamente", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Error al actualizar: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }
}
