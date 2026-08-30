package lt.zygiai.kompasas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lt.zygiai.kompasas.data.CompassThemeId
import lt.zygiai.kompasas.data.CompassUiState
import lt.zygiai.kompasas.data.HeadingMode
import lt.zygiai.kompasas.ui.theme.LocalCompassPalette
import kotlin.math.abs

@Composable
fun CompassApp(
    state: CompassUiState,
    onRequestLocationPermission: () -> Unit,
    onSetTargetCourse: (Float?) -> Unit,
    onThemeSelected: (CompassThemeId) -> Unit,
    onHeadingModeSelected: (HeadingMode) -> Unit,
    onUseTrueNorthChanged: (Boolean) -> Unit,
    onKeepScreenOnChanged: (Boolean) -> Unit
) {
    val palette = LocalCompassPalette.current
    var showCourseDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HIKING COMPASS",
            color = palette.textPrimary,
            fontSize = 27.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Text(
            text = state.activeSource,
            color = palette.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CompassDial(
                continuousHeading = state.continuousHeading,
                targetCourse = state.targetCourse
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%03d°".format(state.headingRounded),
                    color = palette.textPrimary,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = state.directionName,
                    color = palette.textSecondary,
                    fontSize = 15.sp
                )
                if (state.targetCourse != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "SET BEARING",
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "%03d°".format(state.targetCourse.toInt()),
                        color = palette.target,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        CourseGuidance(state)
        Spacer(Modifier.height(18.dp))
        LocationCard(state, onRequestLocationPermission)
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showCourseDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = state.targetCourse?.let { "%03d°".format(it.toInt()) } ?: "BEARING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = {
                    val next = when (state.headingMode) {
                        HeadingMode.AUTO -> HeadingMode.SENSORS
                        HeadingMode.SENSORS -> HeadingMode.GPS
                        HeadingMode.GPS -> HeadingMode.AUTO
                    }
                    onHeadingModeSelected(next)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(state.headingMode.shortTitle, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { onUseTrueNorthChanged(!state.useTrueNorth) },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (state.useTrueNorth) "TRUE N." else "MAG. N.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = {
                    val all = CompassThemeId.entries
                    val next = all[(all.indexOf(state.themeId) + 1) % all.size]
                    onThemeSelected(next)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(state.themeId.title.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (state.targetCourse != null) {
            TextButton(onClick = { onSetTargetCourse(null) }) {
                Text("Clear bearing")
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { onKeepScreenOnChanged(!state.keepScreenOn) }) {
            Text(if (state.keepScreenOn) "Screen stays on · tap to disable" else "Keep screen on during hike")
        }
        Spacer(Modifier.height(12.dp))
    }

    if (showCourseDialog) {
        CourseDialog(
            initialCourse = state.targetCourse,
            onDismiss = { showCourseDialog = false },
            onConfirm = {
                onSetTargetCourse(it)
                showCourseDialog = false
            }
        )
    }
}

@Composable
private fun CourseGuidance(state: CompassUiState) {
    val palette = LocalCompassPalette.current
    val deviation = state.deviationDegrees
    val text: String
    val color = when {
        deviation == null -> {
            text = "SET A BEARING"
            palette.textSecondary
        }
        abs(deviation) <= 2f -> {
            text = "ON COURSE"
            palette.onCourse
        }
        deviation > 0f -> {
            text = "→ ${abs(deviation).toInt()}° RIGHT"
            palette.north
        }
        else -> {
            text = "← ${abs(deviation).toInt()}° LEFT"
            palette.north
        }
    }
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        color = color,
        textAlign = TextAlign.Center,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Black
    )
}

@Composable
private fun LocationCard(state: CompassUiState, onRequestLocationPermission: () -> Unit) {
    val palette = LocalCompassPalette.current
    val l = state.location
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "LOCATION & MOVEMENT",
                color = palette.north,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (l.latitude != null && l.longitude != null) {
                    "%.5f, %.5f   ·   accuracy ±%s m".format(
                        l.latitude,
                        l.longitude,
                        l.accuracyMeters?.toInt()?.toString() ?: "—"
                    )
                } else {
                    "Location unavailable"
                },
                color = palette.textPrimary,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "Altitude ${l.altitudeMeters?.toInt()?.let { "$it m" } ?: "—"}   ·   " +
                    "speed ${l.speedMetersPerSecond?.let { "%.1f km/h".format(it * 3.6f) } ?: "—"}   ·   " +
                    "GPS ${l.bearingDegrees?.toInt()?.let { "$it°" } ?: "—"}",
                color = palette.textSecondary,
                fontSize = 13.sp
            )
            if (!state.locationPermissionGranted) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onRequestLocationPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable GPS features")
                }
            }
        }
    }
}

@Composable
private fun CourseDialog(
    initialCourse: Float?,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var text by remember(initialCourse) {
        mutableStateOf(initialCourse?.toInt()?.toString().orEmpty())
    }
    val parsed = text.toIntOrNull()
    val valid = parsed != null && parsed in 0..359

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set bearing") },
        text = {
            Column {
                Text("Enter an azimuth from 0° to 359°.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(3) },
                    label = { Text("Bearing") },
                    suffix = { Text("°") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = text.isNotBlank() && !valid,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(it.toFloat()) } },
                enabled = valid
            ) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
