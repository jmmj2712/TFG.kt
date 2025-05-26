package com.example.proyectotfg.Api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import com.example.proyectotfg.DataBase.Producto
import com.example.proyectotfg.DataBase.Almacen
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded

interface ApiService {
    @GET("api/getProductos.php")
    fun getProductosPorMarca(@Query("marca") marca: String): Call<List<Producto>>

    @GET("api/getAlmacen.php")
    fun getAlmacen(@Query("marca") marca: String? = null): Call<List<Almacen>>

    @POST("api/addProducto.php")
    fun addProducto(@Body req: AddProductoRequest): Call<AddProductoResponse>

    @POST("api/deleteProducto.php")
    @FormUrlEncoded
    fun deleteProducto(
        @Field("id") id: Int
    ): Call<AddProductoResponse>

    @FormUrlEncoded
    @POST("api/editProducto.php")
    fun editProducto(
        @Field("id")             id: Int,
        @Field("producto")       producto: String,
        @Field("tamano")         tamano: String,
        @Field("precio")         precio: Double,
        @Field("marca")          marca: String,
        @Field("disponibles")    disponibles: Int,
        @Field("almacen")        almacen: Int,
        @Field("cantidad")       cantidad: Int?,
        @Field("fechaCaducidad") fechaCaducidad: String?
    ): Call<AddProductoResponse>
}

