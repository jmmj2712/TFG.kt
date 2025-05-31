package com.example.proyectotfg.Api

data class AddProductoRequest(
    val producto: String,
    val tamano: String,
    val precio: Double,
    val marca: String,
    val disponibles: Int,
    val almacen: Int,
    val cantidad: Int?,
    val fechaCaducidad: String?
)
