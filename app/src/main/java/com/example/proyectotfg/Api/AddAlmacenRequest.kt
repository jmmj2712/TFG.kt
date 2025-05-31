package com.example.proyectotfg.Api

data class AddAlmacenRequest(
    val producto_id: Int,
    val cantidad: Int,
    val fechaCaducidad: String
)
