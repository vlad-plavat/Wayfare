package com.plavatvlad.wayfare.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.plavatvlad.wayfare.R
import android.content.Intent
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import com.plavatvlad.wayfare.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

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

            if(!email.isEmpty() && !password.isEmpty())
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        Log.d("AUTH", "Login successful")
                        Toast.makeText(
                            this,
                            "Login successful",
                            Toast.LENGTH_SHORT
                        ).show()

                        goToMap()

                    } else {

                        Log.e("AUTH", "Login failed", task.exception)
                        Toast.makeText(
                            this,
                            task.exception?.message ?: "Login failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        guestBtn.setOnClickListener {
            goToMap()
        }

        registerBtn.setOnClickListener {

            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if(!email.isEmpty() && !password.isEmpty())
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        Log.d("AUTH", "Registration successful")
                        Toast.makeText(
                            this,
                            "Registration successful",
                            Toast.LENGTH_SHORT
                        ).show()

                        goToMap()

                    } else {

                        Log.e("AUTH", "Registration failed", task.exception)
                        Toast.makeText(
                            this,
                            task.exception?.message ?: "Registration failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun goToMap() {
        val intent = Intent(this, MainActivity::class.java)
        Log.d("MyApp", "Value of x\uD83D\uDC49\uD83D\uDC49: ia uite");
        startActivity(intent)
        finish()
    }
}