package com.example.locationdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var selectedPointText: TextView
    private lateinit var statusText: TextView
    private lateinit var applyButton: Button
    private lateinit var currentLocationButton: Button
    private lateinit var zoomInButton: Button
    private lateinit var zoomOutButton: Button
    private lateinit var warningText: TextView

    private var selectedPoint: GeoPoint? = null
    private var currentPoint: GeoPoint? = null
    private var selectionMarker: Marker? = null
    private var currentLocationMarker: Marker? = null
    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener? = null

    private val defaultPoint = GeoPoint(-6.200000, 106.816666)
    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    private val permissionRequestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        selectedPointText = findViewById(R.id.selectedPointText)
        statusText = findViewById(R.id.statusText)
        applyButton = findViewById(R.id.applyButton)
        currentLocationButton = findViewById(R.id.currentLocationButton)
        zoomInButton = findViewById(R.id.zoomInButton)
        zoomOutButton = findViewById(R.id.zoomOutButton)
        warningText = findViewById(R.id.warningText)

        setupMap()
        bindActions()
        requestLocationPermissions()
        setupLocationTracking()
        updateSelection(defaultPoint, getString(R.string.status_ready))
        showWarning()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (hasLocationPermission()) {
            startLocationTracking()
        }
    }

    override fun onPause() {
        mapView.onPause()
        stopLocationTracking()
        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupLocationTracking()
                startLocationTracking()
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this, locationPermissions, permissionRequestCode)
        }
    }

    private fun setupLocationTracking() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationListener = LocationListener { location ->
            onLocationUpdate(location)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationTracking() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                10f,
                locationListener!!,
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopLocationTracking() {
        try {
            locationListener?.let { locationManager.removeUpdates(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onLocationUpdate(location: Location) {
        currentPoint = GeoPoint(location.latitude, location.longitude)
        updateCurrentLocationMarker()
        statusText.text = getString(R.string.status_location_updated)
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.OpenStreetMap.Mapnik)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(14.0)
        mapView.controller.setCenter(defaultPoint)

        selectionMarker = Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = getString(R.string.marker_title)
            icon = null
        }
        mapView.overlays.add(selectionMarker)

        currentLocationMarker = Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = getString(R.string.current_location_title)
            icon = null
        }
        mapView.overlays.add(currentLocationMarker)

        mapView.overlays.add(
            MapEventsOverlay(
                object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        p?.let {
                            updateSelection(it, getString(R.string.status_point_selected))
                        }
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        p?.let {
                            updateSelection(it, getString(R.string.status_point_selected))
                        }
                        return true
                    }
                },
            ),
        )
    }

    private fun bindActions() {
        applyButton.setOnClickListener {
            val point = selectedPoint
            if (point == null) {
                Toast.makeText(this, R.string.status_no_point, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val message = getString(
                R.string.status_applied,
                formatCoordinate(point.latitude),
                formatCoordinate(point.longitude),
            )
            statusText.text = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        currentLocationButton.setOnClickListener {
            currentPoint?.let {
                mapView.controller.animateTo(it)
                statusText.text = getString(R.string.status_location_updated)
            }
        }

        zoomInButton.setOnClickListener {
            mapView.controller.zoomIn()
        }

        zoomOutButton.setOnClickListener {
            mapView.controller.zoomOut()
        }
    }

    private fun updateSelection(point: GeoPoint, statusMessage: String) {
        selectedPoint = point
        selectionMarker?.position = point
        mapView.invalidate()

        selectedPointText.text = getString(
            R.string.selected_point_template,
            formatCoordinate(point.latitude),
            formatCoordinate(point.longitude),
        )
        statusText.text = statusMessage
    }

    private fun updateCurrentLocationMarker() {
        currentPoint?.let {
            currentLocationMarker?.position = it
            mapView.invalidate()
        }
    }

    private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

    private fun showWarning() {
        warningText.text = getString(R.string.developer_option_warning)
    }
}
