package es.uniovi.amigos

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

// 1. Añadimos el campo 'id' (Antes no lo usábamos, ahora es obligatorio)
data class Amigo(
    val id: Int,
    val name: String,
    val lati: String,
    val longi: String
)

// 2. Nueva clase 'paquete' para enviar solo las coordenadas al servidor
data class LocationPayload(
    val lati: String,
    val longi: String
)

data class DeviceTokenPayload(val device: String)

// 3. Nuevos métodos en la API
interface AmigosApiService {
    // Obtener toda la lista (lo que ya tenías)
    @GET("api/amigos")
    suspend fun getAmigos(): Response<List<Amigo>>

    // NUEVO: Buscar tu propio usuario por nombre (ej: "Manolito")
    @GET("api/amigo/byName/{name}")
    suspend fun getAmigoByName(@Path("name") name: String): Response<Amigo>

    // NUEVO: Actualizar tu posición en el servidor
    @PUT("api/amigo/{id}")
    suspend fun updateAmigoPosition(
        @Path("id") amigoId: Int,
        @Body payload: LocationPayload
    ): Response<Amigo>

    @PUT("/api/amigo/{id}")
    suspend fun updateAmigoDeviceToken(@Path("id") id: Int, @Body data: DeviceTokenPayload)
}

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