package es.uniovi.amigos

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels // Necesario para 'by viewModels()'
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : AppCompatActivity() {

    private var map: MapView? = null

    // Conectamos con el ViewModel automáticamente
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Configuración del mapa (esto ya lo tenías)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val ctx: Context = applicationContext
                Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
            }
            map = findViewById(R.id.map)
            map?.setTileSource(TileSourceFactory.MAPNIK)

            centrarMapaEnEuropa()
        }

        // 2. EL OBSERVADOR (La magia del MVVM)
        // Cada vez que el ViewModel reciba datos nuevos (cada 5s), se ejecuta esto:
        viewModel.amigosList.observe(this) { listaDeAmigos ->
            Log.d("MainActivity", "¡Nuevos datos! Pintando ${listaDeAmigos.size} amigos")
            paintAmigosList(listaDeAmigos)
        }
    }

    // Función para limpiar y repintar todas las chinchetas
    private fun paintAmigosList(amigos: List<Amigo>) {
        // Borramos las anteriores para no duplicarlas
        map?.overlays?.clear()

        for (amigo in amigos) {
            // Convertimos el texto (String) del servidor a números (Double)
            try {
                val lat = amigo.lati.toDouble()
                val lon = amigo.longi.toDouble()
                addMarker(lat, lon, amigo.name)
            } catch (e: NumberFormatException) {
                Log.e("MainActivity", "Coordenadas incorrectas para ${amigo.name}")
            }
        }

        // Obligamos al mapa a redibujarse
        map?.invalidate()
    }

    // Función segura para añadir un marcador
    private fun addMarker(latitud: Double, longitud: Double, name: String?) {
        // Usamos map?.let para evitar errores si el mapa aún no ha cargado
        map?.let { mapaNoNulo ->
            val coords = GeoPoint(latitud, longitud)
            val marker = Marker(mapaNoNulo)
            marker.position = coords
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            marker.title = name
            // marker.icon = ... (Si quieres cambiar el icono, hazlo aquí)

            mapaNoNulo.overlays.add(marker)
        }
    }

    // --- Ciclo de vida del mapa ---
    override fun onResume() {
        super.onResume()
        map?.onResume()
    }

    override fun onPause() {
        super.onPause()
        map?.onPause()
    }

    fun centrarMapaEnEuropa() {
        val mapController = map?.controller
        mapController?.setZoom(5.5)
        val startPoint = GeoPoint(48.8583, 2.2944) // París
        mapController?.setCenter(startPoint)
    }
}