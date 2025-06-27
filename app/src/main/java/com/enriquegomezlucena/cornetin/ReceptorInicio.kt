package com.enriquegomezlucena.cornetin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

// Receptor que se ejecuta automáticamente tras reiniciar el dispositivo
// y vuelve a programar las alarmas de los toques activos
class ReceptorInicio : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        // Acciones compatibles con reinicio del sistema
        val accion = intent?.action

        if (accion == Intent.ACTION_BOOT_COMPLETED ||
            accion == "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
            Log.i("ReceptorInicio", "Dispositivo reiniciado. Reprogramando alarmas...")
            GestorAlarmas.programarAlarmas(context)
        } else {
            Log.w("ReceptorInicio", "Intent no reconocido: $accion")
        }
    }
}
