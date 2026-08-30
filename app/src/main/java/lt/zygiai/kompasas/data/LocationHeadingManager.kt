package lt.zygiai.kompasas.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationHeadingManager(
    context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onLocationChanged(snapshot: LocationSnapshot)
    }

    private val appContext = context.applicationContext
    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(appContext)

    private val request = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        2_000L
    )
        .setMinUpdateIntervalMillis(1_000L)
        .setMinUpdateDistanceMeters(1f)
        .build()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(::publish)
        }
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasPermission()) return
        client.requestLocationUpdates(request, callback, appContext.mainLooper)
        client.lastLocation.addOnSuccessListener { location -> location?.let(::publish) }
    }

    fun stop() {
        client.removeLocationUpdates(callback)
    }

    private fun publish(location: Location) {
        val altitude = if (location.hasAltitude()) location.altitude else 0.0
        val field = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            altitude.toFloat(),
            System.currentTimeMillis()
        )

        listener.onLocationChanged(
            LocationSnapshot(
                latitude = location.latitude,
                longitude = location.longitude,
                altitudeMeters = if (location.hasAltitude()) location.altitude else null,
                accuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                speedMetersPerSecond = if (location.hasSpeed()) location.speed else null,
                bearingDegrees = if (location.hasBearing()) normalize360(location.bearing) else null,
                bearingAccuracyDegrees = if (
                    android.os.Build.VERSION.SDK_INT >= 26 && location.hasBearingAccuracy()
                ) location.bearingAccuracyDegrees else null,
                declinationDegrees = field.declination,
                updatedAtMillis = location.time
            )
        )
    }
}
