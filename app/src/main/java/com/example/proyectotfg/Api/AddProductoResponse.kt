package com.example.proyectotfg.Api

data class AddProductoResponse(
    val status: String,
    val message: String? = null,
    val newId: Int? = null,
    val existingId: Int? = null
)


