package com.getupside.assignment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri.fromParts
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.esri.arcgisruntime.geometry.PointBuilder
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters
import com.esri.arcgisruntime.tasks.geocode.LocatorTask
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng


class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSIONS_REQUEST_LOCATION = 0
        private const val DEFAULT_ZOOM = 16.5f
    }

    private val assignmentApplication by lazy { application as AssignmentApplication }

    private val locationPermissionHelper by lazy {
        LocationPermissionHelper(
            this,
            PERMISSIONS_REQUEST_LOCATION,
            {
                requestCurrentLocation()
                noPermissionView.visibility = GONE
            },
            {
                noPermissionView.visibility = VISIBLE
            },
            { (application as AssignmentApplication).neverAskLocationPermission() })
    }

    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val noPermissionView by lazy { findViewById<View>(R.id.no_permission) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.go_to_settings).setOnClickListener { goToSettings() }
    }

    override fun onResume() {
        super.onResume()
        if (isGooglePlayServicesAvailable())
            locationPermissionHelper.requestPermission(assignmentApplication.isLocationPermissionDenied)
        else findViewById<View>(R.id.no_google_play_services).visibility = VISIBLE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) =
        locationPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)

    private fun goToSettings() = startActivity(
        Intent().apply {
            action = ACTION_APPLICATION_DETAILS_SETTINGS
            data = fromParts("package", packageName, null)
        }
    )

    @SuppressLint("MissingPermission")
    private fun requestCurrentLocation() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        val onMapReadyCallback = OnMapReadyCallback { map ->
            fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                task.result?.let { location ->
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(location.latitude, location.longitude), DEFAULT_ZOOM
                        )
                    )
                    map.isMyLocationEnabled = true
                }
            }
        }
        mapFragment.getMapAsync(onMapReadyCallback)
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        return resultCode == ConnectionResult.SUCCESS
    }
}