package com.plavatvlad.wayfare.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.data.Place

class PlaceDetailsEdit : Fragment(R.layout.fragment_place_edit) {

    private lateinit var placeId: String

    private val db = FirebaseFirestore.getInstance()

    private lateinit var nameEdit: TextView
    private lateinit var notesEdit: TextView
    private lateinit var coordsText: TextView
    private lateinit var publicSwitch: Switch
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        placeId = arguments?.getString(ARG_PLACE_ID)
            ?: error("placeId missing")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nameEdit = view.findViewById(R.id.placeName)
        notesEdit = view.findViewById(R.id.placeDetails)
        coordsText = view.findViewById(R.id.placeCoords)
        saveButton = view.findViewById(R.id.saveButton)
        deleteButton = view.findViewById(R.id.deleteButton)
        publicSwitch = view.findViewById(R.id.publicSwitch)

        loadPlace()

        saveButton.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadPlace() {
        db.collection("places")
            .document(placeId)
            .get()
            .addOnSuccessListener { doc ->

                val place = doc.toObject(Place::class.java)
                    ?: return@addOnSuccessListener

                nameEdit.text = place.name
                notesEdit.text = place.description
                publicSwitch.isChecked = place.publicAvailable
                coordsText.text = "Lat: ${place.latitude}, Lng: ${place.longitude}"
            }
    }

    private fun saveChanges() {
        val updated = mapOf(
            "name" to nameEdit.text.toString(),
            "description" to notesEdit.text.toString(),
            "publicAvailable" to publicSwitch.isChecked
        )

        db.collection("places")
            .document(placeId)
            .update(updated)
    }

    companion object {
        private const val ARG_PLACE_ID = "place_id"

        fun newInstance(placeId: String): PlaceDetailsEdit {
            return PlaceDetailsEdit().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLACE_ID, placeId)
                }
            }
        }
    }
}