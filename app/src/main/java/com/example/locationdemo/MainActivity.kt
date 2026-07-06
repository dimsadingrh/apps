package com.example.locationdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var applyButton: Button
    private lateinit var currentLocationButton: Button
    private lateinit var zoomInButton: Button
    private lateinit var zoomOutButton: Button
    private lateinit var settingsButton: Button
    private lateinit var warningText: TextView

    private var selectedPoint: GeoPoint? = null
    private var currentPoint: GeoPoint? = null
    private var selectionMarker: Marker? = null
    private var currentLocationMarker: Marker? = null
    
    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener? = null
    private var isMockLocationActive = false
    private var mockLocationUpdateHandler: Handler? = null
    private var mockLocationUpdateRunnable: Runnable? = null

    // Settings
    private var randomCoordinate = false
    private var randomAccuracy = false
    private var setFused = false
    private var accuracy = 5f
    private var altitude = 0.0
    private var bearing = 0f
    private var speed = 0f

    private val defaultPoint = GeoPoint(-6.200000, 106.816666)
    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    private val permissionRequestCode = 100
    private val mockLocationUpdateInterval = 2000L // Update every 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        applyButton = findViewById(R.id.applyButton)
        currentLocationButton = findViewById(R.id.currentLocationButton)
        zoomInButton = findViewById(R.id.zoomInButton)
        zoomOutButton = findViewById(R.id.zoomOutButton)
        settingsButton = findViewById(R.id.settingsButton)
        warningText = findViewById(R.id.warningText)

        setupMap()
        bindActions()
        requestLocationPermissions()
        setupLocationTracking()
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
        stopLocationTracking()
        mapView.onPause()
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
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
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
                            selectedPoint = it
                            selectionMarker?.position = it
                            mapView.invalidate()
                        }
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        p?.let {
                            selectedPoint = it
                            selectionMarker?.position = it
                            mapView.invalidate()
                        }
                        return true
                    }
                },
            ),
        )
    }

    private fun bindActions() {
        applyButton.setOnClickListener {
            if (isMockLocationActive) {
                stopMockLocation()
            } else {
                startMockLocation()
            }
        }

        currentLocationButton.setOnClickListener {
            currentPoint?.let {
                mapView.controller.animateTo(it)
            }
        }

        zoomInButton.setOnClickListener {
            mapView.controller.zoomIn()
        }

        zoomOutButton.setOnClickListener {
            mapView.controller.zoomOut()
        }

        settingsButton.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.settings_dialog, null)

        // Get all controls
        val randomCoordSwitch = dialogView.findViewById<SwitchMaterial>(R.id.randomCoordinateSwitch)
        val randomAccuracySwitch = dialogView.findViewById<SwitchMaterial>(R.id.randomAccuracySwitch)
        val setFusedSwitch = dialogView.findViewById<SwitchMaterial>(R.id.setFusedSwitch)
        val accuracySlider = dialogView.findViewById<Slider>(R.id.accuracySlider)
        val accuracyValue = dialogView.findViewById<TextView>(R.id.accuracyValue)
        val altitudeSlider = dialogView.findViewById<Slider>(R.id.altitudeSlider)
        val altitudeValue = dialogView.findViewById<TextView>(R.id.altitudeValue)
        val bearingSlider = dialogView.findViewById<Slider>(R.id.bearingSlider)
        val bearingValue = dialogView.findViewById<TextView>(R.id.bearingValue)
        val speedSlider = dialogView.findViewById<Slider>(R.id.speedSlider)
        val speedValue = dialogView.findViewById<TextView>(R.id.speedValue)

        // Set initial values
        randomCoordSwitch.isChecked = randomCoordinate
        randomAccuracySwitch.isChecked = randomAccuracy
        setFusedSwitch.isChecked = setFused
        accuracySlider.value = accuracy
        altitudeSlider.value = altitude.toFloat()
        bearingSlider.value = bearing
        speedSlider.value = speed

        // Update text values
        accuracyValue.text = accuracy.toInt().toString()
        altitudeValue.text = altitude.toInt().toString()
        bearingValue.text = bearing.toInt().toString()
        speedValue.text = speed.toInt().toString()

        // Slider listeners
        accuracySlider.addOnChangeListener { _, value, _ ->
            accuracy = value
            accuracyValue.text = value.toInt().toString()
        }

        altitudeSlider.addOnChangeListener { _, value, _ ->
            altitude = value.toDouble()
            altitudeValue.text = value.toInt().toString()
        }

        bearingSlider.addOnChangeListener { _, value, _ ->
            bearing = value
            bearingValue.text = value.toInt().toString()
        }

        speedSlider.addOnChangeListener { _, value, _ ->
            speed = value
            speedValue.text = value.toInt().toString()
        }

        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                randomCoordinate = randomCoordSwitch.isChecked
                randomAccuracy = randomAccuracySwitch.isChecked
                setFused = setFusedSwitch.isChecked
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun startMockLocation() {
        val point = selectedPoint
        if (point == null) {
            Toast.makeText(this, R.string.status_no_point, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val provider = LocationManager.GPS_PROVIDER
            
            // Check if test provider already exists
            val existingProviders = locationManager.allProviders
            if (provider in existingProviders) {
                try {
                    locationManager.removeTestProvider(provider)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            locationManager.addTestProvider(
                provider,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                1,
                1,
            )

            locationManager.setTestProviderEnabled(provider, true)

            isMockLocationActive = true
            applyButton.text = "Stop"
            
            // Start continuous updates
            startMockLocationUpdates(point)
            
            val message = getString(
                R.string.status_applied,
                formatCoordinate(point.latitude),
                formatCoordinate(point.longitude),
            )
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startMockLocationUpdates(basePoint: GeoPoint) {
        mockLocationUpdateHandler = Handler(Looper.getMainLooper())
        mockLocationUpdateRunnable = object : Runnable {
            override fun run() {
                if (isMockLocationActive && selectedPoint != null) {
                    updateMockLocationOnce(selectedPoint!!)
                    mockLocationUpdateHandler?.postDelayed(this, mockLocationUpdateInterval)
                }
            }
        }
        mockLocationUpdateRunnable?.let {
            mockLocationUpdateHandler?.post(it)
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateMockLocationOnce(point: GeoPoint) {
        try {
            val provider = LocationManager.GPS_PROVIDER

            // Calculate latitude and longitude with random offset if enabled
            var lat = point.latitude
            var lng = point.longitude

            if (randomCoordinate) {
                val randomOffsetLat = Random.nextDouble(-0.0001, 0.0001)
                val randomOffsetLng = Random.nextDouble(-0.0001, 0.0001)
                lat += randomOffsetLat
                lng += randomOffsetLng
            }

            // Calculate accuracy with random offset if enabled
            var currentAccuracy = accuracy
            if (randomAccuracy) {
                val randomOffset = Random.nextFloat() * 5 - 2.5f // Random between -2.5 and 2.5
                currentAccuracy = (accuracy + randomOffset).coerceIn(0.1f, 100f)
            }

            val mockLocation = Location(provider).apply {
                latitude = lat
                longitude = lng
                accuracy = currentAccuracy
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
                altitude = this@MainActivity.altitude
                bearing = this@MainActivity.bearing
                speed = this@MainActivity.speed
            }

            locationManager.setTestProviderLocation(provider, mockLocation)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopMockLocation() {
        try {
            // Stop continuous updates
            mockLocationUpdateHandler?.removeCallbacks(mockLocationUpdateRunnable ?: return)
            mockLocationUpdateHandler = null
            mockLocationUpdateRunnable = null

            val provider = LocationManager.GPS_PROVIDER
            locationManager.setTestProviderEnabled(provider, false)
            locationManager.removeTestProvider(provider)

            isMockLocationActive = false
            applyButton.text = getString(R.string.apply_button)
            Toast.makeText(this, "Mock location stopped", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
