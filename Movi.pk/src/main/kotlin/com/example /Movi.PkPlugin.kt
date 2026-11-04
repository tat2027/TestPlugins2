package com.example

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class MoviPkPlugin : Plugin() {
    override fun load(context: Context) {
        // Correct Cloudstream standard method to register providers
        registerMainProvider(MoviPkProvider())
    }
}
