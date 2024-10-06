package com.example.test3


// imports for getting data from wear


// Import Firebase libraries


import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.jetbrains.kotlinx.dl.dataset.OnHeapDataset
import org.json.JSONArray
import org.json.JSONObject
import org.jtransforms.fft.DoubleFFT_1D
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt


object Constants {
    const val DATA_PATH = "/Sensor_data_path"
    const val PREFS_NAME = "SensorDataPrefs"

}


class MainActivity : AppCompatActivity(), DataListenerService.DataCallback {
    //private lateinit var textViewData: TextView
    private lateinit var textViewStoredData: TextView
    private lateinit var editTextTag: EditText
    private lateinit var buttonRecord: Button
    private lateinit var buttonShowRecord: Button
    private lateinit var buttonLoadModel: Button

    private var isRecording = false
    private var currentTag =""
    private lateinit var sharedPreferences: SharedPreferences


    private lateinit var customGraphView: CustomGraphView
    // Declare lists to store sensor data
    private val sensor1Data = mutableListOf<Float>()
    private val sensor2Data = mutableListOf<Float>()
    private val sensor3Data = mutableListOf<Float>()

    private lateinit var database: DatabaseReference

    private val REQUEST_CODE_PICK_FILE = 1001
    private val REQUEST_CODE_STORAGE_PERMISSION = 100
    private val REQUEST_CODE_SELECT_FILE = 101

    private lateinit var buttonSitting: Button
    private lateinit var buttonWalking: Button
    private lateinit var buttonWaving: Button
    private lateinit var buttonWriting: Button
    private lateinit var textViewEditOptions: TextView
    private var currentRecordingButton: Button? = null



    // List to store reading within the sliding window
    private val slidingWindowData = mutableListOf<Triple<Float,Float,Float>>()
    private var windowStartTime: Long = 0

    private lateinit var accelerometerView: AccelerometerView

    private lateinit var getContent: ActivityResultLauncher<String>


    private val client = OkHttpClient()

    // Declare your TensorFlow Lite interpreter globally
    private var tfliteInterpreter: Interpreter? = null

    val startProccesTime = System.currentTimeMillis()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Define the file selection and copying logic
        val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val fileName = "trained_model.tflite" // Replace with your desired file name
                val destFile = File(filesDir, "ml/$fileName") // Path to overwrite

                // Copy the selected file to the destination
                copyFileToInternalStorage(it, destFile)
                reloadModel()
            }
        }
        customGraphView = findViewById(R.id.customGraphView)
       // textViewData = findViewById(R.id.textViewData)

        buttonWalking = findViewById(R.id.buttonWalking)
        buttonSitting = findViewById(R.id.buttonSitting)
        buttonWriting = findViewById(R.id.buttonWriting)
        buttonWaving = findViewById(R.id.buttonWaving)

        //editTextTag = findViewById(R.id.editTextTag)
        //buttonRecord = findViewById(R.id.buttonRecord)

        buttonWalking.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isRecording = true
                    currentTag = buttonWalking.text.toString()
                    buttonWalking.setBackgroundColor(Color.RED)
                    // Handle button press
                }
                MotionEvent.ACTION_UP -> {
                    isRecording = false
                    buttonWalking.setBackgroundColor(Color.rgb(173,216,230))
                    currentTag=""
                    // Handle button release
                }
            }
            true
        }
        buttonSitting.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                     isRecording = true
                    currentTag = buttonSitting.text.toString()
                    buttonSitting.setBackgroundColor(Color.RED)
                    // Handle button press
                }
                MotionEvent.ACTION_UP -> {
                    isRecording = false
                    buttonSitting.setBackgroundColor(Color.rgb(173,216,230))
                    currentTag=""
                    // Handle button release
                }
            }
            true
        }
        buttonWriting.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                     isRecording = true
                    currentTag = buttonWriting.text.toString()
                    buttonWriting.setBackgroundColor(Color.RED)
                    // Handle button press
                }
                MotionEvent.ACTION_UP -> {
                    isRecording = false
                    buttonWriting.setBackgroundColor(Color.rgb(173,216,230))
                    currentTag=""
                    // Handle button release
                }
            }
            true
        }
        buttonWaving.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                     isRecording = true
                    currentTag = buttonWaving.text.toString()
                    buttonWaving.setBackgroundColor(Color.RED)
                    // Handle button press
                }
                MotionEvent.ACTION_UP -> {
                    isRecording = false
                    buttonWaving.setBackgroundColor(Color.rgb(173,216,230))
                    currentTag=""
                    // Handle button release
                }
            }
            true
        }




        val textViewEditButtons = findViewById<TextView>(R.id.textViewEditButtons)
        textViewEditButtons.setOnClickListener {
            // Create a dialog for editing buttons
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.activity_edit_buttons)

            // Access the EditText fields
            val editTextButton1 = dialog.findViewById<EditText>(R.id.editTextButton1)
            val editTextButton2 = dialog.findViewById<EditText>(R.id.editTextButton2)
            val editTextButton3 = dialog.findViewById<EditText>(R.id.editTextButton3)
            val editTextButton4 = dialog.findViewById<EditText>(R.id.editTextButton4)

            // Pre-fill with current button values
            editTextButton1.setText(buttonWalking.text)
            editTextButton2.setText(buttonSitting.text)
            editTextButton3.setText(buttonWriting.text)
            editTextButton4.setText(buttonWaving.text)



            // Set listeners for Save and Cancel buttons
            dialog.findViewById<Button>(R.id.buttonCancel).setOnClickListener {
                dialog.dismiss() // Close the dialog
            }

            dialog.findViewById<Button>(R.id.buttonSave).setOnClickListener {
                // Save new button values
                buttonWalking.text = editTextButton1.text.toString()
                buttonSitting.text = editTextButton2.text.toString()
                buttonWriting.text = editTextButton3.text.toString()
                buttonWaving.text = editTextButton4.text.toString()

                dialog.dismiss() // Close the dialog after saving
            }

            dialog.show() // Show the dialog
        }
        sharedPreferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        textViewStoredData = findViewById(R.id.textViewStoredData)

        accelerometerView = findViewById(R.id.accelerometerView)
        // Initialize Firebase Database

        database = FirebaseDatabase.getInstance().reference

        // Load the model when the app starts
        loadModel()

         //Button to open file explorer
