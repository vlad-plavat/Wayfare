package com.plavatvlad.wayfare.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.launchdarkly.sdk.LDContext
import com.launchdarkly.sdk.android.LDClient
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.auth.UserRepository
import com.plavatvlad.wayfare.data.UserProfile
import com.plavatvlad.wayfare.utils.LocationTrackingService
import kotlin.text.category

class AccountFragment : Fragment(R.layout.fragment_account) {

    private lateinit var editName: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPhone: EditText
    private lateinit var btnLogout: Button
    private val repo = UserRepository()
    private lateinit var categoryToggle: MaterialButtonToggleGroup
    private var isLoadingProfile = true;
    private var selectedCategory = "regular"
    private lateinit var switchSafetyTracking: SwitchMaterial
    private lateinit var friendsButton: Button
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editName = view.findViewById(R.id.editName)
        editEmail = view.findViewById(R.id.editEmail)
        editPhone = view.findViewById(R.id.editPhone)
        btnLogout = view.findViewById(R.id.btnLogout)
        switchSafetyTracking = view.findViewById(R.id.switchSafetyTracking)
        friendsButton = view.findViewById(R.id.btnFriends)

        categoryToggle = view.findViewById(R.id.categoryToggle)

        categoryToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || isLoadingProfile) return@addOnButtonCheckedListener

            selectedCategory = when (checkedId) {
                R.id.btnRegular -> "regular"
                R.id.btnPremium -> "premium"
                R.id.btnBetaTester -> "beta"
                else -> "regular"
            }
            saveProfile()
        }

        loadUser()

        editName.setOnFocusChangeListener { _, hasFocus -> if(!hasFocus) saveProfile()}
        editEmail.setOnFocusChangeListener { _, hasFocus -> if(!hasFocus) saveProfile()}
        editPhone.setOnFocusChangeListener { _, hasFocus -> if(!hasFocus) saveProfile()}

        btnLogout.setOnClickListener {
            //Firebase logout
            FirebaseAuth.getInstance().signOut()

            //reset LaunchDarkly context
            val context = LDContext.builder("guest")
                .anonymous(true)
                .set("category", "regular")
                .build()

            LDClient.get().identify(context)

            //Go back to login
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            requireActivity().finish()
        }

        val prefs = requireContext().getSharedPreferences("settings", 0)
        val enabled = prefs.getBoolean("safety_tracking", false)

        switchSafetyTracking.isChecked = enabled
        if(enabled){
            startSafetyTracking()
        }

        switchSafetyTracking.setOnCheckedChangeListener { _, isChecked ->
            saveTrackingState(isChecked)
            if (isChecked) {
                startSafetyTracking()
            } else {
                stopSafetyTracking()
            }
        }

        friendsButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, FriendsFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<Button>(R.id.btnDeleteLoc).setOnClickListener {
            showDeleteLocData()
        }

        view.findViewById<Button>(R.id.btnPath).setOnClickListener {
            val dialog = FriendMapFragment.newInstance(userId, "")
            dialog.show(parentFragmentManager, "friend_map")
        }
    }

    private fun loadUser() {

        repo.getUser(userId) { profile ->
            if (profile == null) {
                Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
                return@getUser
            }

            editName.setText(profile.username)
            editEmail.setText(profile.email)
            editPhone.setText(profile.phone)

            selectedCategory = profile.category

            when (profile.category) {
                "regular" -> categoryToggle.check(R.id.btnRegular)
                "premium" -> categoryToggle.check(R.id.btnPremium)
                "beta" -> categoryToggle.check(R.id.btnBetaTester)
            }

            isLoadingProfile = false
        }
    }

    private fun saveProfile() {

        val updatedUser = UserProfile(
            id = userId,
            username = editName.text.toString(),
            email = editEmail.text.toString(),
            phone = editPhone.text.toString(),
            category = selectedCategory
        )

        repo.updateUser(updatedUser)

        val context = LDContext.builder(userId)
            .name(updatedUser.email)
            .set("category", selectedCategory)
            .build()

        LDClient.get().identify(context)


        Toast.makeText(
            requireContext(),
            "Account updated",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun startSafetyTracking() {
        val intent = Intent(requireContext(), LocationTrackingService::class.java)
        intent.putExtra("tracking_enabled", true)

        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun stopSafetyTracking() {
        val intent = Intent(requireContext(), LocationTrackingService::class.java)
        intent.action = LocationTrackingService.ACTION_STOP

        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun saveTrackingState(enabled: Boolean) {
        val prefs = requireContext().getSharedPreferences("settings", 0)
        prefs.edit().putBoolean("safety_tracking", enabled).apply()
    }

    private fun showDeleteLocData() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete location data")
            .setMessage("Are you sure you want to delete your tracking data?")
            .setPositiveButton("Delete") { _, _ ->
                deleteLocData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteLocData() {
        val db = FirebaseFirestore.getInstance()

        deleteTrackingBatch(db)
    }

    private fun deleteTrackingBatch(db: FirebaseFirestore, lastDoc: DocumentSnapshot? = null) {

        var query = db.collection("users")
            .document(userId)
            .collection("tracking")
            .limit(500)

        // continue from last snapshot if needed
        if (lastDoc != null) {
            query = query.startAfter(lastDoc)
        }

        query.get()
            .addOnSuccessListener { snap ->

                if (snap.isEmpty) {
                    parentFragmentManager.popBackStack()
                    return@addOnSuccessListener
                }

                val batch = db.batch()

                for (doc in snap.documents) {
                    batch.delete(doc.reference)
                }

                batch.commit()
                    .addOnSuccessListener {
                        // recursively continue deleting next chunk
                        deleteTrackingBatch(db, snap.documents.last())
                    }
                    .addOnFailureListener { e ->
                        Log.d("DeleteTracking", "Error: ${e.message}")
                    }
            }
    }
}