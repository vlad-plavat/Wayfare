package com.plavatvlad.wayfare.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.data.Place
import com.plavatvlad.wayfare.utils.PlaceImagesAdapter
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class PlaceDetailsEdit : Fragment(R.layout.fragment_place_edit) {

    private lateinit var placeId: String

    private val db = FirebaseFirestore.getInstance()

    private lateinit var nameEdit: EditText
    private lateinit var notesEdit: EditText
    private lateinit var coordsText: TextView
    private lateinit var publicSwitch: SwitchCompat
    private lateinit var saveButton: Button
    private lateinit var deleteButton: Button
    private lateinit var imagesAdapter: PlaceImagesAdapter
    private lateinit var ratingSummary: TextView
    private lateinit var rateButton: Button

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                uploadPlaceImage(it)
            }
        }

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
        ratingSummary = view.findViewById(R.id.ratingSummary)
        rateButton = view.findViewById(R.id.rateButton)
        loadPlace(view)

        saveButton.setOnClickListener {
            saveChanges()
        }
        deleteButton.setOnClickListener {
            showDeletePlaceDialog()
        }
        val addImageButton = view.findViewById<Button>(R.id.addImageButton)

        addImageButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        ratingSummary.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .add(
                    R.id.fragmentContainer,
                    ReviewsListFragment.newInstance(placeId)
                )
                .addToBackStack(null)
                .commit()
        }

        rateButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .add(
                    R.id.fragmentContainer,
                    AddReviewFragment.newInstance(placeId)
                )
                .addToBackStack(null)
                .commit()
        }

    }

    private fun loadPlace(view: View) {
        db.collection("places")
            .document(placeId)
            .get()
            .addOnSuccessListener { doc ->

                val place = doc.toObject(Place::class.java)
                    ?: return@addOnSuccessListener

                nameEdit.setText(place.name)
                notesEdit.setText(place.description)
                publicSwitch.isChecked = place.publicAvailable
                coordsText.text = "Lat: ${place.latitude}, Lng: ${place.longitude}"
                    ratingSummary.text = "⭐ ${"%.1f".format(place.ratingAverage)} (${place.ratingCount} reviews)"

                val recyclerView = view.findViewById<RecyclerView>(R.id.imagesRecyclerView)

                recyclerView.layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

                imagesAdapter = PlaceImagesAdapter(place.photoUrls){
                    imageUrl -> showDeleteImageDialog(imageUrl)
                }
                recyclerView.adapter = imagesAdapter
            }
    }

    private fun showDeleteImageDialog(imageUrl: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove image")
            .setMessage("Do you want to remove this image?")
            .setPositiveButton("Yes") { _, _ ->
                deleteImage(imageUrl)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteImage(imageUrl: String) {

        FirebaseStorage.getInstance()
            .getReferenceFromUrl(imageUrl)
            .delete()
            .addOnSuccessListener {

                db.collection("places")
                    .document(placeId)
                    .update(
                        "photoUrls",
                        FieldValue.arrayRemove(imageUrl)
                    )
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Delete successful",
                            Toast.LENGTH_LONG
                        ).show()
                        loadPlace(requireView())
                    }
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
            .addOnSuccessListener {
                parentFragmentManager.setFragmentResult(
                    "place_updated",
                    Bundle().apply {
                        putString("placeId", placeId)
                    }
                )
            }
    }

    private fun showDeletePlaceDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete place")
            .setMessage("Are you sure you want to delete this place?")
            .setPositiveButton("Delete") { _, _ ->
                deletePlace()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePlace() {

        val imagesRef = FirebaseStorage.getInstance()
            .reference
            .child("places/$placeId")

        imagesRef.listAll()
            .addOnSuccessListener { result ->

                val deleteTasks = result.items.map { it.delete() }

                com.google.android.gms.tasks.Tasks
                    .whenAllComplete(deleteTasks)
                    .addOnSuccessListener {

                        db.collection("places")
                            .document(placeId)
                            .delete()
                            .addOnSuccessListener {

                                Toast.makeText(
                                    requireContext(),
                                    "Place deleted",
                                    Toast.LENGTH_SHORT
                                ).show()
                                parentFragmentManager.setFragmentResult(
                                    "place_updated",
                                    Bundle().apply {
                                        putString("placeId", placeId)
                                    }
                                )
                                parentFragmentManager.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    requireContext(),
                                    "Delete failed: ${e.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to delete images: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun uploadPlaceImage(uri: Uri) {

        val imageRef = FirebaseStorage.getInstance()
            .reference
            .child("places/$placeId/${UUID.randomUUID()}.jpg")

        imageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                imageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUrl ->

                db.collection("places")
                    .document(placeId)
                    .update("photoUrls", com.google.firebase.firestore.FieldValue.arrayUnion(downloadUrl.toString()))
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Upload successful",
                            Toast.LENGTH_LONG
                        ).show()
                        loadPlace(requireView())
                    }
            }.addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Upload failed: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
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