package com.getupside.assignment

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class LocationPermissionHelper(
    private val activity: Activity,
    private val requestCode: Int,
    private val onGranted: () -> Unit,
    private val onDenied: () -> Unit,
    private val onNeverAskAgain: () -> Unit
) {

    fun requestPermission(isMicrophonePermissionDenied: Boolean) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (!isMicrophonePermissionDenied) {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
            }
        } else {
            onGranted()
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            this.requestCode -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    onGranted()
                } else if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) {
                    // user checked never ask again
                    onNeverAskAgain()
                } else {
                    onDenied()
                }
            }

            else -> {
                // Ignore all other requests.
            }
        }
    }
}