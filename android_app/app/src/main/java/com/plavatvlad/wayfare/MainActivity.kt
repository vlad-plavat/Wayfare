package com.plavatvlad.wayfare

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.launchdarkly.sdk.android.LDClient
import com.plavatvlad.wayfare.ui.AccountFragment
import com.plavatvlad.wayfare.ui.MapFragment
import com.plavatvlad.wayfare.ui.PlacesFragment
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val mapFragment = MapFragment()
    private val accountFragment = AccountFragment()
    private val placesFragment = PlacesFragment()
    //private val settingsFragment = SettingsFragment()

    private var active: Fragment = mapFragment

    private fun switchFragment(target: Fragment) {

        if (target == active) return

        supportFragmentManager.beginTransaction()
            .hide(active)
            .show(target)
            .commit()

        active = target
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val client = LDClient.get()

        if (client.isInitialized) {
            val enabled = client.boolVariation("ai_chat_enabled", true)
            Log.d("FFLAG", "read"+enabled.toString())
            val indicators = client.stringVariation("gps-network-status-indicators", "both")
            Log.d("FFLAG", "indicators: "+indicators.toString())
        }else{
            Log.d("FFLAG", "nui bun")
        }


        supportFragmentManager.beginTransaction()
            //.add(R.id.fragmentContainer, settingsFragment, "4").hide(settingsFragment)
            .add(R.id.fragmentContainer, placesFragment, "3").hide(placesFragment)
            .add(R.id.fragmentContainer, accountFragment, "2").hide(accountFragment)
            .add(R.id.fragmentContainer, mapFragment, "1")
            .commit()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_map -> switchFragment(mapFragment)
                R.id.nav_account -> switchFragment(accountFragment)
                R.id.nav_places -> switchFragment(placesFragment)
                //R.id.nav_settings -> switchFragment(settingsFragment)
            }
            true
        }

    }
}

