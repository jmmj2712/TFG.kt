package com.example.proyectotfg.Api

import com.example.proyectotfg.Model.Producto
import com.example.proyectotfg.Model.Almacen
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    /**
     * Obtiene la lista de productos, opcionalmente filtrada por marca.
     *
     * @param marca Nombre de la marca para filtrar los productos.
     * @return Call con la lista de productos obtenida de la API.
     */
    @GET("api/getProductos.php")
    fun getProductosPorMarca(
        @Query("marca") marca: String
    ): Call<List<Producto>>

    /**
     * Recupera los registros de stock combinando las tablas `almacen` y `productos`.
     *
     * @return Call con la lista de almacenes y sus productos.
     */
    @GET("api/getAlmacen.php")
    fun getAlmacen(): Call<List<Almacen>>

    /**
     * Añade un nuevo producto al sistema.
     *
     * El cuerpo de la petición debe coincidir con AddProductoRequest.
     *
     * @param req Objeto que contiene los datos del producto a crear.
     * @return Call con la respuesta AddProductoResponse, donde se indica
     *         si fue creado o ya existía (y su ID).
     */
    @POST("api/addProducto.php")
    fun addProducto(
        @Body req: AddProductoRequest
    ): Call<AddProductoResponse>

    /**
     * Añade fechas de caducidad a un producto ya existente en inventario.
     *
     * Útil cuando el producto ya existe y solo se quieren gestionar nuevas fechas.
     *
     * @param req Objeto con el ID de producto y la/s fecha/s a añadir.
     * @return Call con la respuesta AddAlmacenResponse indicando éxito o error.
     */
    @POST("api/addAlmacen.php")
    fun addAlmacen(
        @Body req: AddAlmacenRequest
    ): Call<AddAlmacenResponse>

    /**
     * Elimina un producto por su identificador.
     *
     * @param id ID único del producto a eliminar.
     * @return Call con AddProductoResponse indicando resultado de la operación.
     */
    @FormUrlEncoded
    @POST("api/deleteProducto.php")
    fun deleteProducto(
        @Field("id") id: Int
    ): Call<AddProductoResponse>

    /**
     * Actualiza el estado de disponibilidad de un producto.
     *
     * @param id            ID del producto a modificar.
     * @param disponibles   Nuevo valor del campo 'Disponibles' (0 o 1).
     * @return Call con AddProductoResponse con el estado de la actualización.
     */
    @FormUrlEncoded
    @POST("api/available.php")
    fun available(
        @Field("id") id: Int,
        @Field("disponibles") disponibles: Int
    ): Call<AddProductoResponse>

    /**
     * Edita todos los datos de un producto existente, incluyendo stock.
     *
     * @param id             ID del producto.
     * @param producto       Nuevo nombre o código del producto.
     * @param tamano         Tamaño o presentación (e.g., "500ml").
     * @param precio         Precio unitario actualizado.
     * @param marca          Marca asociada.
     * @param disponibles    Unidades disponibles en stock.
     * @param almacen        ID del almacén de almacenamiento.
     * @param cantidad       Cantidad a ajustar en esta operación (opcional).
     * @param fechaCaducidad Fecha de caducidad (ISO yyyy-MM-dd), si aplica.
     * @return Call con AddProductoResponse indicando éxito o error.
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

    /**
     * Disminuye en una unidad el stock de un producto consumido.
     *
     * @param id ID del producto que se va a consumir.
     * @return Call con ConsumeResponse que indica el nuevo stock o error.
     */
    @FormUrlEncoded
    @POST("api/consumeProduct.php")
    fun consumeProduct(
        @Field("id") id: Int
    ): Call<ConsumeResponse>
}