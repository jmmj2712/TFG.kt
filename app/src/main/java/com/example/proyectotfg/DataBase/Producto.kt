package com.example.proyectotfg.DataBase

import com.google.gson.annotations.SerializedName

data class Producto(
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
    val almacen: Int
)
