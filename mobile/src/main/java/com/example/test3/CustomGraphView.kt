package com.example.test3

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CustomGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val sensor1Data = mutableListOf<Float>()
    private val sensor2Data = mutableListOf<Float>()
    private val sensor3Data = mutableListOf<Float>()
    private val paint = Paint()

    init {
        paint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width
        val height = height
        val padding = 50
        val graphHeight = height - 2 * padding
        val graphWidth = width - 2 * padding

        // Draw axes
        paint.color = Color.BLACK
        paint.strokeWidth = 5f
        canvas.drawLine(padding.toFloat(), padding.toFloat(), padding.toFloat(), (height - padding).toFloat(), paint) // Y axis
        canvas.drawLine(padding.toFloat(), (height - padding).toFloat(), (width - padding).toFloat(), (height - padding).toFloat(), paint) // X axis

        // Draw sensor data
        drawGraph(canvas, sensor1Data, Color.RED, graphWidth, graphHeight, padding)
        drawGraph(canvas, sensor2Data, Color.GREEN, graphWidth, graphHeight, padding)
        drawGraph(canvas, sensor3Data, Color.BLUE, graphWidth, graphHeight, padding)
    }

    private fun drawGraph(canvas: Canvas, data: List<Float>, color: Int, graphWidth: Int, graphHeight: Int, padding: Int) {
        if (data.isEmpty()) return

        paint.color = color
        paint.strokeWidth = 3f

        val maxY = listOf(sensor1Data.maxOrNull() ?: 0f, sensor2Data.maxOrNull() ?: 0f, sensor3Data.maxOrNull() ?: 0f).maxOrNull() ?: 0f
        val scaleX = graphWidth.toFloat() / (data.size - 1)
        val scaleY = graphHeight.toFloat() / maxY

        for (i in 0 until data.size - 1) {
            val startX = padding + i * scaleX
            val startY = padding + graphHeight - (data[i] * scaleY)
            val stopX = padding + (i + 1) * scaleX
            val stopY = padding + graphHeight - (data[i + 1] * scaleY)
            canvas.drawLine(startX, startY, stopX, stopY, paint)
        }
    }

    fun setSensorData(sensor1Data: List<Float>, sensor2Data: List<Float>, sensor3Data: List<Float>) {


        this.sensor1Data.clear()
        this.sensor1Data.addAll(sensor1Data.takeLast(50))
        this.sensor2Data.clear()
        this.sensor2Data.addAll(sensor2Data.takeLast(50))
        this.sensor3Data.clear()
        this.sensor3Data.addAll(sensor3Data.takeLast(50))
        invalidate() // Refresh the view
    }
}
