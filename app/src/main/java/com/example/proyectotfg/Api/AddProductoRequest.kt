package com.example.proyectotfg.Api

/**
 * Datos enviados a la API para crear un nuevo producto.
 *
 * @property producto     Nombre identificador del producto.
 * @property tamano        Tamaño o presentación (por ejemplo, "500ml", "XL").
 * @property precio        Precio unitario en la moneda correspondiente.
 * @property marca         Marca comercial del producto.
 * @property disponibles   Unidades actualmente disponibles en stock.
 * @property almacen       Identificador numérico del almacén donde se ubicará.
 * @property cantidad      Cantidad a añadir en esta operación (opcional).
 * @property fechaCaducidad Fecha de caducidad en formato ISO (yyyy-MM-dd), si aplica.
 */
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
