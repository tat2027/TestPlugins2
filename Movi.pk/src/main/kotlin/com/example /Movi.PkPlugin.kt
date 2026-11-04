package com.example

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

/**
 * Single File Bundle entry point.
 * In Cloudstream 3, a single plugin (.cs3) can bundle multiple scrapers/providers
 * by registering them within the load() lifecycle method.
 */
@CloudstreamPlugin
class Plugin : Plugin() {
    override fun load(context: Context) {
        // Register all providers bundled in this single .cs3 package
        registerMainAPI(MoviPk())
        registerMainAPI(Hdhub4u())
    }
}

