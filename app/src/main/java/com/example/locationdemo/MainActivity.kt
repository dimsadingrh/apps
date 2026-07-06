package com.example.locationdemo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    private var selectedPoint: GeoPoint? = null
    private var selectionMarker: Marker? = null

    private val defaultPoint = GeoPoint(-6.200000, 106.816666)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        selectedPointText = findViewById(R.id.selectedPointText)
        statusText = findViewById(R.id.statusText)
        applyButton = findViewById(R.id.applyButton)

        setupMap()
        bindActions()
        updateSelection(defaultPoint, getString(R.string.status_ready))
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(14.0)
        mapView.controller.setCenter(defaultPoint)

        selectionMarker = Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = getString(R.string.marker_title)
        }
        mapView.overlays.add(selectionMarker)

        mapView.overlays.add(
            MapEventsOverlay(
                object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        p?.let {
                            updateSelection(it, getString(R.string.status_point_selected))
                            mapView.controller.animateTo(it)
                        }
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        p?.let {
                            updateSelection(it, getString(R.string.status_point_selected))
                            mapView.controller.animateTo(it)
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
    }

    private fun updateSelection(point: GeoPoint, statusMessage: String) {
        selectedPoint = point
        selectionMarker?.position = point
        selectionMarker?.subDescription = getString(
            R.string.selected_point_template,
            formatCoordinate(point.latitude),
            formatCoordinate(point.longitude),
        )
        mapView.invalidate()

        selectedPointText.text = getString(
            R.string.selected_point_template,
            formatCoordinate(point.latitude),
            formatCoordinate(point.longitude),
        )
        statusText.text = statusMessage
    }

    private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)
}
