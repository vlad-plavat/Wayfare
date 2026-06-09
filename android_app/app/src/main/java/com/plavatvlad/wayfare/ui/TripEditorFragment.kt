package com.plavatvlad.wayfare.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.data.Trip

class TripEditorFragment : Fragment(R.layout.fragment_trip_edit) {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var tripNameInput: EditText
    private lateinit var addPlaceInput: EditText
    private lateinit var addPlaceButton: Button
    private lateinit var saveButton: Button
    private lateinit var placesText: TextView

    private var tripId: String? = null

    private val placeIds = mutableListOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripNameInput = view.findViewById(R.id.tripNameInput)
        addPlaceInput = view.findViewById(R.id.addPlaceInput)
        addPlaceButton = view.findViewById(R.id.addPlaceButton)
        saveButton = view.findViewById(R.id.saveTripButton)
        placesText = view.findViewById(R.id.placesListText)

        tripId = arguments?.getString("tripId")

        if (tripId != null) {
            loadTrip(tripId!!)
        }

        addPlaceButton.setOnClickListener {

            val placeId =
                addPlaceInput.text.toString().trim()

            if (placeId.isEmpty()) return@setOnClickListener

            placeIds.add(placeId)

            addPlaceInput.setText("")

            refreshPlacesText()
        }

        saveButton.setOnClickListener {
            saveTrip()
        }
    }

    private fun loadTrip(id: String) {

        db.collection("trips")
            .document(id)
            .get()
            .addOnSuccessListener { doc ->

                val trip =
                    doc.toObject(Trip::class.java)
                        ?: return@addOnSuccessListener

                tripNameInput.setText(trip.name)

                placeIds.clear()
                placeIds.addAll(trip.placeIds)

                refreshPlacesText()
            }
    }

    private fun refreshPlacesText() {

        placesText.text =
            if (placeIds.isEmpty()) {
                "No places"
            } else {
                placeIds.joinToString("\n")
            }
    }

    private fun saveTrip() {

        val uid =
            FirebaseAuth.getInstance().currentUser?.uid
                ?: return

        val trip = hashMapOf(
            "name" to tripNameInput.text.toString(),
            "placeIds" to placeIds,
            "createdBy" to uid,
            "createdAt" to System.currentTimeMillis()
        )

        if (tripId == null) {

            db.collection("trips")
                .add(trip)
                .addOnSuccessListener {
                    parentFragmentManager.popBackStack()
                }

        } else {

            db.collection("trips")
                .document(tripId!!)
                .update(
                    mapOf(
                        "name" to tripNameInput.text.toString(),
                        "placeIds" to placeIds
                    )
                )
                .addOnSuccessListener {
                    parentFragmentManager.popBackStack()
                }
        }
    }
}