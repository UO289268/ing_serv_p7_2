package es.uniovi.amigos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver

class MainActivity : AppCompatActivity() {

    private var map: MapView? = null
    // Conectamos con el ViewModel automáticamente
    private val viewModel: MainViewModel by viewModels()

    // --- GESTIÓN DE PERMISOS ---
    // Esto maneja la respuesta del usuario cuando sale la ventanita de "¿Permitir GPS?"
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            Log.d("Permissions", "GPS Concedido. Preguntando nombre...")
            askUserName() // ¡Si nos da permiso, le pedimos el nombre!
        } else {
            Log.d("Permissions", "GPS Denegado. No podremos mover tu chincheta.")
        }
    }

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Si recibimos el aviso "updateFromServer"...
            if (intent?.action == "updateFromServer") {
                Log.d("MainActivity", "¡Aviso recibido! Recargando datos...")

                // ... LLAMAMOS A LA FUNCIÓN PARA QUE PIDA LOS DATOS NUEVOS
                viewModel.getAmigosList()
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Configuración del mapa (Igual que antes)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val ctx: Context = applicationContext
                Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
            }
            map = findViewById(R.id.map)
            map?.setTileSource(TileSourceFactory.MAPNIK)
            centrarMapaEnEuropa()
        }

        // 2. OBSERVER: Cada vez que el ViewModel reciba datos de amigos, pintamos
        viewModel.amigosList.observe(this) { listaDeAmigos ->
            paintAmigosList(listaDeAmigos)
        }

        // 3. ARRANQUE: Lo primero es comprobar los permisos
        checkAndRequestLocationPermissions()
    }

    // --- LÓGICA DE PERMISOS ---
    private fun checkAndRequestLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        // Verificamos si ya los tenemos de antes
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            Log.d("Permissions", "Ya tenía permisos. Preguntando nombre...")
            askUserName()
        } else {
            // Si no, los pedimos
            Log.d("Permissions", "No hay permisos. Solicitando...")
            requestPermissionLauncher.launch(permissions)
        }
    }

    // --- LÓGICA DE IDENTIFICACIÓN ---
    private fun askUserName() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Identificación")
        builder.setMessage("Escribe el nombre de tu usuario (ej: Manolito):")

        // Creamos un campo de texto por código
        val input = EditText(this)
        builder.setView(input)

        // Botón Aceptar
        builder.setPositiveButton("Entrar") { _, _ ->
            val name = input.text.toString()
            if (name.isNotBlank()) {
                // ¡Aquí ocurre la magia! Le pasamos el nombre al ViewModel
                viewModel.setUserName(name)
            }
        }

        // Evitamos que el usuario cierre el diálogo pulsando fuera
        builder.setCancelable(false)
        builder.show()
    }

    // --- LÓGICA DEL MAPA (Igual que antes) ---
    private fun paintAmigosList(amigos: List<Amigo>) {
        map?.overlays?.clear()
        for (amigo in amigos) {
            try {
                // Convertimos String a Double
                val lat = amigo.lati.toDouble()
                val lon = amigo.longi.toDouble()
                addMarker(lat, lon, amigo.name)
            } catch (e: Exception) {
                // Ignoramos coordenadas erróneas
            }
        }
        map?.invalidate()
    }

    private fun addMarker(latitud: Double, longitud: Double, name: String?) {
        map?.let { mapaNoNulo ->
            val coords = GeoPoint(latitud, longitud)
            val marker = Marker(mapaNoNulo)
            marker.position = coords
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            marker.title = name
            // Si pusiste el icono personalizado, descomenta esto:
            // marker.icon = ContextCompat.getDrawable(this, R.drawable.baseline_person_24)

            mapaNoNulo.overlays.add(marker)
        }
    }

    override fun onResume() { super.onResume();
        map?.onResume()
        val filter = IntentFilter("updateFromServer")
        registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }
    override fun onPause() { super.onPause();
        map?.onPause()
        unregisterReceiver(updateReceiver)
    }

    fun centrarMapaEnEuropa() {
        val mapController = map?.controller
        mapController?.setZoom(5.5)
        val startPoint = GeoPoint(48.8583, 2.2944) // París
        mapController?.setCenter(startPoint)
    }
}