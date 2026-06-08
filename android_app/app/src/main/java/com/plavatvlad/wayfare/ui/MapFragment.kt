package com.plavatvlad.wayfare.ui

import PlaceManager
import androidx.fragment.app.Fragment
import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.utils.StatusMonitor
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import androidx.core.graphics.toColorInt
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.launchdarkly.sdk.android.LDClient
import com.plavatvlad.wayfare.data.Place
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment(R.layout.fragment_map) {

    private lateinit var map: MapView
    private lateinit var gpsBubble: View
    private lateinit var netBubble: View
    private lateinit var gpsIcon: ImageView
    private lateinit var netIcon: ImageView
    private lateinit var aiFab : FloatingActionButton
    private lateinit var placeManager: PlaceManager


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
        aiFab = view.findViewById(R.id.aiFab)
        placeManager = PlaceManager(map, requireContext(), parentFragmentManager)


        map.setMultiTouchControls(true)

        map.controller.setZoom(12.0)
        map.controller.setCenter(GeoPoint(44.4268, 26.1025))

        userMarker = Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.blue_dot)
            infoWindow = null
        }

        map.overlays.add(userMarker)

        val mapEventsReceiver = object : MapEventsReceiver {

            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                p ?: return false
                if (FirebaseAuth.getInstance().currentUser?.isAnonymous == true)
                    return false
                showAddPlaceDialog(p)

                return true
            }
        }

        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        map.overlays.add(mapEventsOverlay)

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

        showIndicators()

        LDClient.get().registerAllFlagsListener{ _ ->
            hideShowAI()
            showIndicators()
        }
        hideShowAI()
        aiFab.setOnClickListener {
            AIChatFragment().show(parentFragmentManager, "ai_chat")
        }

        placeManager.loadPlaces()

        activity?.supportFragmentManager?.setFragmentResultListener(
            "place_updated",
            viewLifecycleOwner
        ) { _, bundle ->

            val placeId = bundle.getString("placeId")

            if (placeId != null) {
                placeManager.loadPlace(placeId)
            } else {
                placeManager.loadPlaces()
            }
        }


    }

    private fun hideShowAI(){
        val showButton = LDClient.get().boolVariation(
            "ai_chat_enabled",
            false
        )
        aiFab.visibility = if (showButton) View.VISIBLE else View.GONE
    }

    private fun showIndicators(){
        val indicators = LDClient.get().stringVariation(
            "gps-network-status-indicators",
            "both"
        )
        gpsBubble.visibility = if (indicators.contains("gps") || indicators.contains("both")) View.VISIBLE else View.GONE
        netBubble.visibility = if (indicators.contains("net") || indicators.contains("both")) View.VISIBLE else View.GONE
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

    private fun showAddPlaceDialog(point: GeoPoint) {
        val dialogView =
            layoutInflater.inflate(R.layout.dialog_add_place, null)
        val nameEdit =
            dialogView.findViewById<EditText>(R.id.placeName)
        val notesEdit =
            dialogView.findViewById<EditText>(R.id.placeNotes)
        val publicSwitch =
            dialogView.findViewById<SwitchCompat>(R.id.publicSwitch)
        val lonLatText = "Latitude: ${point.latitude}\nLongitude: ${point.longitude}"
        dialogView.findViewById<TextView>(R.id.locationText).text =
            lonLatText

        AlertDialog.Builder(requireContext())
            .setTitle("Add Place")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->


                val name = nameEdit.text.toString().trim()
                val notes = notesEdit.text.toString().trim()

                if (name.isEmpty()) return@setPositiveButton

                val place = Place(
                    id = FirebaseFirestore.getInstance().collection("places").document().id,
                    name = name,
                    description = notes,
                    latitude = point.latitude,
                    longitude = point.longitude,
                    createdBy = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    publicAvailable = publicSwitch.isChecked
                )

                placeManager.savePlace(place)

            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}