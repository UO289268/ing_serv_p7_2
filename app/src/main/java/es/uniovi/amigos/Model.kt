package es.uniovi.amigos

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// 1. LA FORMA DE LOS DATOS (Data Class)
// Debe coincidir EXACTAMENTE con las claves de tu JSON del servidor Flask ('name', 'lati', 'longi')
data class Amigo(
    val name: String,
    val lati: String,
    val longi: String
    // No necesitamos 'id' ni 'device' para pintar el mapa, así que los omitimos
)

// 2. EL CONTRATO (Interfaz API)
// Aquí definimos qué operaciones podemos hacer con el servidor
interface AmigosApiService {
    @GET("api/amigos") // La ruta relativa (se suma a la URL base)
    suspend fun getAmigos(): Response<List<Amigo>>
}

// 3. EL CLIENTE (Singleton)
// El objeto que gestiona la conexión real
object RetrofitClient {
    private const val BASE_URL = "https://noneducatory-chicly-jefferson.ngrok-free.dev"

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: AmigosApiService by lazy {
        retrofit.create(AmigosApiService::class.java)
    }
}