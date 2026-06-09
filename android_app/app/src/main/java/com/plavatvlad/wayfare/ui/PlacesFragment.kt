package com.plavatvlad.wayfare.ui

import PlacesAdapter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.functions
import com.launchdarkly.sdk.android.LDClient
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.data.LocationHolder
import com.plavatvlad.wayfare.data.Place

class PlacesFragment : Fragment(R.layout.fragment_places) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlacesAdapter
    private val db = FirebaseFirestore.getInstance()
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.placesRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                bottom + 16
            )
            insets
        }

        adapter = PlacesAdapter(emptyList()) { place ->
            val uid = FirebaseAuth.getInstance().uid
            val placeId = place.id

            val createdBy = place.createdBy

            val fragment = if (createdBy == uid) {
                PlaceDetailsEdit.newInstance(placeId)
            } else {
                PlaceDetailsEdit.newInstance(placeId, false)
            }

            Log.d("PlacesFragment", "Place clicked: ${place.name}")
            requireActivity().supportFragmentManager
                .beginTransaction()
                .add(R.id.places_overlay_container, fragment)
                .addToBackStack("place_details")
                .commit()
        }
        recyclerView.adapter = adapter

        loadPlaces()
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        swipeRefresh.setOnRefreshListener {
            loadPlaces()
        }

        val client = LDClient.get()
        val strategy = client.stringVariation(
            "feed-experience-mode",
            "default",
            )
        view.findViewById<TextView>(R.id.debugTitle).text = "Strategy: ${strategy}"
    }

    private fun loadPlaces() {

        val client = LDClient.get()

        val strategy = client.stringVariation(
            "feed-experience-mode",
            "default",

        )
        Log.d("PlacesFragment", "Strategy: $strategy")
        Log.d("PlacesFragment", "Location: ${LocationHolder.lat}, ${LocationHolder.lng}")
        val data = hashMapOf(
            "strategy" to strategy,
            "uid" to (FirebaseAuth.getInstance().currentUser?.uid ?: ""),
            "lat" to LocationHolder.lat,
            "lng" to LocationHolder.lng
        )
        Firebase.functions.getHttpsCallable("getPlacesFeed")
            .call(data)
            .addOnSuccessListener { result ->

                val map = result.data as Map<*, *>
                val ids = map["placeIds"] as List<String>
                Log.e("PlacesFragment", "Function succeeded: $ids")
                swipeRefresh.isRefreshing = false

                fetchPlacesByIds(ids)
            }
            .addOnFailureListener { e ->
                Log.e("Places", "Function failed: ${e.message}")
                swipeRefresh.isRefreshing = false
            }



        /*db.collection("places")
            .whereEqualTo("publicAvailable", true)
            .get()
            .addOnSuccessListener { snapshot ->
                swipeRefresh.isRefreshing = false
                val ids = snapshot.documents.mapNotNull { it.id }

                if (ids.isEmpty()) {
                    adapter.updateData(emptyList())
                    return@addOnSuccessListener
                }
                Log.d("PlacesFragment", "ids: ${ids.count()}")

                fetchPlacesByIds(ids)
            }.addOnFailureListener { e ->
                Log.d("PlacesFragment", "Error: ${e.message}")
                swipeRefresh.isRefreshing = false

            }*/
    }

    private fun fetchPlacesByIds(ids: List<String>) {

        val placesMap = mutableMapOf<String, Place>()

        ids.forEach { id ->

            db.collection("places")
                .document(id)
                .get()
                .addOnSuccessListener { doc ->

                    val place = doc.toObject(Place::class.java)

                    if (place != null) {
                        placesMap[id] = place.copy(id = doc.id)
                    }

                    if (placesMap.size == ids.size) {

                        val ordered = ids.mapNotNull { placesMap[it] }

                        adapter.updateData(ordered)
                    }
                }
        }
    }
}