//      val button = findViewById<Button>(R.id.buttonSelectModel)
//        button.setOnClickListener {
//            checkStoragePermission()
//        }

        val buttonClearStoredData: Button = findViewById(R.id.buttonClearStoredData)
        buttonClearStoredData.setOnClickListener {
            clearStoredData()
            analyzeDataSet()
        }

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AppCrash", "Unhandled exception in thread ${thread.name}", throwable)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_PICK_FILE)
        }

//        buttonRecord.setOnClickListener {
//            isRecording = !isRecording
//            if (isRecording) {
//                buttonRecord.text = getString(R.string.stop_recording)
//                windowStartTime = System.currentTimeMillis() // Initialize the start time for the sliding window
//                slidingWindowData.clear() // Clear any previous data
//            } else {
//                buttonRecord.text = getString(R.string.record_button)
//
//            }
//        }
        // button for showing stored data
        buttonShowRecord = findViewById(R.id.buttonShowStoredData)
        buttonShowRecord.setOnClickListener {
            showStoredData()
        }
        // Set data callback
        DataListenerService.setDataCallback(this)

    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Get the updated button texts
            val button1Text = data?.getStringExtra("button1Text")
            val button2Text = data?.getStringExtra("button2Text")
            val button3Text = data?.getStringExtra("button3Text")
            val button4Text = data?.getStringExtra("button4Text")

            // Update buttons with new text
            buttonWalking.text = button1Text
            buttonSitting.text = button2Text
            buttonWriting.text = button3Text
            buttonWaving.text = button4Text
        }
    }

    // Function to load the TensorFlow Lite model
    private fun loadModel() {
        val mlDir = File(applicationContext.filesDir, "ml")
        val modelFile = File(mlDir, "trained_model.tflite")

        if (modelFile.exists()) {
            try {
                val modelBuffer = modelFile.inputStream().readBytes()
                val model = ByteBuffer.allocateDirect(modelBuffer.size).order(ByteOrder.nativeOrder())
                model.put(modelBuffer)
                tfliteInterpreter = Interpreter(model)

                Toast.makeText(this, "Model loaded successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Model file not found", Toast.LENGTH_SHORT).show()
        }
    }
    // Function to override the model and reload it
    private fun overrideModelFile(uri: Uri) {
        val mlDir = File(applicationContext.filesDir, "ml")
        if (!mlDir.exists()) {
            mlDir.mkdir()
        }
        val modelFile = File(mlDir, "trained_model.tflite")

        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(modelFile)

            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            inputStream?.close()
            outputStream.close()

            Toast.makeText(this, "Model file replaced successfully", Toast.LENGTH_SHORT).show()

            // Reload the TensorFlow Lite model
            loadModel()

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error replacing model file", Toast.LENGTH_SHORT).show()
        }
    }

    // Step 1: Check Storage Permission
    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_STORAGE_PERMISSION
            )
        } else {
            openFileExplorer()
        }
    }
    // Step 2: Open File Explorer to select a .tflite file
    private fun openFileExplorer() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"  // MIME type for binary files like .tflite
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream"))
        }
        startActivityForResult(intent, REQUEST_CODE_SELECT_FILE)
    }
    // Step 3: Handle file selection
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == Activity.RESULT_OK) {
//            data?.data?.let { uri ->
//                // Process the selected .tflite file
//                overrideModelFile(uri)
//            }
//        }
//    }

    // Step 4: Override the model file in the app's 'ml' directory

    // Step 6: Reload the app to apply the new model
    private fun reloadApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()  // Close the current activity
    }
    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openFileExplorer()
        } else {
            Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    // Helper function to copy the selected file to internal storage
    private fun copyFileToInternalStorage(selectedFileUri: Uri, destFile: File) {
        contentResolver.openInputStream(selectedFileUri)?.use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }


    private fun clearStoredData() {
        // Clear all stored data from SharedPreferences
        sharedPreferences.edit().clear().apply()
        // Clear the textViewStoredData
        textViewStoredData.text = ""
    }
    private fun showStoredData() {
        val storedData = getAllStoredData()
        // Example call to the function
        //analyzeDataSet()

        trainModel()
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
    private fun roundToThreeDecimalPlaces(value: Float): Float {
        return String.format("%.3f", value).toFloat()
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
            //textViewData.append("[$formattedTime] $data\n") // You can format the data as needed
            // Split the data into smaller segments (simulating real-time updates)
            val values = try {
                data.split(",").mapNotNull { it.toFloatOrNull() }
            } catch (e: Exception) {
                Log.e("Mobile-app-DataParsing", "Failed to parse data: $data", e)
                emptyList()
            }
            if (values.size % 3 == 0) {
              // Ensure complete (x, y, z) sets
                // Clear previous second's data
                sensor1Data.clear()
                sensor2Data.clear()
                sensor3Data.clear()
                // Populate the buffer with received values
                for (i in values.indices step 3) {
                    sensor1Data.add(roundToThreeDecimalPlaces(values[i]))
                    sensor2Data.add(roundToThreeDecimalPlaces(values[i + 1]))
                    sensor3Data.add(roundToThreeDecimalPlaces(values[i + 2]))
                }
                // Simulate real-time display over one second
                Log.d("Mobile-app-test", "Reached point X with value: $sensor1Data")
                Log.d("Mobile-app-test", "Reached point X with value: $sensor2Data")
                Log.d("Mobile-app-test", "Reached point X with value: $sensor3Data")
                // Update the graph view with the new data
                simulateRealTimeDisplay()

                    val minSize = minOf(sensor1Data.size, sensor2Data.size, sensor3Data.size)
                    // Add data points to slidingWindowData from the synchronized sensor data lists
                    for (i in 0 until minSize) {
                        slidingWindowData.add(Triple(sensor1Data[i], sensor2Data[i], sensor3Data[i]))
                    }
                        processSlidingWindow(this)
                       windowStartTime = timestamp // Reset the window start time
                        slidingWindowData.clear() // Clear the list for the next window

                }
            // Scroll to the bottom to show the latest data
           // textViewData.post { textViewData.scrollTo(0, textViewData.bottom) }
        }
    }

    private val accumulatedSensor1Data = mutableListOf<Float>()
    private val accumulatedSensor2Data = mutableListOf<Float>()
    private val accumulatedSensor3Data = mutableListOf<Float>()
    private fun simulateRealTimeDisplay() {
        // Launch a coroutine to simulate real-time graph updates

        lifecycleScope.launch {
            val totalSteps = sensor1Data.size
            Log.d("AnalyzeDataSet", "working on current packet - amount of reading is : $totalSteps")

            val delayPerStep = (1000L / totalSteps).coerceAtLeast(10L) // Ensure a minimum delay to avoid overwhelming the UI

            // Circular buffer implementation for accumulated data
            val maxSize = 101
            for (i in sensor1Data.indices) {
                accumulatedSensor1Data.add(sensor1Data[i])
                accumulatedSensor2Data.add(sensor2Data[i])
                accumulatedSensor3Data.add(sensor3Data[i])

                // Maintain max size by trimming the oldest data
                if (accumulatedSensor1Data.size > maxSize) {
                    accumulatedSensor1Data.removeAt(0)
                    accumulatedSensor2Data.removeAt(0)
                    accumulatedSensor3Data.removeAt(0)
                }
                // Update the graph on the main thread
                withContext(Dispatchers.Main) {
                    customGraphView.setSensorData(
                        accumulatedSensor1Data,
                        accumulatedSensor2Data,
                        accumulatedSensor3Data
                    )
                }
                // Delay for next update to simulate real-time
                delay(delayPerStep)
            }


        }
    }
    private fun processSlidingWindow(context: Context) {
        //val tag = editTextTag.text.toString()
        val tag = "testing"
        Log.d("testing-press-fetuare", "current tag is : ${currentTag}")

        // Find the amount of values we have in the window


        val readingAmount = slidingWindowData.size
        Log.d("FeatureComputation-packet-size", "working on current packet - amount of reading is : $readingAmount")

        val elapsedSeconds = (System.currentTimeMillis()- startProccesTime) / 1000

        if (elapsedSeconds.toInt() % 10 == 0) {
            Log.d("FeatureComputation-packet-time", "working for  : $elapsedSeconds")
        }


        // Use the window's start timestamp to store the aggregated data
        if(readingAmount>=15) {

            // Time-domain features
            val startTime = System.currentTimeMillis()
            Log.d("FeatureComputation", "Feature computation started at: $startTime")
            val timeDomainStart = System.currentTimeMillis()
            val meanX = slidingWindowData.map { it.first }.average()
            val meanY = slidingWindowData.map { it.second }.average()
            val meanZ = slidingWindowData.map { it.third }.average()

            val stdX = slidingWindowData.map { it.first }.standardDeviation()
            val stdY = slidingWindowData.map { it.second }.standardDeviation()
            val stdZ = slidingWindowData.map { it.third }.standardDeviation()

            val maxX = slidingWindowData.maxOf { it.first }
            val maxY = slidingWindowData.maxOf { it.second }
            val maxZ = slidingWindowData.maxOf { it.third }

            val minX = slidingWindowData.minOf { it.first }
            val minY = slidingWindowData.minOf { it.second }
            val minZ = slidingWindowData.minOf { it.third }

            val rangeX = maxX - minX
            val rangeY = maxY - minY
            val rangeZ = maxZ - minZ

            val rmsX = sqrt(slidingWindowData.map { it.first * it.first }.average())
            val rmsY = sqrt(slidingWindowData.map { it.second * it.second }.average())
            val rmsZ = sqrt(slidingWindowData.map { it.third * it.third }.average())

            val timeDomainEnd = System.currentTimeMillis()
            Log.d("FeatureComputation", "Time-domain features computed in: ${timeDomainEnd - timeDomainStart} ms")


            // Frequency-domain features using FFT
            val frequencyDomainStart = System.currentTimeMillis()
            val fftX = calculateFFT(slidingWindowData.map { it.first })
            //logFFTData(fftX)
            val fftY = calculateFFT(slidingWindowData.map { it.second })
            val fftZ = calculateFFT(slidingWindowData.map { it.third })
            val dominantFrequencyX = findDominantFrequency(fftX)
            val dominantFrequencyY = findDominantFrequency(fftY)
            val dominantFrequencyZ = findDominantFrequency(fftZ)

            val spectralEnergyX = calculateSpectralEnergy(fftX)
            val spectralEnergyY = calculateSpectralEnergy(fftY)
            val spectralEnergyZ = calculateSpectralEnergy(fftZ)

            val spectralEntropyX = calculateSpectralEntropy(fftX)
            val spectralEntropyY = calculateSpectralEntropy(fftY)
            val spectralEntropyZ = calculateSpectralEntropy(fftZ)

            val spectralCentroidX = calculateSpectralCentroid(fftX)
            val spectralCentroidY = calculateSpectralCentroid(fftY)
            val spectralCentroidZ = calculateSpectralCentroid(fftZ)

            val frequencyDomainEnd = System.currentTimeMillis()
            Log.d("FeatureComputation", "Frequency-domain features computed in: ${frequencyDomainEnd - frequencyDomainStart} ms")
            // Total computation time
            val endTime = System.currentTimeMillis()
            // Construct JSON string
            val jsonString = """
    {
       
        "maxX": $maxX,
        "maxY": $maxY,
        "maxZ": $maxZ,
        "meanX": $meanX,
        "meanY": $meanY,
        "meanZ": $meanZ,
        "stdX": $stdX,
        "stdY": $stdY,
        "stdZ": $stdZ,
        "minX": $minX,
        "minY": $minY,
        "minZ": $minZ,
        "rangeX": $rangeX,
        "rangeY": $rangeY,
        "rangeZ": $rangeZ,
        "rmsX": $rmsX,
        "rmsY": $rmsY,
        "rmsZ": $rmsZ,
        "dominantFrequencyX": $dominantFrequencyX,
        "dominantFrequencyY": $dominantFrequencyY,
        "dominantFrequencyZ": $dominantFrequencyZ,
        "spectralEnergyX": $spectralEnergyX,
        "spectralEnergyY": $spectralEnergyY,
        "spectralEnergyZ": $spectralEnergyZ,
        "spectralEntropyX": $spectralEntropyX,
        "spectralEntropyY": $spectralEntropyY,
        "spectralEntropyZ": $spectralEntropyZ,
        "spectralCentroidX": $spectralCentroidX,
        "spectralCentroidY": $spectralCentroidY,
        "spectralCentroidZ": $spectralCentroidZ
    }
        """.trimIndent()
            Log.d("FeatureComputation", "Total feature computation time: ${endTime - startTime} ms")
           // startRecording(windowStartTime, tag,readingAmount, maxX, maxY, maxZ , meanX , meanY , meanZ
           //                                         ,stdX , stdY , stdZ , minX , minY , minZ,
           //                                          rangeX , rangeY , rangeZ , rmsX , rmsY , rmsZ
            //)


            if (isRecording && currentTag != "") {
                startRecording(windowStartTime, currentTag, readingAmount, jsonString) }
            val key = "record_$windowStartTime"
            sharedPreferences.edit().putString(key,jsonString ).apply()

            // Prepare the feature vector for the model
            val featureVector = floatArrayOf(
                meanX.toFloat(), meanY.toFloat(), meanZ.toFloat(),
                stdX.toFloat(), stdY.toFloat(), stdZ.toFloat(),
                minX.toFloat(), minY.toFloat(), minZ.toFloat(),
                maxX.toFloat(), maxY.toFloat(), maxZ.toFloat(),
                rangeX.toFloat(), rangeY.toFloat(), rangeZ.toFloat(),
                rmsX.toFloat(), rmsY.toFloat(), rmsZ.toFloat(),
                dominantFrequencyX.toFloat(), dominantFrequencyY.toFloat(), dominantFrequencyZ.toFloat(),
                spectralEnergyX.toFloat(), spectralEnergyY.toFloat(), spectralEnergyZ.toFloat(),
                spectralEntropyX.toFloat(), spectralEntropyY.toFloat(), spectralEntropyZ.toFloat(),
                spectralCentroidX.toFloat(), spectralCentroidY.toFloat(), spectralCentroidZ.toFloat()
            )

            // Log the size of the feature vector
            Log.d("FeatureComputation-test", "Feature vector size: ${featureVector.size}")

            sendFeaturesToServer(featureVector) { topPredictions ->
                // Extract the index of the first prediction
                val highestIndex = topPredictions[0].first

                // Map the index to the tag
                val predictedTag = numberToTag[highestIndex]

                // Log the most likely prediction and the corresponding tag
                Log.d("FeatureComputation-test-online", "Most likely prediction index: $highestIndex")
                Log.d("FeatureComputation-test-online", "@@: $topPredictions")
                Log.d("FeatureComputation-test-online", "Predicted tag: $predictedTag")

                // Update the AccelerometerView with the predicted tag
                accelerometerView.setTextValue(predictedTag.toString() , topPredictions[0].second.toString())
            }



            // Load the TensorFlow Lite model
            CoroutineScope(Dispatchers.Main).launch {
                val model = loadModel(context)



                // Prepare input tensor buffer
                val inputFeature0 =
                    TensorBuffer.createFixedSize(intArrayOf(1, 30), DataType.FLOAT32)
                inputFeature0.loadArray(featureVector)



                Log.d("FeatureComputation-test", "Expected input size for model: ${inputFeature0.shape.joinToString(", ")}")

                val numTags = numberToTag.size
                // Run model inference
                val outputFeature0 =
                    TensorBuffer.createFixedSize(intArrayOf(1, numTags), DataType.FLOAT32)
                model.run(inputFeature0.buffer, outputFeature0.buffer.rewind())


                // Process the model output
                val result = outputFeature0.floatArray
                Log.d("FeatureComputation-test", "Inference result: ${result.joinToString(", ")}")

                // Find the index of the maximum value
                val maxIndex = result.indices.maxByOrNull { result[it] } ?: -1


                // Map the index to the tag
                val predictedTag = numberToTag[maxIndex]

                // Log the most likely prediction and the corresponding tag
                Log.d("FeatureComputation-test", "Most likely prediction index: $maxIndex")
                Log.d("FeatureComputation-test", "Predicted tag: $predictedTag")

                // Update the AccelerometerView with the predicted tag
                //accelerometerView.setTextValue(predictedTag.toString())

                // Release model resources if no longer used
                model.close()
            }



        }
    }
    // Function to send the feature vector to the Flask server
    private fun sendFeaturesToServer(featureVector: FloatArray, callback: (List<Pair<Int, Double>>) -> Unit) {
        // Flask server URL (replace with your server's actual address)
        val url = "https://yuvalPerets.pythonanywhere.com/predict"

        // Create JSON body with the features
        val featuresJsonArray = JSONArray()
        featureVector.forEach { featuresJsonArray.put(it) }

        val jsonObject = JSONObject()
        jsonObject.put("features", JSONArray().put(featuresJsonArray)) // Add the feature vector to the "features" field

        // Convert the JSON object to a string
        val requestBody = jsonObject.toString()

        // Create OkHttp client
        val client = OkHttpClient()

        // Create the POST request
        val body: RequestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), requestBody)
        val request: Request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        // Send the request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure (e.g., log the error)
                Log.e("HttpError", "Failed to send features: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        // Handle unsuccessful response (log the error)
                        Log.e("HttpError", "Unexpected code $response")
                    } else {
                        val responseBody = response.body?.string()
                        Log.d("HttpResponse", "Prediction result: $responseBody")

                        // Parse the response as JSON
                        val jsonObject = JSONObject(responseBody)
                        val predictionsArray = jsonObject.getJSONArray("predictions").getJSONArray(0)

                        // Create a list of pairs (index, value)
                        val predictions = mutableListOf<Pair<Int, Double>>()
                        for (i in 0 until predictionsArray.length()) {
                            predictions.add(Pair(i, predictionsArray.getDouble(i)))
                        }

                        // Sort the predictions by value in descending order
                        val top3 = predictions.sortedByDescending { it.second }.take(3)

                        // Convert the top 3 values to percentages (0-100%)
                        val top3Percentages = top3.map { Pair(it.first, it.second * 100) }

                        // Log the top 3 percentages with their original indices
                        Log.d("HttpResponseTopPredictions", "Top 3 predictions: ${top3Percentages.map { "Index: ${it.first}, Value: %.2f%%".format(it.second) }}")

                        // Return the entire object with the top 3 predictions via the callback
                        callback(top3Percentages)
                    }
                }
            }
        })
    }

    // Load TensorFlow Lite model in a background thread

    private fun reloadModel() {
        val modelFile = File(filesDir, "ml/trained_model.tflite")
        if (modelFile.exists()) {
            // Reinitialize the TensorFlow Lite Interpreter
            val interpreter = Interpreter(modelFile)
            // Reassign the interpreter to the one you use in the app
            // Use this interpreter in your inference logic
        } else {
            Log.e("ModelLoader", "Model file not found")
        }
    }
    private suspend fun loadModel(context: Context): Interpreter {
    //private suspend fun loadModel(context: Context, modelPath: String): Interpreter {

        return withContext(Dispatchers.IO) {
            val modelFile = loadModelFile(context, "trained_model.tflite")
           //val modelFile = loadModelFile(context, modelPath)
            Interpreter(modelFile)
        }
    }
    // New overload to load a model from a file
    private suspend fun loadModel(context: Context, modelFile: File): Interpreter {
        return withContext(Dispatchers.IO) {
            Interpreter(modelFile)
        }
    }




    // Helper function to load the model file
    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val fileInputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    private fun logFFTData(fftData: DoubleArray) {
        val halfSize = fftData.size / 2
        for (i in 0 until halfSize) {
            val realPart = fftData[2 * i]
            val imaginaryPart = fftData[2 * i + 1]
            Log.d("FeatureComputation", "Index: $i, Real: $realPart, Imaginary: $imaginaryPart")
        }
    }

    // Function to export input and output data to a CSV file

    fun exportDataAndSendRequest(context: Context, input: List<List<Double>>, output: List<Int>) {
        val csvData = buildCsv(input, output)

        // Create temporary CSV file
        val tempFile = File.createTempFile("training_data", ".csv", context.cacheDir)
        tempFile.writeText(csvData)

        // Use coroutines to perform network request on background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("ModelDownload", "save point 2")
                // Log the labels you're passing (the output list)


                // Create the POST request to send the CSV file
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", tempFile.name, tempFile.asRequestBody("text/csv".toMediaTypeOrNull()))
                    .build()

                val request = Request.Builder()
                    .url("https://yuvalPerets.pythonanywhere.com/process_csv") // Ensure the URL is correct
                    .post(requestBody)
                    .build()

                Log.d("ModelDownload", "save point 3")


                // Execute the request and handle response
                val response = client.newCall(request).execute()

                // Switch to Main thread for UI updates
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        response.body?.let { responseBody ->
                            Log.d("ModelDownload", "save point 4")
                            try {
                                val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                val modelFile = File(downloadsDirectory, "trained_model.tflite")

                                // Move file operations to IO context
                                withContext(Dispatchers.IO) {
                                    val inputStream = responseBody.byteStream()
                                    val outputStream = FileOutputStream(modelFile)
                                    Log.d("ModelDownload", "save point 5")

                                    inputStream.copyTo(outputStream)
                                    outputStream.close()
                                    inputStream.close()
                                }


                                Log.d("ModelDownload", "Model saved to ${modelFile.absolutePath}")
                            } catch (e: Exception) {
                                Log.e("ModelDownload", "Failed to save model: ", e)
                            }
                        }
                    } else {
                        Log.e("ModelDownload", "Server returned error: ${response.message}")
                    }
                }
            } catch (e: IOException) {
                Log.e("ModelDownload", "Failed to send file: ${e.message}")
            }
        }
    }

    // Helper function to build the CSV data
    private fun buildCsv(input: List<List<Double>>, output: List<Int>): String {
        val sb = StringBuilder()
        input.forEachIndexed { index, row ->
            val line = row.joinToString(",") + "," + output[index]
            sb.append(line).append("\n")
        }
        return sb.toString()
    }
    private fun exportDataToDownloads(context: Context,input: List<List<Double>>, output: List<Int>) {
        // Use the standard Downloads folder
        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        // Create the file in the Downloads folder
        val dataFile = File(downloadsDirectory, "training_data.csv")
        try {
            dataFile.printWriter().use { out ->
                input.forEachIndexed { index, row ->
                    val line = row.joinToString(",") + "," + output[index]
                    out.println(line)
                }
            }
            Log.d("AnalyzeDataSet", "Data exported to ${dataFile.absolutePath}")
        } catch (e: IOException) {
            Log.e("DataExport", "Failed to save file: ${e.message}")
        }
    }


    private val tagToNumber = mutableMapOf<String, Int>()
    private val numberToTag = mutableMapOf<Int, String>()
    private suspend fun fetchDataFromFirebase(context: Context): Pair<List<List<Double>>, List<Int>> = withContext(Dispatchers.IO) {
        val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("sensorData")
        val input = mutableListOf<List<Double>>()
        val output = mutableListOf<String>()


        val task = CompletableDeferred<Pair<List<List<Double>>, List<Int>>>()

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                input.clear()
                output.clear()

                for (dataSnapshot in snapshot.children) {
                    val data = dataSnapshot.child("data").getValue(String::class.java)
                    val tag = dataSnapshot.child("tag").getValue(String::class.java)

                    if (data != null && tag != null) {
                        val jsonObject = JSONObject(data)
                        val dataPoint = listOf(
                            jsonObject.getDouble("maxX"),
                            jsonObject.getDouble("maxY"),
                            jsonObject.getDouble("maxZ"),
                            jsonObject.getDouble("meanX"),
                            jsonObject.getDouble("meanY"),
                            jsonObject.getDouble("meanZ"),
                            jsonObject.getDouble("stdX"),
                            jsonObject.getDouble("stdY"),
                            jsonObject.getDouble("stdZ"),
                            jsonObject.getDouble("minX"),
                            jsonObject.getDouble("minY"),
                            jsonObject.getDouble("minZ"),
                            jsonObject.getDouble("rangeX"),
                            jsonObject.getDouble("rangeY"),
                            jsonObject.getDouble("rangeZ"),
                            jsonObject.getDouble("rmsX"),
                            jsonObject.getDouble("rmsY"),
                            jsonObject.getDouble("rmsZ"),
                            jsonObject.getDouble("dominantFrequencyX"),
                            jsonObject.getDouble("dominantFrequencyY"),
                            jsonObject.getDouble("dominantFrequencyZ"),
                            jsonObject.getDouble("spectralEnergyX"),
                            jsonObject.getDouble("spectralEnergyY"),
                            jsonObject.getDouble("spectralEnergyZ"),
                            jsonObject.getDouble("spectralEntropyX"),
                            jsonObject.getDouble("spectralEntropyY"),
                            jsonObject.getDouble("spectralEntropyZ"),
                            jsonObject.getDouble("spectralCentroidX"),
                            jsonObject.getDouble("spectralCentroidY"),
                            jsonObject.getDouble("spectralCentroidZ")
                        )
                        input.add(dataPoint)
                        output.add(tag)
                    }
                }

                val uniqueTags = output.distinct()
                tagToNumber.clear()
                numberToTag.clear()
                uniqueTags.forEachIndexed { index, tag ->
                    tagToNumber[tag] = index
                    numberToTag[index] = tag
                }
                val encodedOutput = output.map { tagToNumber[it] ?: -1 }

                Log.d("FirebaseData", "Data Point: $input")
                Log.d("FirebaseData", "Tag: $output")
                Log.d("FirebaseData", "Output array size: ${output.size}")

                Log.d("FirebaseData", "Tag to Number Mapping: $tagToNumber")
                Log.d("FirebaseData", "Number to Tag Mapping: $numberToTag")
                Log.d("FirebaseData", "Encoded Output: $encodedOutput")

                // Export data after fetching from Firebase

                exportDataToDownloads(context, input, encodedOutput)
                exportDataAndSendRequest(context, input, encodedOutput)


                task.complete(Pair(input, encodedOutput))

            }

            override fun onCancelled(error: DatabaseError) {
                task.completeExceptionally(Exception("Error fetching data: ${error.message}"))
            }
        })

        task.await()
    }
    fun loadModelFile(filePath: String): ByteBuffer {
        val file = FileInputStream(filePath).channel
        val buffer = file.map(FileChannel.MapMode.READ_ONLY, 0, file.size())
        file.close()
        return buffer
    }



    // Converts the input and encoded output to a dataset format
    fun convertToDataset(input: List<List<Double>>, encodedOutput: List<Int>): OnHeapDataset {
        // Convert input from List<List<Double>> to List<DoubleArray> and then to Array<FloatArray>
        val inputArray = input.map { it.toDoubleArray().map(Double::toFloat).toFloatArray() }.toTypedArray()

        // Convert output from List<Int> to FloatArray
        val outputArray = encodedOutput.map { it.toFloat() }.toFloatArray()

        // Create the dataset
        return OnHeapDataset.create(
            features = inputArray,
            labels = outputArray
        )
    }

    private fun trainModel ()  {

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val (input, encodedOutput) = fetchDataFromFirebase(this@MainActivity)
                /*
                Log.d("ModelTraining", "Data processing complete. Proceeding with model training.")
                Log.d("ModelTraining", "Data processing complete. input : $input.")
                Log.d("ModelTraining", "Data processing complete. output: $encodedOutput.")
                // Example: Train your model here with the input and encodedOutput
                val numFeatures = input[0].size
                // Convert input and output to Dataset
                val trainingData = convertToDataset(input, encodedOutput)
                Log.d("ModelTraining", "Dataset conversion complete: $trainingData")

                Log.d("ModelTraining", "stopped here.")

                val basicModel = Sequential.of(
                    Input(30),  // Input layer with 30 features
                    Dense(10, activation = Activations.Relu),  // A single hidden layer with 10 units
                    Dense(3, activation = Activations.Softmax)  // Output layer with 3 units (assuming 3 classes)
                )
                Log.d("ModelTraining", "stopped here1.5.")

                val model = Sequential.of(
                    Input(30),
                    Flatten(),
                    Dense(256, activation = Activations.Relu),
                    Dense(128, activation = Activations.Relu),
                    Dense(3, activation = Activations.Softmax)
                )

                Log.d("ModelTraining", "stopped here2.")
                model.compile(
                    optimizer = Adam(clipGradient =  ClipGradientByValue(0.1f)),
                    loss = Losses.SOFT_MAX_CROSS_ENTROPY_WITH_LOGITS,
                    metric = Metrics.ACCURACY
                )

                Log.d("ModelTraining", "stopped here3.")
                // Training parameters
                val epochs = 10
                val batchSize = 5

                for (epoch in 0 until epochs) {
                    Log.d("ModelTraining", "Epoch $epoch: Training started")

                    // Train for one epoch
                    model.fit(
                        trainingData,
                        epochs = 1, // Train for one epoch at a time
                        batchSize = batchSize
                    )
                    // Assuming you have a method to evaluate metrics after training
                    val accuracy = model.evaluate(dataset = trainingData)// Replace with actual method if available
                    Log.d("ModelTraining", "Epoch $epoch: , Accuracy = $accuracy")
                }
                */
                Log.d("ModelTraining", "Model training complete.")

            } catch (e: Exception) {
                Log.e("ModelTraining", "Error: ${e.message}")
            }
        }
    }


    private fun startRecording(timestamp: Long,  tag: String, numReadings: Int , jsonString: String)
    {
        // Save tag with current timestamp as key
        val key = "record_$timestamp"

        // Save data with tag in SharedPreferences
        // Store the max values along with the tag in SharedPreferences
        //sharedPreferences.edit().putString(key, "$numReadings readings,$tag,$maxX,$maxY,$maxZ").apply()
        sharedPreferences.edit().putString(key,jsonString ).apply()

        // Create a unique key using the timestamp or any other unique identifier
        val keyForInstance = database.child("sensorData").push().key ?: return

        // Create a data map to store the information
        val dataMap = mapOf(
            "timestamp" to timestamp,
            "tag" to tag,
            "numReadings" to numReadings,
            "data" to jsonString
        )

        // Save the data to Firebase under the "sensorData" node
        database.child("sensorData").child(keyForInstance).setValue(dataMap)
            .addOnSuccessListener {
                Log.d("Firebase", "Data saved successfully.")
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Failed to save data", e)
            }
    }
    private fun analyzeDataSet() {
        // Reference to the sensor data node in Firebase
        val sensorDataRef = database.child("sensorData")
        Log.d("AnalyzeDataSet", "stopped here")

        // Retrieve all data from the sensorData node
        sensorDataRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val dataSnapshot = task.result
                if (dataSnapshot != null && dataSnapshot.exists()) {
                    // Use a HashSet to track unique tags
                    val uniqueTags = HashSet<String>()
                    var totalLines = 0

                    // Iterate through each child in the snapshot
                    for (data in dataSnapshot.children) {
                        // Get the tag from each entry (assuming it's saved under "tag")
                        val tag = data.child("tag").getValue(String::class.java)
                        if (tag != null) {
                            uniqueTags.add(tag) // Add the tag to the set for uniqueness
                        }
                        totalLines++ // Count each line
                    }

                    // Output the number of unique tags and total lines
                    Log.d("AnalyzeDataSet", "Number of different tags: ${uniqueTags.size}")
                    Log.d("AnalyzeDataSet", "Total number of lines saved: $totalLines")

                } else {
                    Log.d("AnalyzeDataSet", "No data found in the dataset.")
                }
            } else {
                Log.e("AnalyzeDataSet", "Failed to read data", task.exception)
            }
        }
    }

    // Helper function to calculate standard deviation
    private fun List<Float>.standardDeviation(): Double {
        val mean = this.average()
        return sqrt(this.map { (it - mean).pow(2) }.average())
    }

    // Helper function to calculate variance
    private fun List<Float>.variance(): Double {
        val mean = this.average()
        return this.map { (it - mean).pow(2) }.average()
    }
    private fun padToPowerOfTwo(data: List<Float>): DoubleArray {
        val size = data.size
        val powerOfTwo = 1 shl (31 - Integer.numberOfLeadingZeros(size - 1)) // Next power of 2
        return DoubleArray(powerOfTwo) { index -> data.getOrElse(index) { 0.0 }.toDouble() }
    }

    // Helper function to calculate FFT
    private fun calculateFFT(data: List<Float>): DoubleArray {
        val paddedData = padToPowerOfTwo(data)
        val fft = DoubleFFT_1D(paddedData.size.toLong())
        val startTime = System.currentTimeMillis()
        fft.realForward(paddedData)
        val endTime = System.currentTimeMillis()
        Log.d("FeatureComputation", "FFT computation time: ${endTime - startTime} ms")
        return paddedData
    }

    // Find the dominant frequency
    private fun findDominantFrequency(fftData: DoubleArray): Double {
        var maxIndex = 0
        var maxMagnitude = 0.0
        for (i in 1 until fftData.size / 2) {
            val magnitude = sqrt(fftData[2 * i] * fftData[2 * i] + fftData[2 * i + 1] * fftData[2 * i + 1])
            if (magnitude > maxMagnitude) {
                maxMagnitude = magnitude
                maxIndex = i
            }
        }
        return maxIndex.toDouble() // Convert index to actual frequency as needed
    }

    // Calculate spectral energy
    private fun calculateSpectralEnergy(fft: DoubleArray): Double {
        return fft.map { it * it }.average()
    }

    // Calculate spectral entropy
    private fun calculateSpectralEntropy(fftData: DoubleArray): Double {
        val magnitudes = fftData.asList()
            .chunked(2) // FFT data is interleaved: real and imaginary parts
            .map { (real, imaginary) -> sqrt(real * real + imaginary * imaginary) }
            .filter { it.isFinite() }

        val total = magnitudes.sum()
        if (total == 0.0) return 0.0 // Handle zero division case

        val probabilities = magnitudes.map { it / total }
        return -probabilities.sumOf { p ->
            if (p > 0) p * ln(p) else 0.0 // Avoid NaN for non-positive probabilities
        }
    }

    // Calculate spectral centroid
    private fun calculateSpectralCentroid(fftData: DoubleArray): Double {
        val magnitudes = fftData.toList().chunked(2).map { sqrt(it[0] * it[0] + it[1] * it[1]) }
        val weightedSum = magnitudes.mapIndexed { index, value -> index * value }.sum()
        return weightedSum / magnitudes.sum()
    }
    // Helper function to calculate standard deviation
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
                    Log.d("Mobile-app", "Received data: $receivedData")
                    // Update UI with received data
                }
            }
        }
        dataEvents.release()
    }



}
