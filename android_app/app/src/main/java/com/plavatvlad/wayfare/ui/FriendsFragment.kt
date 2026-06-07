package com.plavatvlad.wayfare.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.utils.FriendAdapter

class FriendsFragment : Fragment(R.layout.fragment_friends) {

    private lateinit var recycler: RecyclerView
    private lateinit var editSearch: EditText
    private lateinit var btnAdd: Button

    private lateinit var adapter: FriendAdapter
    private val db = FirebaseFirestore.getInstance()
    private val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.friendsRecycler)
        editSearch = view.findViewById(R.id.editSearchUser)
        btnAdd = view.findViewById(R.id.btnAddFriend)

        adapter = FriendAdapter { friendId ->
            removeFriend(friendId)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        btnAdd.setOnClickListener {
            val username = editSearch.text.toString()
            findAndAddFriend(username)
        }

        loadFriends()
    }

    // -------------------------
    // LOAD FRIENDS (REALTIME)
    // -------------------------
    private fun loadFriends() {
        db.collection("users")
            .document(myUid)
            .collection("friends")
            .addSnapshotListener { snap, error ->
                Log.d("FriendsFragment", "Error: ${error?.message} + ${snap?.metadata}")

                if (error != null || snap == null) return@addSnapshotListener

                val list = snap.documents.map { it.id }


                adapter.submit(list)
            }
    }

    // -------------------------
    // SEARCH USER + ADD FRIEND
    // -------------------------
    private fun findAndAddFriend(username: String) {
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { docs ->
                if(docs.count() == 0){
                    Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                }
                val user = docs.firstOrNull() ?: return@addOnSuccessListener
                val friendUid = user.id

                if (friendUid == myUid) return@addOnSuccessListener

                addFriend(friendUid)
            }.addOnFailureListener { e->
                Log.d("FriendsFragment", "Error: ${e.message}")
            }
    }

    // -------------------------
    // ADD FRIEND (SYMMETRIC)
    // -------------------------
    private fun addFriend(friendUid: String) {

        val empty = mapOf<String, Any>()

        db.collection("users").document(myUid)
            .collection("friends").document(friendUid)
            .set(empty).addOnFailureListener { Log.d("FriendsFragment", "Error: ${it.message}") }
            .addOnSuccessListener { loadFriends() }

        db.collection("users").document(friendUid)
            .collection("friends").document(myUid)
            .set(empty)
    }

    // -------------------------
    // REMOVE FRIEND (SYMMETRIC)
    // -------------------------
    private fun removeFriend(friendUid: String) {

        db.collection("users").document(myUid)
            .collection("friends").document(friendUid)
            .delete().addOnFailureListener { Log.d("FriendsFragment", "Error: ${it.message}") }
            .addOnSuccessListener { loadFriends() }

        db.collection("users").document(friendUid)
            .collection("friends").document(myUid)
            .delete()
    }
}