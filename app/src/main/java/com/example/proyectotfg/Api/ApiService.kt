package com.example.proyectotfg.Api

import com.example.proyectotfg.DataBase.Producto
import com.example.proyectotfg.DataBase.Almacen
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @GET("api/getProductos.php")
    fun getProductosPorMarca(@Query("marca") marca: String): Call<List<Producto>>

    @GET("api/getAlmacen.php")
    fun getAlmacen(@Query("marca") marca: String? = null): Call<List<Almacen>>

    @POST("api/addProducto.php")
    fun addProducto(@Body req: AddProductoRequest): Call<AddProductoResponse>

    @FormUrlEncoded
    @POST("api/deleteProducto.php")
    fun deleteProducto(
        @Field("id") id: Int
    ): Call<AddProductoResponse>

    @FormUrlEncoded
    @POST("api/available.php")
    fun available(
        @Field("id") id: Int,
        @Field("disponibles") disponibles: Int
    ): Call<AddProductoResponse>

    /** Método completo para editar producto, si lo necesitas aún */
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
