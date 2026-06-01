package com.plavatvlad.wayfare.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.data.Place

class PlaceDetailsFragment : Fragment(R.layout.fragment_place_details) {

    private lateinit var placeId: String

    private val db = FirebaseFirestore.getInstance()

    private lateinit var nameEdit: TextView
    private lateinit var notesEdit: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        placeId = arguments?.getString(ARG_PLACE_ID)
            ?: error("placeId missing")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nameEdit = view.findViewById(R.id.placeName)
        notesEdit = view.findViewById(R.id.placeDetails)

        loadPlace()
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

            }
    }

    companion object {
        private const val ARG_PLACE_ID = "place_id"

        fun newInstance(placeId: String): PlaceDetailsFragment {
            return PlaceDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLACE_ID, placeId)
                }
            }
        }
    }
}