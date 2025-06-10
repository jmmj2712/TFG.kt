package com.example.proyectotfg.Model

import com.google.gson.annotations.SerializedName

/**
 * Representa un registro de inventario resultante de la unión entre tablas `productos` y `almacen`.
 *
 * @property id             Identificador único del registro de inventario.
 * @property producto       Nombre o código del producto.
 * @property tamano         Tamaño o presentación del producto (e.g., "500ml").
 * @property precio         Precio unitario del producto.
 * @property marca          Marca comercial asociada al producto.
 * @property disponibles    Unidades actualmente disponibles en stock.
 * @property almacen        Identificador del almacén donde está ubicado este stock.
 * @property cantidad       Cantidad ajustada en la operación (puede ser nulo si no aplica).
 * @property fechaCaducidad Fecha de caducidad en formato ISO (dd/MM/yyyy), nula si no tiene.
 */
data class Almacen(
    @SerializedName("ID")
    val id: Int,

    @SerializedName("Producto")
    val producto: String,

    @SerializedName("Tamano")
    val tamano: String,

    @SerializedName("Precio")
    val precio: Double,

    @SerializedName("Marca")
    val marca: String,

    @SerializedName("Disponibles")
    val disponibles: Int,

    @SerializedName("Almacen")
    val almacen: Int,

    @SerializedName("Cantidad")
    val cantidad: Int?,

    @SerializedName("FechaCaducidad")
    val fechaCaducidad: String?
)
