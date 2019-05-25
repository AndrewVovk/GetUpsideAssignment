package com.getupside.assignment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri.fromParts
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSIONS_REQUEST_LOCATION = 2019
        private const val DEFAULT_ZOOM = 14f
    }

    private val assignmentApplication by lazy { application as AssignmentApplication }
    private val viewModel by lazy { ViewModelProviders.of(this)[MainViewModel::class.java] }

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

    private val recyclerView by lazy {
        findViewById<RecyclerView>(R.id.recycler_view).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private val nameTextView by lazy { findViewById<TextView>(R.id.placeName) }
    private val addressTextView by lazy { findViewById<TextView>(R.id.placeAddress) }
    private val phoneTextView by lazy { findViewById<TextView>(R.id.placePhone) }
    private val urlTextView by lazy { findViewById<TextView>(R.id.placeUrl) }
    private val typeTextView by lazy { findViewById<TextView>(R.id.placeType) }
    private val detailsView by lazy { findViewById<View>(R.id.details_root) }
    private val phoneLayout by lazy { findViewById<View>(R.id.phone_layout) }
    private val urlLayout by lazy { findViewById<View>(R.id.url_layout) }

    private val onMapReadyCallback = OnMapReadyCallback { map ->
        showLastLocation(map)
        observePlaces(map)
        attachOnCameraIdleListener(map)
        map.setOnMarkerClickListener {
            viewModel.onMarkerClick(it.position)
            true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.go_to_settings).setOnClickListener { goToSettings() }

        observeSelectedPlace()
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

    private fun attachOnCameraIdleListener(map: GoogleMap) {
        map.setOnCameraIdleListener(object : GoogleMap.OnCameraIdleListener {
            private var firstCallSkipped = false
            override fun onCameraIdle() {
                if (firstCallSkipped) {
                    viewModel.onCameraIdle(map.cameraPosition.target)
                } else {
                    firstCallSkipped = true
                }
            }
        })
    }

    private fun observeSelectedPlace() {
        viewModel.selectedPlace.observe(this, Observer { place ->
            detailsView.visibility = VISIBLE

            nameTextView.text = place.name
            addressTextView.text = place.address

            if (place.phone.isNullOrEmpty()) {
                phoneLayout.visibility = GONE
            } else {
                phoneLayout.visibility = VISIBLE
                phoneTextView.text = place.phone
            }

            if (place.url.isNullOrEmpty()) {
                urlLayout.visibility = GONE
            } else {
                urlLayout.visibility = VISIBLE
                urlTextView.text = place.url
            }

            typeTextView.text = place.type
        })
    }

    private fun observePlaces(map: GoogleMap) {
        viewModel.places.observe(this, Observer { list ->
            recyclerView.adapter = PlacesAdapter(list, viewModel::onPlaceClick)
            map.clear()
            for (place in list) {
                map.addMarker(
                    MarkerOptions().position(
                        LatLng(
                            place.latitude ?: throw IllegalStateException(),
                            place.longitude ?: throw IllegalStateException()
                        )
                    )
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.restaurant_pin))
                )
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun showLastLocation(map: GoogleMap) {
        fusedLocationClient.lastLocation.addOnCompleteListener { task ->
            task.result?.let { location ->
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude), DEFAULT_ZOOM
                    )
                )
                map.isMyLocationEnabled = true
                viewModel.onMapReady(map.projection.visibleRegion.latLngBounds, location)
            }
        }
    }

    private fun requestCurrentLocation() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(onMapReadyCallback)
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        return resultCode == ConnectionResult.SUCCESS
    }
}