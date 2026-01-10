package es.uniovi.amigos

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

// Clases para manejar el resultado (Ubicación nueva, error o proveedor desactivado)
sealed class LocationResult {
    data class NewLocation(val location: Location) : LocationResult()
    object PermissionDenied : LocationResult()
    object ProviderDisabled : LocationResult()
}

// Función de extensión que añade 'createLocationFlow' al Contexto de la app
@SuppressLint("MissingPermission")
fun Context.createLocationFlow(): kotlinx.coroutines.flow.Flow<LocationResult> {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Comprobamos si el GPS está encendido
    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    if (!isGpsEnabled) {
        return kotlinx.coroutines.flow.flowOf(LocationResult.ProviderDisabled)
    }

    // Creamos el Flow
    return callbackFlow {
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // ¡Éxito! Enviamos la nueva ubicación al flujo
                trySend(LocationResult.NewLocation(location))
            }
            override fun onProviderDisabled(provider: String) {
                trySend(LocationResult.ProviderDisabled)
            }
            // Estos métodos son obligatorios pero no los usaremos
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }

        // Configuramos: GPS, mínimo 5 segundos, mínimo 10 metros
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            5000L,
            10f,
            locationListener
        )

        // Cuando el ViewModel se cierre, esto limpiará el listener para no gastar batería
        awaitClose {
            locationManager.removeUpdates(locationListener)
        }
    }
}