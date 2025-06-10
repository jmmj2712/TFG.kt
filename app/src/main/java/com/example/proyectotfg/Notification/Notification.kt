package com.example.proyectotfg.Notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.proyectotfg.Model.Almacen
import com.example.proyectotfg.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Worker encargado de enviar una notificación cuando un producto está próximo a caducar.
 *
 * Obtiene el nombre del producto y su fecha de caducidad de los datos de entrada,
 * crea un canal (si es necesario) y lanza la notificación con la información correspondiente.
 */
class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    /**
     * Se ejecuta en segundo plano para construir y mostrar la notificación.
     *
     * @return Result.success() si la notificación se envía, Result.failure() si faltan datos.
     */
    override fun doWork(): Result {
        // Recupera parámetros de entrada o falla si no existen
        val producto = inputData.getString(KEY_PRODUCTO) ?: return Result.failure()
        val fechaCad = inputData.getString(KEY_FECHA) ?: return Result.failure()

        // Gestor de notificaciones del sistema
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "recordatorio_caducidad"
        // A partir de Android O, se requiere canal de notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios Caducidad",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Construye la notificación mostrando título, texto e icono
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Producto a punto de caducar")
            .setContentText("El producto \"$producto\" caduca el $fechaCad")
            .setSmallIcon(R.drawable.ic_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Muestra la notificación usando hash del nombre como ID única
        notificationManager.notify(producto.hashCode(), notification)
        return Result.success()
    }

    companion object {
        // Claves para pasar datos al Worker
        private const val KEY_PRODUCTO = "PRODUCTO"
        private const val KEY_FECHA    = "FECHA"

        /**
         * Programa un recordatorio 2 semanas antes de la fecha de caducidad.
         *
         * @param context Contexto de la aplicación para acceder a WorkManager.
         * @param item    Registro de almacen que contiene nombre y fecha de caducidad.
         */
        fun programar(
            context: Context,
            item: Almacen
        ) {
            // Parseamos la fecha de caducidad usando formato dd/MM/yyyy
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaDate = sdf.parse(item.fechaCaducidad ?: return) ?: return

            // Calculamos el tiempo de disparo restando 14 días en milisegundos
            val triggerTime = fechaDate.time - TimeUnit.DAYS.toMillis(14)
            val delay = triggerTime - System.currentTimeMillis()
            if (delay <= 0) return  // Si ya pasó o está muy próximo, no programar

            // Preparamos los datos de entrada para el Worker
            val data = Data.Builder()
                .putString(KEY_PRODUCTO, item.producto)
                .putString(KEY_FECHA, item.fechaCaducidad)
                .build()

            // Creamos la tarea de WorkManager con retraso calculado
            val request = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            // Encolamos la tarea
            androidx.work.WorkManager
                .getInstance(context)
                .enqueue(request)
        }
    }
}