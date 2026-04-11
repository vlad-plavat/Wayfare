package com.plavatvlad.wayfare.ui

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.utils.StatusMonitor
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import androidx.core.graphics.toColorInt
import org.osmdroid.views.overlay.Marker

class MapActivity : AppCompatActivity() {

    private lateinit var map: MapView
    lateinit var gpsBubble: View
    lateinit var netBubble: View
    lateinit var gpsIcon: ImageView
    lateinit var netIcon: ImageView
    private var userMarker: Marker? = null

    private var lastGPSTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osm", MODE_PRIVATE)
        )

        setContentView(R.layout.activity_map)
        StatusMonitor.init(applicationContext)


        map = findViewById(R.id.map)
        map.setMultiTouchControls(true)

        gpsBubble = findViewById(R.id.gpsBubble)
        netBubble = findViewById(R.id.netBubble)
        gpsIcon = findViewById(R.id.gpsIcon)
        netIcon = findViewById(R.id.netIcon)

        val mapController = map.controller
        mapController.setZoom(12.0)
        mapController.setCenter(GeoPoint(44.4268, 26.1025)) // Bucharest

        userMarker = Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = ContextCompat.getDrawable(applicationContext, R.drawable.blue_dot)
            infoWindow = null
        }
        map.overlays.add(userMarker)

        StatusMonitor.listener = { status ->
            runOnUiThread {

                // GPS color
                val gpsColor: Int = when {
                    !status.gpsLocked -> android.graphics.Color.RED
                    (status.gpsAccuracy ?: 999f) < 10 -> android.graphics.Color.GREEN
                    (status.gpsAccuracy ?: 999f) < 20 -> android.graphics.Color.YELLOW
                    else -> "#EF4F00".toColorInt()
                }

                setBubbleColor(gpsBubble, gpsColor)

                // NET color
                val speedKbps = status.internetQuality ?: 0

                val netColor = when {
                    speedKbps >= 5000 -> android.graphics.Color.GREEN      // ~5 Mbps+ (very good)
                    speedKbps >= 1500 -> android.graphics.Color.YELLOW     // ~1.5 Mbps+ (usable)
                    speedKbps >= 300 -> android.graphics.Color.rgb(255, 165, 0) // orange (weak)
                    else -> android.graphics.Color.RED                     // very poor
                }

                setBubbleColor(netBubble, netColor)

                //GPS position
                val lat = status.location?.latitude ?: return@runOnUiThread
                val lon = status.location.longitude
                val accuracy = status.gpsAccuracy ?: 0f

                val point = GeoPoint(lat, lon)

                // ---- update marker ----
                if (userMarker == null) {
                    userMarker = Marker(map).apply {
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = ContextCompat.getDrawable(applicationContext, R.drawable.blue_dot)
                    }
                    map.overlays.add(userMarker)
                }
                userMarker?.position = point

                if(status.gpsLocked) {
                    if(System.currentTimeMillis() - lastGPSTime > 10000){
                        map.controller.setZoom(18.0)
                        map.controller.animateTo(userMarker?.position)
                    }
                    lastGPSTime = System.currentTimeMillis()
                }
            }

        }




    }


    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onStart() {
        super.onStart()
        StatusMonitor.start()
    }

    override fun onStop() {
        super.onStop()
        StatusMonitor.stop()
    }

    fun setBubbleColor(view: View, color: Int) {
        val drawable = view.background.mutate()
        drawable.setTint(color)
    }
}