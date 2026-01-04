package es.uniovi.amigos

import android.content.Context
import android.os.Bundle
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class MainActivity : AppCompatActivity() {
    private var map: MapView? = null // Referencia al objeto MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Cargar el layout
        setContentView(R.layout.activity_main)

        // 2. Leer la configuración de la aplicación e inicializar mapa
        // Esta es una operación de E/S en disco, para no bloquear el hilo GUI lo lanzamos
        // en un hilo separado, via una corutina
        lifecycleScope.launch {
            // Dentro de la corutina, usamos un hilo del pool de hilos I/O
            withContext(Dispatchers.IO) {
                val ctx: Context = applicationContext
                Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
            }
            // Una vez cargada la configuración, inicializamos el mapa
            map = findViewById(R.id.map)
            map?.setTileSource(TileSourceFactory.MAPNIK)

            centrarMapaEnEuropa()
        }
    }

    override fun onResume() {
        super.onResume()
        map?.onResume()
    }

    override fun onPause() {
        super.onPause()
        map?.onPause()
    }

    fun centrarMapaEnEuropa() {
        // Esta función mueve el centro del mapa a Paris y ajusta el zoom
        // para que se vea Europa
        val mapController = map?.controller
        mapController?.setZoom(5.5)
        val startPoint = GeoPoint(48.8583, 2.2944)
        mapController?.setCenter(startPoint)
    }
}