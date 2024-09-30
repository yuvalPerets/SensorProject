package com.example.test3.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.*
/*
object Constants {
    const val DATA_PATH = "/Sensor_data_path"
}
*/
class SensorService : Service(), SensorEventListener {

    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Buffer to accumulate accelerometer data
    private val dataBuffer = mutableListOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private val sendBufferRunnable = object : Runnable {
        override fun run() {
            if (dataBuffer.isNotEmpty()) {
                // Send the accumulated data as a single packet
                sendDataToMobile(applicationContext, dataBuffer.joinToString(""))
                // Clear the buffer after sending
                dataBuffer.clear()
            }
            // Schedule the next send
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Create a notification channel for Android 8.0 and higher
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "SensorServiceChannel",
                "Sensor Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, "SensorServiceChannel")
            .setContentTitle("Sensor Service")
            .setContentText("Running...")

            .build()

        startForeground(1, notification)

        // Acquire a wake lock to keep the CPU running
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::SensorWakeLock")
        wakeLock.acquire()

        // Initialize sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        // Start the handler to send buffered data every second
        handler.postDelayed(sendBufferRunnable, 1000)
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Handle accelerometer data changes
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Log the accelerometer data
        val timestamp = System.currentTimeMillis()
        Log.d("SensorService", "Time: $timestamp - Accelerometer Data - X: $x, Y: $y, Z: $z")


        // Add data to the buffer
        dataBuffer.add("$x,")
        dataBuffer.add("$y,")
        dataBuffer.add("$z,")

        // Send data to mobile

        //sendDataToMobile(applicationContext, "$x,$y,$z")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        wakeLock.release()
        handler.removeCallbacks(sendBufferRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun sendDataToMobile(context: Context, data: String) {
        val putDataMapRequest = PutDataMapRequest.create(Constants.DATA_PATH)
        putDataMapRequest.dataMap.putString("data_key", data)

        val putDataRequest = putDataMapRequest.asPutDataRequest()
        val putDataTask: Task<DataItem> = Wearable.getDataClient(context).putDataItem(putDataRequest)
        val timestamp = System.currentTimeMillis()

        putDataTask.addOnSuccessListener { dataItem ->
            Log.d("SensorService-bluetooth-sent", "Time: $timestamp -Data item sent: ${dataItem.uri} ")
        }.addOnFailureListener { e ->
            Log.e("SensorService-bluetooth-sent", "Time: $timestamp -Failed to send data item", e)
        }
    }
}
