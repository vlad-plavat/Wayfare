package com.plavatvlad.wayfare.auth

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.plavatvlad.wayfare.data.UserProfile

class UserRepository {

    private val db = FirebaseFirestore.getInstance()

    fun createUser(user: UserProfile) {
        db.collection("users")
            .document(user.id)
            .set(user)
    }

    fun getUser(uid: String, callback: (UserProfile?) -> Unit) {
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                callback(doc.toObject(UserProfile::class.java))
            }
            .addOnFailureListener {
                Log.e("UserRepository", "Error getting user", it)
                callback(null)
            }
    }
}