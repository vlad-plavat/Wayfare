package com.plavatvlad.wayfare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.data.Trip

class TripsFragment : Fragment(R.layout.fragment_trips) {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var recyclerView: RecyclerView
    private lateinit var newTripButton: Button

    private val trips = mutableListOf<Trip>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.tripsRecyclerView)
        newTripButton = view.findViewById(R.id.newTripButton)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        /*val adapter = TripsAdapter(trips) { trip ->
            openTrip(trip.id)
        }

        recyclerView.adapter = adapter

        newTripButton.setOnClickListener {
            openTrip(null)
        }

        loadTrips(adapter)*/
    }

    /*private fun loadTrips(adapter: TripsAdapter) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("trips")
            .whereEqualTo("createdBy", uid)
            .get()
            .addOnSuccessListener { snapshot ->

                trips.clear()

                snapshot.documents.forEach { doc ->

                    val trip =
                        doc.toObject(Trip::class.java)
                            ?: return@forEach

                    trip.id = doc.id

                    trips.add(trip)
                }

                adapter.notifyDataSetChanged()
            }
    }*/

    private fun openTrip(tripId: String?) {

        val fragment = TripEditorFragment().apply {
            arguments = Bundle().apply {
                putString("tripId", tripId)
            }
        }

        /*parentFragmentManager.beginTransaction()
            .add(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()*/
    }
}