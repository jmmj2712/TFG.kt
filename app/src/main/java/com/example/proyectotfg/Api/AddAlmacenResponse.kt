package com.example.proyectotfg.Api

/**
 * Datos devueltos por la API al añadir un almacén.
 *
 * @property status Estado de la operación ("success" o "error").
 * @property message Mensaje opcional con detalles o errores.
 */
data class AddAlmacenResponse(
    val status: String,
    val message: String?
)
