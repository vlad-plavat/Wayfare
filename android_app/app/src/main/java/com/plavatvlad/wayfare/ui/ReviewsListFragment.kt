package com.plavatvlad.wayfare.ui

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.data.Review

class ReviewsListFragment : Fragment(R.layout.fragment_reviews_list) {

    private lateinit var placeId: String
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        placeId = arguments?.getString(ARG_PLACE_ID)
            ?: error("placeId missing")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadReviews(view)
    }

    private fun loadReviews(view: View) {

        val container =
            view.findViewById<LinearLayout>(R.id.reviewsContainer)

        db.collection("places")
            .document(placeId)
            .collection("reviews")
            .get()
            .addOnSuccessListener { snapshot ->

                container.removeAllViews()

                snapshot.documents.forEach { doc ->

                    val review =
                        doc.toObject(Review::class.java)
                            ?: return@forEach

                    val reviewView = layoutInflater.inflate(
                        R.layout.item_review,
                        container,
                        false
                    )

                    setStars(reviewView, review)

                    reviewView.findViewById<TextView>(R.id.commentText)
                        .text = review.comment

                    reviewView.findViewById<TextView>(R.id.userText)
                        .text = review.userName

                    container.addView(reviewView)
                }
            }
    }

    private fun setStars( reviewView: View, review: Review){
        val rating = review.rating

        val stars = "★".repeat(rating) + "☆".repeat(5 - rating)
        val spannable = SpannableString(stars)

        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#FFD700")),
            0,
            rating,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable.setSpan(
            ForegroundColorSpan(Color.GRAY),
            rating,
            stars.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        reviewView.findViewById<TextView>(R.id.ratingText).text = spannable
    }

    companion object {

        private const val ARG_PLACE_ID = "place_id"

        fun newInstance(placeId: String): ReviewsListFragment {
            return ReviewsListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLACE_ID, placeId)
                }
            }
        }
    }
}