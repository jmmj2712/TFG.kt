package com.example.proyectotfg.Model

import com.google.gson.annotations.SerializedName

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
