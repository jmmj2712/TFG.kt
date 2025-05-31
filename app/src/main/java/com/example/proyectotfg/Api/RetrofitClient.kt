package com.example.proyectotfg.Api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder

object RetrofitClient {
    //private const val BASE_URL = "http://192.168.1.233/"
    private const val BASE_URL = "http://192.168.18.178/"

    val instance: ApiService by lazy {
        // 1. Crea un interceptor para loguear cuerpo de peticiones y respuestas
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        // 2. Configura GSON lenient por si hay pequeñas inconsistencias
        val gson = GsonBuilder()
            .setLenient()
            .create()

        // 3. Construye Retrofit con el interceptor y GSON
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}
