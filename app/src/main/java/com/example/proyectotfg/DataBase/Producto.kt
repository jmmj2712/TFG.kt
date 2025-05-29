package com.example.proyectotfg.DataBase

import com.google.gson.annotations.SerializedName

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
