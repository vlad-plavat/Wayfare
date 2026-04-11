package com.plavatvlad.wayfare.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.utils.StatusMonitor
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class MapActivity : AppCompatActivity() {

    private lateinit var map: MapView

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

        val mapController = map.controller
        mapController.setZoom(12.0)
        mapController.setCenter(GeoPoint(44.4268, 26.1025)) // Bucharest
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

}