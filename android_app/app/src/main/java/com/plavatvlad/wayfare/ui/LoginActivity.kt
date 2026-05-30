package com.plavatvlad.wayfare.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.plavatvlad.wayfare.R
import android.content.Intent
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import com.launchdarkly.sdk.LDContext
import com.plavatvlad.wayfare.MainActivity
import com.plavatvlad.wayfare.auth.LauncherActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private fun buildContext(uid: String, email: String?, category: String): LDContext {
        return LDContext.builder(uid)
            .name(email ?: "unknown")
            .set("category", category)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        setContentView(R.layout.activity_login)

        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val guestBtn = findViewById<Button>(R.id.guestBtn)
        val registerBtn = findViewById<Button>(R.id.registerBtn)

        val emailInput = findViewById<EditText>(R.id.emailLoginField)
        val passwordInput = findViewById<EditText>(R.id.passwordLoginField)

        loginBtn.setOnClickListener {

            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {

                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        goToLauncher()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // GUEST
        guestBtn.setOnClickListener {

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        registerBtn.setOnClickListener {

            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        goToLauncher()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun goToLauncher() {
        startActivity(Intent(this, LauncherActivity::class.java))
        finish()
    }
}