package es.uniovi.amigos

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// CAMBIO IMPORTANTE: Heredamos de AndroidViewModel para poder usar 'application'
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Lista de amigos (para el Mapa)
    private val _amigosList = MutableLiveData<List<Amigo>>()
    val amigosList: LiveData<List<Amigo>> = _amigosList

    // Variables de Identidad (Quién soy yo)
    private var userName: String? = null
    var userId: Int? = null

    // El flujo de datos del GPS
    private val locationFlow = application.createLocationFlow()

    init {
        Log.d("MainViewModel", "Arrancando... iniciando descarga de amigos.")
        startPolling() // Empezamos a descargar chinchetas ajenas
    }

    // --- 1. IDENTIFICACIÓN: Preguntar al servidor quién soy ---
    fun setUserName(name: String) {
        userName = name
        Log.d("MainViewModel", "Usuario establecido: $name. Buscando ID...")

        viewModelScope.launch {
            try {
                // Buscamos el ID en el servidor usando el nombre
                val response = RetrofitClient.api.getAmigoByName(name)
                if (response.isSuccessful) {
                    val yo = response.body()
                    userId = yo?.id
                    Log.d("MainViewModel", "¡Identificado! Soy ID: $userId")

                    // En cuanto sé quién soy, enciendo el GPS
                    startLocationUpdates()
                } else {
                    Log.e("MainViewModel", "Usuario '$name' no encontrado en BD. Crea el usuario en la web primero.")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error buscando usuario: ${e.message}")
            }
        }
    }

    // --- 2. GPS: Escuchar movimiento y avisar al servidor ---
    private fun startLocationUpdates() {
        viewModelScope.launch {
            Log.d("GPS", "Iniciando escucha de GPS...")

            // Este bloque se queda "escuchando" para siempre
            locationFlow.collect { result ->
                if (result is LocationResult.NewLocation) {
                    val loc = result.location
                    // Log.d("GPS", "Me he movido a: ${loc.latitude}, ${loc.longitude}")

                    // TRUCO: Solo enviamos la posición si ya tenemos ID (userId no es null)
                    userId?.let { idNoNulo ->
                        updatePositionInServer(idNoNulo, loc.latitude, loc.longitude)
                    }

                } else if (result is LocationResult.ProviderDisabled) {
                    Log.w("GPS", "El GPS está desactivado")
                }
            }
        }
    }

    // Enviar el PUT al servidor
    private fun updatePositionInServer(id: Int, lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val payload = LocationPayload(lat.toString(), lon.toString())
                RetrofitClient.api.updateAmigoPosition(id, payload)
                Log.d("API", "Posición enviada al servidor (Lat: $lat, Lon: $lon)")
            } catch (e: Exception) {
                Log.e("API", "Error actualizando posición: ${e.message}")
            }
        }
    }

    // --- 3. POLLING: Descargar amigos cada 5s (Igual que antes) ---
    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                getAmigosList()
                delay(5000)
            }
        }
    }

    private fun getAmigosList() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getAmigos()
                if (response.isSuccessful) {
                    // Usamos postValue para evitar errores de hilo
                    _amigosList.postValue(response.body())
                }
            } catch (e: Exception) { Log.e("Error", "Error polling: ${e.message}") }
        }
    }
}