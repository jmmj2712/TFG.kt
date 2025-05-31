package com.example.proyectotfg.Api

import com.example.proyectotfg.DataBase.Producto
import com.example.proyectotfg.DataBase.Almacen
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    /**
     * Devuelve todos los productos (filtrados por marca si se pasa el parámetro).
     * El JSON esperado por retrofit se mapeará a List<Producto>.
     */
    @GET("api/getProductos.php")
    fun getProductosPorMarca(
        @Query("marca") marca: String
    ): Call<List<Producto>>

    /**
     * Devuelve todos los registros de stock (tabla `almacen` JOIN con `productos`).
     * Ya no usamos marca aquí: el endpoint ignora el parámetro.
     */
    @GET("api/getAlmacen.php")
    fun getAlmacen(): Call<List<Almacen>>

    /**
     * Inserta un nuevo producto. El cuerpo JSON coincide con la data class AddProductoRequest.
     * Responde AddProductoResponse (que normalmente contendrá { status: "ok", newId: X } u objeto de error).
     */
    @POST("api/addProducto.php")
    fun addProducto(
        @Body req: AddProductoRequest
    ): Call<AddProductoResponse>

    /**
     * Elimina un producto dado su ID. Retorna AddProductoResponse con status ok/error.
     */
    @FormUrlEncoded
    @POST("api/deleteProducto.php")
    fun deleteProducto(
        @Field("id") id: Int
    ): Call<AddProductoResponse>

    /**
     * Cambia el campo `Disponibles` de un producto (0 o 1).
     * También retorna AddProductoResponse (status ok o error).
     */
    @FormUrlEncoded
    @POST("api/available.php")
    fun available(
        @Field("id") id: Int,
        @Field("disponibles") disponibles: Int
    ): Call<AddProductoResponse>

    /**
     * Edita por completo la información de un producto (incluyendo stock/almacén).
     * El endpoint PHP actualizará tanto la tabla productos como la tabla almacen según convenga.
     */
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