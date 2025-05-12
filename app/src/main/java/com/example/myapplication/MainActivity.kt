package com.example.myapplication

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val apiKey = "10bf32ac09abf5b2bb74ea44221d0330" // OpenWeatherMap API key
    private lateinit var idNumberInput: EditText
    private lateinit var cityInput: EditText
    private lateinit var latitudeInput: EditText
    private lateinit var longitudeInput: EditText
    private lateinit var addButton: Button
    private lateinit var retrieveButton: Button
    private lateinit var deleteButton: Button
    private lateinit var updateButton: Button

    // Content Provider URI
    private val CONTENT_URI = Uri.parse("content://com.example.myapplication.provider/cities")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize UI components
        idNumberInput = findViewById(R.id.id_number_input)
        cityInput = findViewById(R.id.city_input)
        latitudeInput = findViewById(R.id.latitude_input)
        longitudeInput = findViewById(R.id.longitude_input)
        addButton = findViewById(R.id.add_button)
        retrieveButton = findViewById(R.id.retrieve_button)
        deleteButton = findViewById(R.id.delete_button)
        updateButton = findViewById(R.id.update_button)

        // Initialize the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set button listeners
        addButton.setOnClickListener {  }
        retrieveButton.setOnClickListener {  }
        deleteButton.setOnClickListener {  }
        updateButton.setOnClickListener {  }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Set custom InfoWindow adapter
        mMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                return null // Use getInfoContents to provide custom view
            }

            override fun getInfoContents(marker: Marker): View {
                // Inflate the custom layout
                val view = LayoutInflater.from(this@MainActivity)
                    .inflate(R.layout.custom_info_window, null)

                // Find views in the custom layout
                val titleTextView = view.findViewById<TextView>(R.id.title)
                val snippetTextView = view.findViewById<TextView>(R.id.snippet)

                // Set marker data
                titleTextView.text = marker.title
                snippetTextView.text = marker.snippet

                return view
            }
        })

        retrieveCities()
    }

    private fun addCity() {
        val idNumber = idNumberInput.text.toString()
        val city = cityInput.text.toString()
        val latitudeStr = latitudeInput.text.toString()
        val longitudeStr = longitudeInput.text.toString()

        if (idNumber.isEmpty() || city.isEmpty() || latitudeStr.isEmpty() || longitudeStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val latitude = latitudeStr.toDouble()
            val longitude = longitudeStr.toDouble()

            // Insert into Content Provider
            val values = ContentValues().apply {
                put("id_number", idNumber)
                put("city", city)
                put("latitude", latitude)
                put("longitude", longitude)
            }

            val uri = contentResolver.insert(CONTENT_URI, values)
            if (uri != null) {
                Toast.makeText(this, "City added", Toast.LENGTH_SHORT).show()
                // Clear inputs
                idNumberInput.text.clear()
                cityInput.text.clear()
                latitudeInput.text.clear()
                longitudeInput.text.clear()
                // Add marker and move map to the new city
                val latLng = LatLng(latitude, longitude)
                fetchTemperature(city, latLng)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
            } else {
                Toast.makeText(this, "Failed to add city", Toast.LENGTH_SHORT).show()
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Invalid latitude or longitude", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCity() {
        val idNumber = idNumberInput.text.toString()
        val city = cityInput.text.toString()
        val latitudeStr = latitudeInput.text.toString()
        val longitudeStr = longitudeInput.text.toString()

        if (idNumber.isEmpty() || city.isEmpty() || latitudeStr.isEmpty() || longitudeStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields to update", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val latitude = latitudeStr.toDouble()
            val longitude = longitudeStr.toDouble()

            // Prepare values for update
            val values = ContentValues().apply {
                put("city", city)
                put("latitude", latitude)
                put("longitude", longitude)
            }

            // Update via Content Provider
            val rowsUpdated = contentResolver.update(
                CONTENT_URI,
                values,
                "id_number = ?",
                arrayOf(idNumber)
            )

            if (rowsUpdated > 0) {
                Toast.makeText(this, "City updated", Toast.LENGTH_SHORT).show()
                // Clear inputs
                idNumberInput.text.clear()
                cityInput.text.clear()
                latitudeInput.text.clear()
                longitudeInput.text.clear()
                // Refresh map
                retrieveCities()
            } else {
                Toast.makeText(this, "City not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Invalid latitude or longitude", Toast.LENGTH_SHORT).show()
        }
    }

    private fun retrieveCities() {
        mMap.clear() // Clear existing markers
        val idNumber = idNumberInput.text.toString()

        if (idNumber.isNotEmpty()) {
            // Query Content Provider for specific ID
            contentResolver.query(
                CONTENT_URI,
                null,
                "id_number = ?",
                arrayOf(idNumber),
                null
            )?.use { cursor ->
                Log.d("MainActivity", "Cursor count for ID $idNumber: ${cursor.count}")
                if (cursor.moveToFirst()) {
                    val city = cursor.getString(cursor.getColumnIndexOrThrow("city"))
                    val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
                    val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
                    val latLng = LatLng(latitude, longitude)

                    // Fetch temperature and add marker
                    fetchTemperature(city, latLng)
                    // Move map to the city's location
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 1f))
                } else {
                    Toast.makeText(this, "City not found", Toast.LENGTH_SHORT).show()
                    // Fallback to default location
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(31.9539, 35.9106), 1f))
                }
            }
        } else {
            // Query all cities if no ID is provided
            val boundsBuilder = LatLngBounds.Builder()
            var hasMarkers = false

            contentResolver.query(CONTENT_URI, null, null, null, null)?.use { cursor ->
                Log.d("MainActivity", "Cursor count: ${cursor.count}")
                if (cursor.moveToFirst()) {
                    do {
                        val city = cursor.getString(cursor.getColumnIndexOrThrow("city"))
                        val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
                        val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
                        val latLng = LatLng(latitude, longitude)

                        // Fetch temperature and add marker
                        fetchTemperature(city, latLng)
                        boundsBuilder.include(latLng)
                        hasMarkers = true
                    } while (cursor.moveToNext())
                }
            }

            // Adjust map bounds
            if (hasMarkers) {
                try {
                    val bounds = boundsBuilder.build()
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                } catch (e: IllegalStateException) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(31.9539, 35.9106), 10f))
                }
            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(31.9539, 35.9106), 10f))
            }
        }
    }

    private fun deleteCity() {
        val idNumber = idNumberInput.text.toString()
        if (idNumber.isEmpty()) {
            Toast.makeText(this, "Please enter ID number", Toast.LENGTH_SHORT).show()
            return
        }

        // Delete from Content Provider
        val rowsDeleted = contentResolver.delete(
            CONTENT_URI,
            "id_number = ?",
            arrayOf(idNumber)
        )
        if (rowsDeleted > 0) {
            Toast.makeText(this, "City deleted", Toast.LENGTH_SHORT).show()
            idNumberInput.text.clear()
            retrieveCities()
        } else {
            Toast.makeText(this, "City not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchTemperature(city: String, latLng: LatLng) {
        val client = OkHttpClient()
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey&units=metric"

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "Failed to fetch weather for $city: ${e.message}")
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    val json = JSONObject(responseData)
                    val temp = json.getJSONObject("main").getDouble("temp")
                    val humidity = json.getJSONObject("main").getInt("humidity")
                    val windSpeed = json.getJSONObject("wind").getDouble("speed")
                    val iconCode = json.getJSONArray("weather").getJSONObject(0).getString("icon")

                    runOnUiThread {
                        addTemperatureMarker(city, latLng, temp, humidity, windSpeed, iconCode)
                    }
                }
            }
        })
    }

    private fun addTemperatureMarker(city: String, latLng: LatLng, temp: Double, humidity: Int, windSpeed: Double, iconCode: String) {
        // Load weather icon from OpenWeatherMap
        val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"

        Glide.with(this)
            .asBitmap()
            .load(iconUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    // Add marker with custom weather icon
                    mMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(city)
                            .snippet("Temperature: $temp°C\nHumidity: $humidity%\nWind Speed: $windSpeed m/s")
                            .icon(BitmapDescriptorFactory.fromBitmap(resource))
                    )
                }

                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                    // No action needed when resource is cleared
                }

                override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                    // Fallback to default marker if icon fails to load
                    val hue = when {
                        temp < 10 -> BitmapDescriptorFactory.HUE_BLUE
                        temp < 20 -> BitmapDescriptorFactory.HUE_GREEN
                        temp < 30 -> BitmapDescriptorFactory.HUE_YELLOW
                        else -> BitmapDescriptorFactory.HUE_RED
                    }
                    mMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(city)
                            .snippet("Temperature: $temp°C\nHumidity: $humidity%\nWind Speed: $windSpeed m/s")
                            .icon(BitmapDescriptorFactory.defaultMarker(hue))
                    )
                }
            })
    }
}