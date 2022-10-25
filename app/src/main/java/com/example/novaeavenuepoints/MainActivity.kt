package com.example.novaeavenuepoints

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.example.novaeavenuepoints.databinding.ActivityMainBinding
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding

    private val mOkHttpClient = OkHttpClient()

    private val apiKey = BuildConfig.apiToken

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }

    private var okHttpCall: Call? = null
    private val indiaLatLng = LatLng(23.0, 77.0)
    private val mapper = ObjectMapper().apply {
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    }
    private val mMap: MutableLiveData<GoogleMap> = MutableLiveData()
    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            updateNearByPoints(p0.lastLocation)
        }
    }

    companion object {
        private const val POINTS_BASE_URL = "https://apis.novaeavenue.com/points/api/v1/nearby"
        private const val TAG = "MainActivity"
    }

    private var isMapZoomed = false
    private val iconDescriptor by lazy {
        bitmapDescriptorFromDrawable(R.drawable.ic_pothole)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mapsView.onCreate(savedInstanceState)
        lifecycle.addObserver(LocationLifecycleObserver(this, mLocationCallback) {
            updateNearByPoints(it)
        })

        mMap.observe(this) {
            it.animateCamera(CameraUpdateFactory.newLatLngZoom(indiaLatLng, 4f))
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }
        binding.mapsView.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.style_json
                )
            )
        } catch (e: Resources.NotFoundException) {
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }
        map.isMyLocationEnabled = true
        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isMyLocationButtonEnabled = true
            isTiltGesturesEnabled = false
            isRotateGesturesEnabled = false
        }
        mMap.postValue(map)
    }


    private fun updateNearByPoints(location: Location?) {
        if (location == null) return
        val latLng = LatLng(location.latitude, location.longitude)
        if (isMapZoomed.not()) {
            mMap.value?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
            isMapZoomed = true
        }
        lifecycleScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            okHttpCall?.cancel()
            val request = Request.Builder()
                .url("$POINTS_BASE_URL?key=$apiKey&latLng=${latLng.latitude},${latLng.longitude}")
                .get()
                .build()
            okHttpCall = mOkHttpClient.newCall(request)
            val response = okHttpCall?.execute() ?: return@launch
            val body = response.body?.string() ?: return@launch
            Log.d(TAG, "updateNearByPoints: $body")
            val ptsResponse = mapper.readValue(body, PointsResponse::class.java)
            ptsResponse.results.ptsAddress.orEmpty().map {
                val latLng = LatLng(it.lat, it.lng)
                withContext(Dispatchers.Main) {
                    val marker = mMap.value?.addMarker(
                        com.google.android.gms.maps.model.MarkerOptions()
                            .position(latLng)
                            .icon(iconDescriptor)
                    )
                    marker?.tag = it
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            binding.mapsView.getMapAsync(this)
        }
    }

    override fun onStop() {
        super.onStop()
        binding.mapsView.onStop()
    }

    override fun onStart() {
        super.onStart()
        binding.mapsView.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapsView.onDestroy()
    }

    private fun bitmapDescriptorFromVector(vectorResId: Int): BitmapDescriptor? {
        return ResourcesCompat.getDrawable(resources, vectorResId, theme)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap =
                Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }?: BitmapDescriptorFactory.defaultMarker()
    }

    private fun bitmapDescriptorFromDrawable(
        drawableRes: Int
    ): BitmapDescriptor {
        return ResourcesCompat.getDrawable(resources, drawableRes, theme)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap =
                Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 80, 80, false)
            BitmapDescriptorFactory.fromBitmap(scaledBitmap)
        } ?: BitmapDescriptorFactory.defaultMarker()
    }
}