package com.enriquegomezlucena.cornetin

// Clase de datos que representa una categoría de toques en Firebase Firestore
data class Categoria(
    val id: String = "",         // ID único de la categoría (coincide con el document ID)
    val nombre: String = ""      // Nombre legible (Ej: Diana, Fajina, Arriado...)
)
