package com.enriquegomezlucena.cornetin

// Clase de datos que representa un mes específico en la colección "meses"
data class Mes(
    val id: String = "",         // ID del mes (Ej: enero, febrero...), en minúsculas
    val nombre: String = "",     // Nombre completo del mes con mayúscula inicial (Ej: Enero, Febrero...)
    val orden: Int = 99          // Orden cronológico (1 = Enero, 12 = Diciembre)
)
