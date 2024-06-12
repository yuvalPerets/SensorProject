/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.example.test3.presentation

// imports for grpah show
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.test3.presentation.theme.Test3Theme

// imports for sending data
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.*
import android.util.Log
object Constants {
    const val DATA_PATH = "/Sensor_data_path"
}

private fun sendDataToMobile(context: Context, data: String) {

    val putDataMapRequest = PutDataMapRequest.create(Constants.DATA_PATH)
    putDataMapRequest.dataMap.putString("data_key", data)

    val putDataRequest = putDataMapRequest.asPutDataRequest()

    val putDataTask: Task<DataItem> = Wearable.getDataClient(context).putDataItem(putDataRequest)
    putDataTask.addOnSuccessListener { dataItem ->
        Log.d("Wear", "Data item sent: ${dataItem.uri}")
    }.addOnFailureListener { e ->
        Log.e("Wear", "Failed to send data item", e)
    }
}

class MainActivity : ComponentActivity() {

    //connecting to sensors
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private val xAccelerationState = mutableStateOf(0f)
    private val yAccelerationState = mutableStateOf(0f)
    private val zAccelerationState = mutableStateOf(0f)

    //part for sending bluetooth msg


    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        // Initialize sensor manager and accelerometer
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Register sensor listener
        accelerometer?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)

            setContent {
                WearApp("Android", xAccelerationState.value , yAccelerationState.value, zAccelerationState.value )
            }
    }
}
    // Define sensor listener
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            // Handle accelerometer data changes here
            val xAcceleration = event.values[0]
            val yAcceleration = event.values[1]
            val zAcceleration = event.values[2]
            // Process the accelerometer data as needed
            xAccelerationState.value = xAcceleration
            yAccelerationState.value = yAcceleration
            zAccelerationState.value = zAcceleration

            // Send data to mobile app
            sendDataToMobile(applicationContext, "$xAcceleration,$yAcceleration,$zAcceleration")

        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Unregister sensor listener
        sensorManager.unregisterListener(sensorListener)
    }
}

@Composable
fun SensorGraph(xAcceleration: Float, yAcceleration: Float, zAcceleration: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        val scaleFactor = 10 // Adjust this scale factor as per your preference for visualizing the movement

        // Draw lines representing x, y, z accelerations
        drawLine(
            color = Color.Red,
            start = Offset(centerX, centerY),
            end = Offset(centerX + xAcceleration * scaleFactor, centerY),
            strokeWidth = 3f
        )
        drawLine(
            color = Color.Green,
            start = Offset(centerX, centerY),
            end = Offset(centerX, centerY + yAcceleration * scaleFactor),
            strokeWidth = 3f
        )
        drawLine(
            color = Color.Blue,
            start = Offset(centerX, centerY),
            end = Offset(centerX + zAcceleration * scaleFactor, centerY + zAcceleration * scaleFactor),
            strokeWidth = 3f
        )
    }
}

@Composable
fun WearApp(greetingName: String, xAcceleration: Float, yAcceleration: Float, zAcceleration: Float) {
    Test3Theme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            Greeting(greetingName = greetingName, xAcceleration = xAcceleration, yAcceleration = yAcceleration, zAcceleration = zAcceleration)
            SensorGraph(xAcceleration = xAcceleration, yAcceleration = yAcceleration, zAcceleration = zAcceleration)
        }
    }
}

@Composable
fun Greeting(greetingName: String, xAcceleration: Float,yAcceleration: Float,zAcceleration: Float) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = buildAnnotatedString {
            //append(stringResource(R.string.hello_world, greetingName))
            append("sensor data : ")
            append("\n")
            append("X-axis Acceleration: $xAcceleration")
            append("\n")
            append("y-axis Acceleration: $yAcceleration")
            append("\n")
            append("z-axis Acceleration: $zAcceleration")
        }
    )
}



@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Preview Android", 0f , 0f , 0f)
}
