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
import com.example.proyectotfg.DataBase.Almacen
import com.example.proyectotfg.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val producto = inputData.getString(KEY_PRODUCTO) ?: return Result.failure()
        val fechaCad = inputData.getString(KEY_FECHA) ?: return Result.failure()

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "recordatorio_caducidad"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios Caducidad",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Producto a punto de caducar")
            .setContentText("El producto \"$producto\" caduca el $fechaCad")
            .setSmallIcon(R.drawable.ic_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(producto.hashCode(), notification)
        return Result.success()
    }

    companion object {
        private const val KEY_PRODUCTO = "PRODUCTO"
        private const val KEY_FECHA    = "FECHA"

        /**
         * Programa un recordatorio a 2 semanas antes de la fecha de caducidad.
         */
        fun programar(
            context: Context,
            item: Almacen
        ) {
            // Parseamos la fecha en formato dd/MM/yyyy
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaDate = sdf.parse(item.fechaCaducidad ?: return) ?: return

            // Calculamos dos semanas antes
            val triggerTime = fechaDate.time - TimeUnit.DAYS.toMillis(14)
            val delay = triggerTime - System.currentTimeMillis()
            if (delay <= 0) return  // ya pasó o está muy cerca

            // Construimos los datos de entrada
            val data = Data.Builder()
                .putString(KEY_PRODUCTO, item.producto)
                .putString(KEY_FECHA, item.fechaCaducidad)
                .build()

            // Creamos la tarea de WorkManager apuntando a este Worker
            val request = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()

            androidx.work.WorkManager
                .getInstance(context)
                .enqueue(request)
        }
    }
}
