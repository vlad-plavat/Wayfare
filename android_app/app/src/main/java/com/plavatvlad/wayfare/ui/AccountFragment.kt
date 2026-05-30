package com.plavatvlad.wayfare.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.plavatvlad.wayfare.R

class AccountFragment : Fragment(R.layout.fragment_account) {

    private lateinit var editName: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPhone: EditText
    private lateinit var btnSave: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editName = view.findViewById(R.id.editName)
        editEmail = view.findViewById(R.id.editEmail)
        editPhone = view.findViewById(R.id.editPhone)
        btnSave = view.findViewById(R.id.btnSave)

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
    }
}