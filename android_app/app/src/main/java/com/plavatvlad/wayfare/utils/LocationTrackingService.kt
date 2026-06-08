package com.plavatvlad.wayfare.utils
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.plavatvlad.wayfare.R
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private const val GPS_SAFETY_INTERVAL = 3_000L

class LocationTrackingService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private var callback: LocationCallback? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        Log.d("LocationTrackingService", "Creating service")

        fusedClient = LocationServices.getFusedLocationProviderClient(this)

    }

    private fun buildNotification(): Notification {

        return NotificationCompat.Builder(this, "tracking_channel")
            .setContentTitle("Safety tracking active")
            .setContentText("Your location is being saved")
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startLocationUpdates() {

        Log.d("LocationTrackingService", "Starting location updates")
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            GPS_SAFETY_INTERVAL
        )
            .setMinUpdateIntervalMillis(GPS_SAFETY_INTERVAL)
            .build()

        val newCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach {
                    saveToFirestore(it)
                }
            }
        }

        callback = newCallback

        fusedClient.requestLocationUpdates(
            request,
            newCallback,
            Looper.getMainLooper()
        )
    }

    private fun saveToFirestore(location: android.location.Location) {
        Log.d("LocationTrackingService", "Saving location: $location")
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val data = mapOf(
            "lat" to location.latitude,
            "lng" to location.longitude,
            "accuracy" to location.accuracy,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(userId)
            .collection("tracking")
            .document(System.currentTimeMillis().toString())
            .set(data)
            .addOnFailureListener {
                Log.d("LocationTrackingService", "Failed to save location: ${it.localizedMessage}")
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d("Service", "onStartCommand: ${intent?.action}")

        when (intent?.action) {

            ACTION_STOP -> {
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            else -> {

                startForeground(1, buildNotification())

                // SAFE: only remove if exists
                callback?.let {
                    fusedClient.removeLocationUpdates(it)
                }

                startLocationUpdates()
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            val channel = android.app.NotificationChannel(
                "tracking_channel",
                "Location Tracking",
                android.app.NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    private fun stopLocationUpdates() {
        callback?.let {
            fusedClient.removeLocationUpdates(it)
        }
        callback = null
    }

    companion object {
        const val ACTION_STOP = "STOP_TRACKING"
    }
}