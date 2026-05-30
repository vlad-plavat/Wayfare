package com.plavatvlad.wayfare.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.launchdarkly.sdk.LDContext
import com.launchdarkly.sdk.android.LDClient
import com.plavatvlad.wayfare.R
import com.plavatvlad.wayfare.auth.UserRepository

class AccountFragment : Fragment(R.layout.fragment_account) {

    private lateinit var editName: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPhone: EditText
    private lateinit var btnSave: Button
    private lateinit var btnLogout: Button

    private val repo = UserRepository()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editName = view.findViewById(R.id.editName)
        editEmail = view.findViewById(R.id.editEmail)
        editPhone = view.findViewById(R.id.editPhone)
        btnSave = view.findViewById(R.id.btnSave)
        btnLogout = view.findViewById(R.id.btnLogout)

        loadUser()

        // Example existing data
        editName.setText("Vlad")
        editEmail.setText("vlad@example.com")
        editPhone.setText("+40 700 000 000")

        btnSave.setOnClickListener {

            val name = editName.text.toString()
            val email = editEmail.text.toString()
            val phone = editPhone.text.toString()

            // TODO: save to database/sharedprefs/api

            Toast.makeText(
                requireContext(),
                "Account updated",
                Toast.LENGTH_SHORT
            ).show()
        }

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
        }
    }
}