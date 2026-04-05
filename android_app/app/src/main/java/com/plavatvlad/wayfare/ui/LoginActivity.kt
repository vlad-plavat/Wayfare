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

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val guestBtn = findViewById<Button>(R.id.guestBtn)

        loginBtn.setOnClickListener {
            // TODO: add real auth later
            goToMap()
        }

        guestBtn.setOnClickListener {
            goToMap()
        }
    }

    private fun goToMap() {
        val intent = Intent(this, MapActivity::class.java)
        Log.d("MyApp", "Value of x\uD83D\uDC49\uD83D\uDC49: ia uite");
        startActivity(intent)
        finish()
    }
}