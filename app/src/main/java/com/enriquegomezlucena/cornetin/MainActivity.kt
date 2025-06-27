package com.enriquegomezlucena.cornetin

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    // Lista combinada que mostrará tanto los encabezados de sección como los toques
    private val elementosRecycler = mutableListOf<Any>()

    // Adaptador del RecyclerView, con callbacks para editar y eliminar
    private lateinit var adaptador: AdaptadorSecciones

    // Instancia de Firestore
    private val db = FirebaseFirestore.getInstance()

    // Mapas que almacenan las categorías y meses legibles
    private val mapaCategorias: MutableMap<String, String> = mutableMapOf()
    private val mapaMeses: MutableMap<String, Pair<String, Int>> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permitir que el fondo de la app se dibuje detrás de la status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Controlador para ajustar el color de los iconos de la barra de estado
        val controladorBarraEstado = WindowInsetsControllerCompat(window, window.decorView)
        val fondoBarraOscuro = true // true si el fondo es oscuro
        controladorBarraEstado.isAppearanceLightStatusBars = !fondoBarraOscuro

        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerToques)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adaptador = AdaptadorSecciones(elementos = elementosRecycler, onEditarClick = { toque ->
            db.collection("toques").whereEqualTo("nombre", toque.nombre)
                .whereEqualTo("hora", toque.hora).limit(1).get().addOnSuccessListener { docs ->
                    if (docs.isEmpty) {
                        Toast.makeText(this, "No se encontró el toque", Toast.LENGTH_SHORT).show()
                    } else {
                        val doc = docs.first()
                        val intent = Intent(this, EditarToqueActivity::class.java)
                        intent.putExtra("id_toque", doc.id)
                        startActivity(intent)
                    }
                }
        }, onEliminarClick = { toque ->
            db.collection("toques").whereEqualTo("nombre", toque.nombre)
                .whereEqualTo("hora", toque.hora).limit(1).get().addOnSuccessListener { docs ->
                    if (!docs.isEmpty) {
                        val doc = docs.first()
                        db.collection("toques").document(doc.id).delete().addOnSuccessListener {
                            Toast.makeText(this, "Toque eliminado", Toast.LENGTH_SHORT).show()
                            cargarToques()
                        }.addOnFailureListener {
                            Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        })

        recyclerView.adapter = adaptador

        verificarPermisoAlarmas()
        verificarPermisoNotificaciones()
        cargarCategoriasYMeses()
        GestorAlarmas.programarAlarmas(applicationContext)
    }

    private fun cargarCategoriasYMeses() {
        db.collection("categorias").get().addOnSuccessListener { catDocs ->
            for (doc in catDocs) {
                val categoria = doc.id
                val nombre = doc.getString("nombre") ?: "SIN CATEGORÍA"
                mapaCategorias[categoria] = nombre
            }

            db.collection("meses").get().addOnSuccessListener { mesDocs ->
                for (doc in mesDocs) {
                    val mes = doc.id
                    val nombre = doc.getString("nombre") ?: "sin mes"
                    val orden = doc.getLong("orden")?.toInt() ?: 99
                    mapaMeses[mes] = Pair(nombre, orden)
                }

                cargarToques()
            }.addOnFailureListener {
                Toast.makeText(this, "Error al cargar meses", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al cargar categorías", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun cargarToques() {
        db.collection("toques").get().addOnSuccessListener { documentos ->
            val listaToques = documentos.mapNotNull { doc ->
                val toque = doc.toObject(Toques::class.java)
                toque.categoria = mapaCategorias[toque.categoria_id] ?: "SIN CATEGORÍA"
                val mesInfo = mapaMeses[toque.mes_id]
                toque.mes = mesInfo?.first ?: "sin mes"
                toque.ordenMes = mesInfo?.second ?: 99
                toque
            }

            elementosRecycler.clear()

            // Agrupar por categoría, fusionando ARRIADO y ORACIÓN
            val agrupadosPorCategoria = listaToques.groupBy {
                if (it.categoria.uppercase() == "ARRIADO" || it.categoria.uppercase() == "ORACIÓN") {
                    "ARRIADO Y ORACIÓN"
                } else {
                    it.categoria.uppercase()
                }
            }

            agrupadosPorCategoria.toSortedMap().forEach { (categoria, toquesCategoria) ->
                elementosRecycler.add(categoria)

                val conMes = toquesCategoria.filter { it.mes != "sin mes" }
                val sinMes = toquesCategoria.filter { it.mes == "sin mes" }

                // Agrupamos los que tienen mes
                val agrupadosPorMes = conMes.groupBy { it.ordenMes }
                agrupadosPorMes.toSortedMap().forEach { (_, grupoMes) ->
                    elementosRecycler.addAll(
                        grupoMes.sortedWith(
                            compareBy({ prioridadNombre(it.nombre) }, { it.nombre.lowercase() })
                        )
                    )
                }

                // Añadimos al final los que no tienen mes
                if (sinMes.isNotEmpty()) {
                    elementosRecycler.addAll(
                        sinMes.sortedWith(
                            compareBy({ prioridadNombre(it.nombre) }, { it.nombre.lowercase() })
                        )
                    )
                }
            }

            adaptador.notifyDataSetChanged()
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Error al cargar los toques", Toast.LENGTH_SHORT).show()
            Log.e("MainActivity", "Error al cargar toques", exception)
        }
    }

    private fun prioridadNombre(nombre: String): Int {
        val nombreMin = nombre.lowercase()
        return when {
            "diario" in nombreMin -> 0
            "viernes" in nombreMin -> 1
            "festivo" in nombreMin -> 2
            else -> 3
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_nuevo_toque -> {
                startActivity(Intent(this, CrearToqueActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarCategoriasYMeses()
    }

    private fun verificarPermisoAlarmas() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:$packageName".toUri()
                }
                startActivity(intent)
                Toast.makeText(this, "Activa el permiso de alarmas exactas", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun verificarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permiso = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permiso) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permiso), 101)
            }
        }
    }
}
