package es.uniovi.amigos

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // _amigosList es privada y mutable (nosotros escribimos aquí)
    private val _amigosList = MutableLiveData<List<Amigo>>()

    // amigosList es pública e inmutable (la Activity lee de aquí)
    val amigosList: LiveData<List<Amigo>> = _amigosList

    init {
        Log.d("MainViewModel", "ViewModel creado. Iniciando polling...")
        startPolling() // Arrancamos el bucle automático al crear el ViewModel
    }

    // Ejercicio 8: Polling (Preguntar cada 5 segundos)
    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                Log.d("Polling", "Pidiendo lista de amigos...")
                getAmigosList()
                delay(5000) // Pausa de 5 segundos
            }
        }
    }

    // Petición a Retrofit y actualización del LiveData
    // Petición a Retrofit y actualización del LiveData
    private fun getAmigosList() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.api.getAmigos()
                if (response.isSuccessful) {
                    // Si body() no es nulo, entra aquí y 'it' es la lista segura
                    response.body()?.let { listaSegura ->
                        _amigosList.value = listaSegura
                        Log.d("MainViewModel", "Recibidos ${listaSegura.size} amigos")
                    }
                }
                // ... (resto de catch y else)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error: ${e.message}")
            }
        }
    }
}