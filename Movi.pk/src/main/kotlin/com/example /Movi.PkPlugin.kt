package com.example

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class MoviPkPlugin : Plugin() {
    override fun load(context: Context) {
        // Register provider using current Cloudstream API standard
        register(MoviPkProvider())
    }
}
