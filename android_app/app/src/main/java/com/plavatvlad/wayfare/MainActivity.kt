package com.plavatvlad.wayfare

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.plavatvlad.wayfare.ui.MapFragment

class MainActivity : AppCompatActivity() {

    private val mapFragment = MapFragment()
    //private val accountFragment = AccountFragment()
    //private val historyFragment = HistoryFragment()
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

        supportFragmentManager.beginTransaction()
            //.add(R.id.fragmentContainer, settingsFragment, "4").hide(settingsFragment)
            //.add(R.id.fragmentContainer, historyFragment, "3").hide(historyFragment)
            //.add(R.id.fragmentContainer, accountFragment, "2").hide(accountFragment)
            .add(R.id.fragmentContainer, mapFragment, "1")
            .commit()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_map -> switchFragment(mapFragment)
                //R.id.nav_account -> switchFragment(accountFragment)
                //R.id.nav_history -> switchFragment(historyFragment)
                //R.id.nav_settings -> switchFragment(settingsFragment)
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}