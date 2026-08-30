package lt.zygiai.kompasas.data

import android.hardware.SensorManager

enum class HeadingMode(val title: String, val shortTitle: String) {
    AUTO("Automatic", "AUTO"),
    SENSORS("Phone sensors", "SENSORS"),
    GPS("GPS movement bearing", "GPS")
}

enum class CompassThemeId(val title: String) {
    HIKING("Hiking"),
    NIGHT("Night"),
    MINIMAL("Minimal"),
    TOPOGRAPHIC("Topographic"),
    CLASSIC("Classic")
}

data class LocationSnapshot(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitudeMeters: Double? = null,
    val accuracyMeters: Float? = null,
    val speedMetersPerSecond: Float? = null,
    val bearingDegrees: Float? = null,
    val bearingAccuracyDegrees: Float? = null,
    val declinationDegrees: Float = 0f,
    val updatedAtMillis: Long? = null
)

data class CompassUiState(
    val magneticHeading: Float = 0f,
    val trueHeading: Float = 0f,
    val displayedHeading: Float = 0f,
    val continuousHeading: Float = 0f,
    val gpsHeading: Float? = null,
    val headingMode: HeadingMode = HeadingMode.AUTO,
    val activeSource: String = "Sensors",
    val useTrueNorth: Boolean = false,
    val sensorAvailable: Boolean = true,
    val sensorAccuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE,
    val locationPermissionGranted: Boolean = false,
    val location: LocationSnapshot = LocationSnapshot(),
    val targetCourse: Float? = null,
    val themeId: CompassThemeId = CompassThemeId.HIKING,
    val keepScreenOn: Boolean = false,
    val isRunning: Boolean = false
) {
    val headingRounded: Int
        get() = normalize360(displayedHeading).toInt()

    val deviationDegrees: Float?
        get() = targetCourse?.let { shortestAngle(displayedHeading, it) }

    val isOnCourse: Boolean
        get() = deviationDegrees?.let { kotlin.math.abs(it) <= 2f } == true

    val directionName: String
        get() = directionNameEnglish(displayedHeading)
}

fun normalize360(value: Float): Float {
    val normalized = value % 360f
    return if (normalized < 0f) normalized + 360f else normalized
}

/** Signed shortest rotation from [from] to [to], range -180..180. */
fun shortestAngle(from: Float, to: Float): Float =
    ((to - from + 540f) % 360f) - 180f

fun circularBlend(a: Float, b: Float, bWeight: Float): Float {
    val weight = bWeight.coerceIn(0f, 1f)
    return normalize360(a + shortestAngle(a, b) * weight)
}

fun directionNameEnglish(degrees: Float): String {
    val names = listOf(
        "North",
        "Northeast",
        "East",
        "Southeast",
        "South",
        "Southwest",
        "West",
        "Northwest"
    )
    val index = ((normalize360(degrees) + 22.5f) / 45f).toInt() % 8
    return names[index]
}
