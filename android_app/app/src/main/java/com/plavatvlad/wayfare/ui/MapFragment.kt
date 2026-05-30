package com.plavatvlad.wayfare.ui

import androidx.fragment.app.Fragment
import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.utils.StatusMonitor
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import androidx.core.graphics.toColorInt
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment(R.layout.fragment_map) {

    private lateinit var map: MapView
    private lateinit var gpsBubble: View
    private lateinit var netBubble: View
    private lateinit var gpsIcon: ImageView
    private lateinit var netIcon: ImageView

    private var userMarker: Marker? = null
    private var lastGPSTime: Long = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osm", AppCompatActivity.MODE_PRIVATE)
        )

        StatusMonitor.init(requireContext())

        map = view.findViewById(R.id.map)
        gpsBubble = view.findViewById(R.id.gpsBubble)
        netBubble = view.findViewById(R.id.netBubble)
        gpsIcon = view.findViewById(R.id.gpsIcon)
        netIcon = view.findViewById(R.id.netIcon)

        map.setMultiTouchControls(true)

        map.controller.setZoom(12.0)
        map.controller.setCenter(GeoPoint(44.4268, 26.1025))

        userMarker = Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.blue_dot)
            infoWindow = null
        }

        map.overlays.add(userMarker)

        Log.d("ceva", "ajung")
        StatusMonitor.listener = { status ->
            requireActivity().runOnUiThread {


                val gpsColor = when {
                    !status.gpsLocked -> android.graphics.Color.RED
                    (status.gpsAccuracy ?: 999f) < 10 -> android.graphics.Color.GREEN
                    (status.gpsAccuracy ?: 999f) < 20 -> android.graphics.Color.YELLOW
                    else -> "#EF4F00".toColorInt()
                }

                setBubbleColor(gpsBubble, gpsColor)

                val speedKbps = status.internetQuality ?: 0

                val netColor = when {
                    speedKbps >= 5000 -> android.graphics.Color.GREEN
                    speedKbps >= 1500 -> android.graphics.Color.YELLOW
                    speedKbps >= 300 -> android.graphics.Color.rgb(255, 165, 0)
                    else -> android.graphics.Color.RED
                }

                setBubbleColor(netBubble, netColor)

                val lat = status.location?.latitude ?: return@runOnUiThread
                val lon = status.location.longitude

                val point = GeoPoint(lat, lon)

                userMarker?.position = point

                if (status.gpsLocked) {
                    if (System.currentTimeMillis() - lastGPSTime > 10000) {
                        map.controller.setZoom(18.0)
                        map.controller.animateTo(point)
                    }
                    lastGPSTime = System.currentTimeMillis()
                }
            }
        }

        val aiFab = view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.aiFab
        )

        aiFab.setOnClickListener {
            showAIChatDialog()
        }
    }


    private fun showAIChatDialog() {

        val dialogView = layoutInflater.inflate(R.layout.dialog_ai_chat, null)

        val chatHistory = dialogView.findViewById<TextView>(R.id.chatHistory)
        val chatInput = dialogView.findViewById<EditText>(R.id.chatInput)
        val sendBtn = dialogView.findViewById<Button>(R.id.sendBtn)

        val prefs = requireContext().getSharedPreferences("ai_chat", 0)

        // Load saved chat
        chatHistory.text = prefs.getString("history", "") ?: ""

        sendBtn.setOnClickListener {

            val message = chatInput.text.toString().trim()
            if (message.isEmpty()) return@setOnClickListener

            val old = chatHistory.text.toString()

            val newChat = old + "\nYou: " + message

            chatHistory.text = newChat

            chatInput.setText("")

            // Save (so it persists)
            prefs.edit()
                .putString("history", newChat)
                .apply()
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
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

    private fun setBubbleColor(view: View, color: Int) {
        view.background?.mutate()?.setTint(color)
    }
}