package com.enriquegomezlucena.cornetin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

// Worker que lanza la notificación push-up 5 minutos antes del toque
class NotificacionWorker(
    context: Context, params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val nombreToque = inputData.getString("nombre_toque") ?: "Toque"
        crearCanalNotificacion()

        val mensaje = "En 5 minutos: $nombreToque"

        // Intent para abrir MainActivity al pulsar la notificación
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder =
            NotificationCompat.Builder(applicationContext, "canal_notificaciones_cornetin")
                .setSmallIcon(R.mipmap.ic_launcher) // Usa el icono de la app
                .setContentTitle("Toque programado").setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setAutoCancel(true)
                .setContentIntent(pendingIntent) // Abrir app al tocarla

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(nombreToque.hashCode(), builder.build())

        vibrarDispositivo()

        Log.i("NotificacionWorker", "Notificación enviada para $nombreToque")
        return Result.success()
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                "canal_notificaciones_cornetin",
                "Notificaciones Cornetín",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios previos a los toques"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(canal)
        }
    }

    private fun vibrarDispositivo() {
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION") applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val efecto = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(efecto)
        } else {
            @Suppress("DEPRECATION") vibrator.vibrate(500)
        }
    }
}
