package com.plavatvlad.wayfare.utils

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.view.View
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.*

object StatusMonitor {

    private lateinit var appContext: Context
    private var job: Job? = null


    private var lastLocation: Location? = null
    private var isGpsLocked = false
    var listener: ((Status) -> Unit)? = null

    data class Status(
        val gpsEnabled: Boolean,
        val gpsLocked: Boolean,
        val gpsAccuracy: Float?,
        val location: Location?,
        val internetQuality: Int?
    )

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startGpsTracking() {

        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener = object : LocationListener {

            override fun onLocationChanged(location: Location) {
                lastLocation = location
                isGpsLocked = (System.currentTimeMillis() - location.time < 5000)
            }

            override fun onProviderDisabled(provider: String) {
                isGpsLocked = false
            }
        }

        try {
            // 📍 GPS (high accuracy)
            lm.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                locationListener
            )

            // 📡 Network (fast fallback, VERY important)
            lm.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000L,
                0f,
                locationListener
            )

        } catch (e: Exception) {
            Log.e("STATUS", "Location error", e)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun start() {
        if (job != null) return

        startGpsTracking()

        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {

                val gpsEnabled = isGpsEnabled()
                val net = getInternetQuality()

                val status = Status(
                    gpsEnabled = gpsEnabled,
                    gpsLocked = isGpsLocked,
                    gpsAccuracy = lastLocation?.accuracy,
                    location = lastLocation,
                    internetQuality = net
                )

                Log.d("STATUS", status.toString())

                listener?.invoke(status)

                delay(1000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    // 🔵 GPS
    private fun isGpsEnabled(): Boolean {
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    // 🌐 Internet quality
    private fun getInternetQuality(): Int {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return -2
        val caps = cm.getNetworkCapabilities(network) ?: return -2

        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return -1
        }

        val downKbps = caps.linkDownstreamBandwidthKbps
        Log.d("STATUS", " NET: $downKbps")

        return downKbps
    }


}