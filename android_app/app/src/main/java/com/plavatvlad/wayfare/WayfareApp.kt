package com.plavatvlad.wayfare

import android.app.Application
import com.launchdarkly.sdk.LDContext
import com.launchdarkly.sdk.android.LDClient
import com.launchdarkly.sdk.android.LDConfig

class WayfareApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = LDConfig.Builder(
            LDConfig.Builder.AutoEnvAttributes.Enabled
            )
            .mobileKey(BuildConfig.LAUNCHDARKLY_MOBILE_KEY)
            .build()

        val context = LDContext.builder("anonymous")
            .anonymous(true)
            .build()

        LDClient.init(
            this,
            config,
            context,
            5
        )
    }
}