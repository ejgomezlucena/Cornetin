package com.enriquegomezlucena.cornetin

// Clase de datos que representa un toque almacenado en Firebase Firestore
data class Toques(
    val nombre: String = "",         // Nombre del toque (Ej: Diana, Silencio, etc.)
    val hora: String = "",           // Hora en formato HH:mm
    val repeticion: String = "",     // Días de repetición
    val categoria_id: String = "",   // ID de la categoría (referencia)
    val mes_id: String = "",         // ID del mes (referencia)
    val sonido: String = "",         // Nombre del sonido o ruta externa
    val activo: Boolean = true       // ¿Está activo el toque?
) {
    // Estos campos no se guardan en Firebase, los usa la app para mostrar datos legibles
    var categoria: String = ""
    var mes: String = ""
    var ordenMes: Int = 99
}
