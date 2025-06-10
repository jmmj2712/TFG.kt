package com.example.proyectotfg.Api

/**
 * Respuesta de la API al consumir (usar) una unidad de producto.
 *
 * @property status   Código de estado de la operación (por ejemplo, "ok" o "error").
 * @property message  Mensaje opcional con detalles o razón de error.
 * @property newCount Nueva cantidad disponible tras la operación, si aplica.
 * @property nextDate Fecha estimada de reposición o próxima caducidad, en formato ISO (yyyy-MM-dd), si procede.
 */
data class ConsumeResponse(
    val status: String?,
    val message: String?,
    val newCount: Int?,
    val nextDate: String?
)
