package com.example.proyectotfg.Model

import com.google.gson.annotations.SerializedName

/**
 * Representa un producto disponible en el sistema.
 *
 * Esta clase modela los datos esenciales de un producto obtenidos desde la API,
 * incluyendo identificación, características y asociación con un almacén.
 *
 * @property id           Identificador único del producto.
 * @property producto     Nombre o descripción del producto.
 * @property tamano       Categoría de tamaño del producto (por ejemplo, "Grande" o "Pequeña").
 * @property precio       Precio unitario en la moneda definida.
 * @property marca        Marca comercial del producto.
 * @property disponibles  Unidades disponibles actualmente en stock.
 * @property almacen      ID del almacén donde reside el stock.
 */
data class Producto(
    @SerializedName("ID")
    val id: Int = 0,

    @SerializedName("Producto")
    val producto: String = "",

    @SerializedName("Tamano")
    val tamano: String = "",

    @SerializedName("Precio")
    val precio: Double = 0.0,

    @SerializedName("Marca")
    val marca: String = "",

    @SerializedName("Disponibles")
    val disponibles: Int = 0,

    @SerializedName("Almacen")
    val almacen: Int = 0
)

