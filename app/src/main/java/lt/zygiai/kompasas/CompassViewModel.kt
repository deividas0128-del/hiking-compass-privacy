package lt.zygiai.kompasas

import android.app.Application
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import lt.zygiai.kompasas.data.CompassThemeId
import lt.zygiai.kompasas.data.CompassUiState
import lt.zygiai.kompasas.data.HeadingMode
import lt.zygiai.kompasas.data.LocationHeadingManager
import lt.zygiai.kompasas.data.LocationSnapshot
import lt.zygiai.kompasas.data.SensorFusionManager
import lt.zygiai.kompasas.data.normalize360
import lt.zygiai.kompasas.data.shortestAngle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

class CompassViewModel(application: Application) : AndroidViewModel(application),
    SensorFusionManager.Listener,
    LocationHeadingManager.Listener {

    private val sensorManager = SensorFusionManager(application, this)
    private val locationManager = LocationHeadingManager(application, this)

    private val _uiState = MutableStateFlow(
        CompassUiState(
            sensorAvailable = sensorManager.hasCompassSensor,
            locationPermissionGranted = locationManager.hasPermission()
        )
    )
    val uiState: StateFlow<CompassUiState> = _uiState.asStateFlow()

    private var filteredMagneticHeading = 0f
    private var sensorInitialized = false
    private var lastDisplayedNormalized = 0f
    private var continuousHeading = 0f
    private var displayInitialized = false

    fun start(locationPermissionGranted: Boolean) {
        _uiState.update {
            it.copy(
                isRunning = true,
                locationPermissionGranted = locationPermissionGranted
            )
        }
        sensorManager.start()
        if (locationPermissionGranted) locationManager.start()
    }

    fun stop() {
        sensorManager.stop()
        locationManager.stop()
        _uiState.update { it.copy(isRunning = false) }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(locationPermissionGranted = granted) }
        if (granted && _uiState.value.isRunning) locationManager.start()
    }

    fun setTargetCourse(value: Float?) {
        _uiState.update { it.copy(targetCourse = value?.let(::normalize360)) }
    }

    fun setTheme(themeId: CompassThemeId) {
        _uiState.update { it.copy(themeId = themeId) }
    }

    fun setHeadingMode(mode: HeadingMode) {
        _uiState.update { it.copy(headingMode = mode) }
        recomputeDisplayedHeading()
    }

    fun setUseTrueNorth(enabled: Boolean) {
        _uiState.update { it.copy(useTrueNorth = enabled) }
        recomputeDisplayedHeading()
    }

    fun setKeepScreenOn(enabled: Boolean) {
        _uiState.update { it.copy(keepScreenOn = enabled) }
    }

    override fun onHeadingChanged(magneticHeadingDegrees: Float) {
        if (!sensorInitialized) {
            filteredMagneticHeading = magneticHeadingDegrees
            sensorInitialized = true
        } else {
            val delta = shortestAngle(filteredMagneticHeading, magneticHeadingDegrees)
            val adaptiveAlpha = when {
                abs(delta) > 35f -> 0.38f
                abs(delta) > 12f -> 0.26f
                else -> 0.16f
            }
            filteredMagneticHeading = normalize360(filteredMagneticHeading + delta * adaptiveAlpha)
        }

        val declination = _uiState.value.location.declinationDegrees
        _uiState.update {
            it.copy(
                magneticHeading = filteredMagneticHeading,
                trueHeading = normalize360(filteredMagneticHeading + declination)
            )
        }
        recomputeDisplayedHeading()
    }

    override fun onSensorAccuracyChanged(accuracy: Int) {
        _uiState.update { it.copy(sensorAccuracy = accuracy) }
        recomputeDisplayedHeading()
    }

    override fun onSensorAvailabilityChanged(available: Boolean) {
        _uiState.update { it.copy(sensorAvailable = available) }
        recomputeDisplayedHeading()
    }

    override fun onLocationChanged(snapshot: LocationSnapshot) {
        _uiState.update {
            it.copy(
                location = snapshot,
                gpsHeading = snapshot.bearingDegrees,
                trueHeading = normalize360(it.magneticHeading + snapshot.declinationDegrees)
            )
        }
        recomputeDisplayedHeading()
    }

    private fun recomputeDisplayedHeading() {
        val current = _uiState.value
        val sensorHeading = if (current.useTrueNorth) current.trueHeading else current.magneticHeading
        val gpsUsable = current.gpsHeading != null &&
            (current.location.speedMetersPerSecond ?: 0f) >= 1.2f &&
            (current.location.bearingAccuracyDegrees ?: 25f) <= 35f

        val selected = when (current.headingMode) {
            HeadingMode.SENSORS -> sensorHeading to if (current.useTrueNorth) {
                "Sensors · true north"
            } else {
                "Sensors · magnetic north"
            }

            HeadingMode.GPS -> if (gpsUsable) {
                current.gpsHeading!! to "GPS movement bearing"
            } else {
                sensorHeading to "GPS waiting for movement · sensors"
            }

            HeadingMode.AUTO -> when {
                !current.sensorAvailable && gpsUsable -> current.gpsHeading!! to "GPS · compass sensor unavailable"
                current.sensorAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE && gpsUsable ->
                    current.gpsHeading!! to "GPS · low magnetic accuracy"
                else -> sensorHeading to if (current.useTrueNorth) {
                    "AUTO · sensors + GPS correction"
                } else {
                    "AUTO · magnetic sensors"
                }
            }
        }

        val displayed = normalize360(selected.first)
        if (!displayInitialized) {
            lastDisplayedNormalized = displayed
            continuousHeading = displayed
            displayInitialized = true
        } else {
            continuousHeading += shortestAngle(lastDisplayedNormalized, displayed)
            lastDisplayedNormalized = displayed
        }

        _uiState.update {
            it.copy(
                displayedHeading = displayed,
                continuousHeading = continuousHeading,
                activeSource = selected.second
            )
        }
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
