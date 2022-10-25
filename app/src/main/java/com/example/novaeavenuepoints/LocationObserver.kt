package com.example.novaeavenuepoints

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener

class LocationLifecycleObserver(
    private val mContext: Context,
    private val mCallback: LocationCallback,
    private val successListener: OnSuccessListener<Location>?,
) : DefaultLifecycleObserver {
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest

    companion object {
        private const val DEFAULT_INTERVAL_IN_MILLISECONDS = 10000L
    }


    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)
        mLocationRequest =
            LocationRequest.create().setInterval(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setFastestInterval(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
    }


    //@SuppressLint("MissingPermission")
    override fun onResume(owner: LifecycleOwner) {
        if (ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        if (successListener != null)
            mFusedLocationClient.lastLocation
                .addOnSuccessListener(
                    successListener
                )
        Looper.myLooper()?.let {
            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest,
                mCallback,
                it
            )
        }

    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        mFusedLocationClient.removeLocationUpdates(mCallback)
    }
}