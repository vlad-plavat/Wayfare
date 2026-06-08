package com.plavatvlad.wayfare.utils

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.plavatvlad.wayfare.R
import com.google.firebase.firestore.FirebaseFirestore

class FriendAdapter(
    private val onRemove: (String) -> Unit,
    private val onMap: (String) -> Unit
) : RecyclerView.Adapter<FriendAdapter.VH>() {

    private val items = mutableListOf<String>()

    fun submit(list: List<String>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val username = view.findViewById<TextView>(R.id.textUsername)
        val remove = view.findViewById<Button>(R.id.btnRemove)

        val map = view.findViewById<ImageButton>(R.id.btnMap)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val id = items[position]
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(id).get().addOnSuccessListener {
            holder.username.text = it.getString("username")
        }

        holder.remove.setOnClickListener {
            onRemove(id)
        }
        holder.map.setOnClickListener {
            onMap(id)
        }
    }

    override fun getItemCount() = items.size
}