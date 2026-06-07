package com.plavatvlad.wayfare.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editName = view.findViewById(R.id.editName)
        editEmail = view.findViewById(R.id.editEmail)
        editPhone = view.findViewById(R.id.editPhone)
        btnLogout = view.findViewById(R.id.btnLogout)
        switchSafetyTracking = view.findViewById(R.id.switchSafetyTracking)

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
    }

    private fun loadUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        repo.getUser(uid) { profile ->
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val updatedUser = UserProfile(
            id = uid,
            username = editName.text.toString(),
            email = editEmail.text.toString(),
            phone = editPhone.text.toString(),
            category = selectedCategory
        )

        repo.updateUser(updatedUser)

        val context = LDContext.builder(uid)
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


}