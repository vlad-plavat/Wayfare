package com.plavatvlad.wayfare.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.data.Trip

class TripsAdapter(
    private val trips: List<Trip>,
    private val onClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripsViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TripsViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.trip_element, parent, false)

        return TripsViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: TripsViewHolder,
        position: Int
    ) {
        holder.bind(trips[position], onClick)
    }

    override fun getItemCount() = trips.size
}

class TripsViewHolder(view: View)
    : RecyclerView.ViewHolder(view) {

    private val tripName =
        view.findViewById<TextView>(R.id.tripName)

    private val tripPreview =
        view.findViewById<TextView>(R.id.tripPlacesPreview)

    fun bind(
        trip: Trip,
        onClick: (Trip) -> Unit
    ) {

        tripName.text = trip.name

        tripPreview.text =
            trip.placeIds.take(5).joinToString(", ")

        itemView.setOnClickListener {
            onClick(trip)
        }
    }
}