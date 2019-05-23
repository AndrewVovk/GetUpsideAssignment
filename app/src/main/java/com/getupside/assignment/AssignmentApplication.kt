package com.getupside.assignment

import android.app.Application
import android.content.Context


private const val LOCATION_PERMISSION_DENIED = "location_permission_denied"

class AssignmentApplication : Application() {

    private val prefs
        get() = getSharedPreferences("PREFS", Context.MODE_PRIVATE)

    val isLocationPermissionDenied: Boolean
        get() = prefs.getBoolean(LOCATION_PERMISSION_DENIED, false)

    fun neverAskLocationPermission() = prefs.edit().putBoolean(LOCATION_PERMISSION_DENIED, true).apply()

}