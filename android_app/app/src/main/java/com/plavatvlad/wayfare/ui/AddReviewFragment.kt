package com.plavatvlad.wayfare.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.data.Review

class AddReviewFragment : Fragment(R.layout.fragment_add_review) {

    private lateinit var placeId: String
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        placeId = arguments?.getString(ARG_PLACE_ID)
            ?: error("placeId missing")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBar)
        val commentInput = view.findViewById<EditText>(R.id.commentInput)
        val submitButton = view.findViewById<Button>(R.id.submitButton)
        val deleteButton = view.findViewById<Button>(R.id.deleteButton)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val placeRef = db.collection("places").document(placeId)

        placeRef.collection("reviews")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                document.toObject(Review::class.java)?.let { review ->
                    ratingBar.rating = review.rating.toFloat()
                    commentInput.setText(review.comment)
                    deleteButton.visibility = View.VISIBLE
                }
            }



        submitButton.setOnClickListener {

            val rating = ratingBar.rating.toInt()
            val comment = commentInput.text.toString()

            if (rating == 0) {
                Toast.makeText(requireContext(), "Select rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitReview(rating, comment)
        }

        deleteButton.setOnClickListener {
            deleteReviewPopup()
        }
    }

    private fun submitReview(rating: Int, comment: String) {

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->

                val userName = doc.getString("username") ?: return@addOnSuccessListener
                val review = hashMapOf(
                    "userId" to userId,
                    "userName" to userName,
                    "rating" to rating,
                    "comment" to comment,
                    "createdAt" to java.time.LocalDate.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                )

                val placeRef = db.collection("places").document(placeId)

                placeRef.collection("reviews")
                    .document(userId)
                    .set(review)
                    .addOnSuccessListener {

                        parentFragmentManager.popBackStack()
                    }

            }
    }

    private fun deleteReviewPopup(){
        AlertDialog.Builder(requireContext())
            .setTitle("Remove review")
            .setMessage("Do you want to remove your review?")
            .setPositiveButton("Yes") { _, _ ->
                deleteReview()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteReview() {
        db.collection("places")
            .document(placeId)
            .collection("reviews")
            .document(userId)
            .delete()
            .addOnSuccessListener {
                parentFragmentManager.popBackStack()
            }
    }


    companion object {

        private const val ARG_PLACE_ID = "place_id"

        fun newInstance(placeId: String): AddReviewFragment {
            return AddReviewFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLACE_ID, placeId)
                }
            }
        }
    }
}