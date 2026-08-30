package lt.zygiai.kompasas.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager

class SensorFusionManager(
    context: Context,
    private val listener: Listener
) : SensorEventListener {

    interface Listener {
        fun onHeadingChanged(magneticHeadingDegrees: Float)
        fun onSensorAccuracyChanged(accuracy: Int)
        fun onSensorAvailabilityChanged(available: Boolean)
    }

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val rotationMatrix = FloatArray(9)
    private val remappedRotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)

    private var hasGravity = false
    private var hasGeomagnetic = false
    private var running = false

    val hasCompassSensor: Boolean
        get() = rotationVectorSensor != null || (accelerometer != null && magnetometer != null)

    fun start() {
        if (running) return
        running = true
        listener.onSensorAvailabilityChanged(hasCompassSensor)

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(
                this,
                rotationVectorSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        } else {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
            magnetometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    fun stop() {
        if (!running) return
        running = false
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                publishOrientation(rotationMatrix)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                lowPass(event.values, gravity, 0.12f)
                hasGravity = true
                publishFallbackIfReady()
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                lowPass(event.values, geomagnetic, 0.10f)
                hasGeomagnetic = true
                publishFallbackIfReady()
            }
        }
    }

    private fun publishFallbackIfReady() {
        if (!hasGravity || !hasGeomagnetic) return
        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
            publishOrientation(rotationMatrix)
        }
    }

    private fun publishOrientation(matrix: FloatArray) {
        @Suppress("DEPRECATION")
        val rotation = windowManager.defaultDisplay.rotation

        val (xAxis, yAxis) = when (rotation) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }

        SensorManager.remapCoordinateSystem(matrix, xAxis, yAxis, remappedRotationMatrix)
        SensorManager.getOrientation(remappedRotationMatrix, orientation)

        val azimuthDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
        listener.onHeadingChanged(normalize360(azimuthDegrees))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD || sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            listener.onSensorAccuracyChanged(accuracy)
        }
    }

    private fun lowPass(input: FloatArray, output: FloatArray, alpha: Float) {
        val size = minOf(input.size, output.size)
        for (i in 0 until size) {
            output[i] += alpha * (input[i] - output[i])
        }
    }
}
