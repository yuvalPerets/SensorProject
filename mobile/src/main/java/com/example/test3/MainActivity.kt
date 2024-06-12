package com.example.test3

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


// imports for getting data from wear
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import android.widget.TextView


import com.example.test3.R.id.accelerometerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



object Constants {
    const val DATA_PATH = "/Sensor_data_path"
    const val PREFS_NAME = "SensorDataPrefs"
}


class MainActivity : AppCompatActivity(), DataListenerService.DataCallback {
    private lateinit var textViewData: TextView
    private lateinit var textViewStoredData: TextView
    private lateinit var editTextTag: EditText
    private lateinit var buttonRecord: Button
    private lateinit var buttonShowRecord: Button

    private var isRecording = false
    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        textViewData = findViewById(R.id.textViewData)
        editTextTag = findViewById(R.id.editTextTag)
        buttonRecord = findViewById(R.id.buttonRecord)
        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        textViewStoredData = findViewById(R.id.textViewStoredData)

        val buttonClearStoredData: Button = findViewById(R.id.buttonClearStoredData)
        buttonClearStoredData.setOnClickListener {
            clearStoredData()
        }


        buttonRecord.setOnClickListener {
            isRecording = !isRecording
            if (isRecording) {
                buttonRecord.text = "Stop Recording"
            } else {
                buttonRecord.text = "Record"

            }
        }
        // button for showing stored data
        buttonShowRecord = findViewById(R.id.buttonShowStoredData)
        buttonShowRecord.setOnClickListener {

            showStoredData()
        }

        // Set data callback
        DataListenerService.setDataCallback(this)

    }
    private fun clearStoredData() {
        // Clear all stored data from SharedPreferences
        sharedPreferences.edit().clear().apply()
        // Clear the textViewStoredData
        textViewStoredData.text = ""
    }
    private fun showStoredData() {
        val storedData = getAllStoredData()
        textViewStoredData.text = storedData
    }
    private fun getAllStoredData(): String {
        val allEntries = sharedPreferences.all
        val stringBuilder = StringBuilder()
        for ((key, value) in allEntries) {
            //stringBuilder.append("Key: $key, Value: $value\n")
            stringBuilder.append("Value: $value\n")
        }
        return stringBuilder.toString()
    }

    override fun onDataReceived(data: String) {
        runOnUiThread {
            // Get current timestamp
            val timestamp = System.currentTimeMillis()


            // Format timestamp to a human-readable format
            val formattedTime = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(
                Date(timestamp)
            )

            // Append the received data and timestamp to the existing text
            textViewData.append("[$formattedTime] $data\n") // You can format the data as needed

            val values = data.split(",")
            if (values.size >= 3) {

                    // Parse each substring to float and assign to respective variables
                    val x = values[0].toFloat()
                    val y = values[1].toFloat()
                    val z = values[2].toFloat()
                if (isRecording) {
                    val tag = editTextTag.text.toString()
                    // Call startRecording function to save data with tag
                    startRecording(tag, x, y, z)

                }

                    // Now you have x, y, and z representing the three axes of the accelerometer
                    // You can use these values as needed
                // Set the values to the custom view
                findViewById<AccelerometerView>(accelerometerView).setValues(x, y, z)
                    Log.d("Accelerometer Data", "X: $x, Y: $y, Z: $z")


                }
            // Scroll to the bottom to show the latest data
           // textViewData.post { textViewData.scrollTo(0, textViewData.bottom) }
        }
    }
    private fun startRecording(tag: String, x: Float, y: Float, z: Float) {
        // Save tag with current timestamp as key
        val timestamp = System.currentTimeMillis()
        val key = "record_$timestamp"
        val values = textViewData.text.toString() // Change this to whatever format you need to save
        // Save data with tag in SharedPreferences
        sharedPreferences.edit().putString(key, "$tag,$x,$y,$z").apply()
    }



}

class DataListenerService : WearableListenerService() {
    interface DataCallback {

        fun onDataReceived(data: String)
    }
    companion object {
        private var dataCallback: DataCallback? = null

        fun setDataCallback(callback: DataCallback) {
            dataCallback = callback
        }
    }
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                if (dataItem.uri.path == Constants.DATA_PATH) {
                    val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                    val receivedData = dataMap.getString("data_key")
                    if (receivedData != null) {
                        dataCallback?.onDataReceived(receivedData)
                    }
                    Log.d("Mobile", "Received data: $receivedData")
                    // Update UI with received data
                }
            }
        }
        dataEvents.release()
    }


}
