package com.plavatvlad.wayfare.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.launchdarkly.sdk.LDContext
import com.launchdarkly.sdk.android.LDClient
import com.plavatvlad.wayfare.MainActivity
import com.plavatvlad.wayfare.data.UserProfile
import com.plavatvlad.wayfare.ui.LoginActivity

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser

        // 1. Not logged in → Login screen
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val uid = user.uid

        // 2. Load user profile from Firestore
        UserRepository().getUser(uid) { profile ->

            val finalUser = profile ?: UserProfile(
                id = uid,
                email = user.email ?: "",
                username = "unknown",
                category = "regular"
            )

            // 3. Build LaunchDarkly context (ONLY PLACE THIS HAPPENS)
            val context = LDContext.builder(finalUser.id)
                .name(finalUser.email)
                .set("category", finalUser.category)
                .build()

            LDClient.get().identify(context)

            // 4. Go to main app
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}