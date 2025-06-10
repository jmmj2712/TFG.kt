package com.example.proyectotfg.Api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder

/**
 * Singleton que configura y provee la instancia de Retrofit para las llamadas a la API.
 *
 * Utiliza OkHttp con un interceptor de logging para depuración y GSON con lenient parsing
 * para manejar respuestas JSON con posibles inconsistencias.
 */
object RetrofitClient {
    /**
     * URL base de la API. Cambiar el valor según la red local o entorno.
     */
    private const val BASE_URL = "http://192.168.1.233/"
    // Alternativa local: http://192.168.18.178/

    /**
     * Instancia perezosa de ApiService configurada con Retrofit.
     */
    val instance: ApiService by lazy {
        // 1. Interceptor para registrar cuerpo de peticiones y respuestas HTTP
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 2. Cliente HTTP de OkHttp con el interceptor de logging
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        // 3. Configuración de GSON tolerante a pequeños errores en JSON
        val gson = GsonBuilder()
            .setLenient()
            .create()

        // 4. Construcción de Retrofit usando la URL base, el cliente OkHttp y el convertidor GSON
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
