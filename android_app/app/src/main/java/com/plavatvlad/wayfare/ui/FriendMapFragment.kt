package com.plavatvlad.wayfare.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.plavatvlad.wayfare.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

class FriendMapFragment : DialogFragment() {

    private lateinit var map: MapView
    private val db = FirebaseFirestore.getInstance()
    private var friendUid: String = ""
    private var friendUsername: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        friendUid = requireArguments().getString("uid") ?: ""
        friendUsername = requireArguments().getString("username") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val root = inflater.inflate(R.layout.fragment_friend_map, container, false)

        map = root.findViewById(R.id.map)
        map.setMultiTouchControls(true)
        loadPath()

        return root
    }

    private fun loadPath() {
        db.collection("users")
            .document(friendUid)
            .collection("tracking")
            .get()
            .addOnSuccessListener { snap ->
                Log.d("FriendMapFragment", "Loaded ${snap.documents.size} points")
                val points = snap.documents.mapNotNull { doc ->
                    val lat = doc.getDouble("lat") ?: return@mapNotNull null
                    val lng = doc.getDouble("lng") ?: return@mapNotNull null
                    GeoPoint(lat, lng)
                }
                zoomToPoints(points)


                drawPath(points)
            }.addOnFailureListener { e ->
                Log.d("FriendMapFragment", "Error: ${e.message}")
            }
    }

    private fun drawPath(points: List<GeoPoint>) {
        if (points.size < 2) return

        val minColor = android.graphics.Color.GREEN
        val maxColor = android.graphics.Color.RED

        val segmentCount = points.size - 1

        for (i in 0 until segmentCount) {

            val ratio = i.toFloat() / segmentCount

            val color = interpolateColor(minColor, maxColor, ratio)

            val segment = Polyline().apply {
                setPoints(listOf(points[i], points[i + 1]))
                outlinePaint.color = color
                outlinePaint.strokeWidth = 8f
            }

            map.overlays.add(segment)
        }

        map.invalidate()
    }

    private fun interpolateColor(colorStart: Int, colorEnd: Int, ratio: Float): Int {
        val r = (android.graphics.Color.red(colorStart) +
                (android.graphics.Color.red(colorEnd) - android.graphics.Color.red(colorStart)) * ratio).toInt()

        val g = (android.graphics.Color.green(colorStart) +
                (android.graphics.Color.green(colorEnd) - android.graphics.Color.green(colorStart)) * ratio).toInt()

        val b = (android.graphics.Color.blue(colorStart) +
                (android.graphics.Color.blue(colorEnd) - android.graphics.Color.blue(colorStart)) * ratio).toInt()

        return android.graphics.Color.rgb(r, g, b)
    }

    private fun zoomToPoints(points: List<GeoPoint>) {
        if (points.isEmpty()) return

        map.post {
            val bbox = org.osmdroid.util.BoundingBox.fromGeoPoints(points)

            map.zoomToBoundingBox(bbox, false)

            val targetZoom = minOf(map.zoomLevelDouble*0.9, 20.0)

            map.controller.setZoom(targetZoom)
            map.controller.setCenter(bbox.centerWithDateLine)

            Log.d("FriendMapFragment", "Final zoom: ${map.zoomLevelDouble}")
        }
    }

    companion object {
        fun newInstance(uid: String, username: String) = FriendMapFragment().apply {
            arguments = Bundle().apply {
                putString("uid", uid)
                putString("username", username)
            }
        }
    }
}