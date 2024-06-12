package com.example.test3

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class AccelerometerView : View {
    private var xValue: Float = 0f
    private var yValue: Float = 0f
    private var zValue: Float = 0f

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 40f
        color = Color.BLACK
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setValues(x: Float, y: Float, z: Float) {
        xValue = x
        yValue = y
        zValue = z
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Determine the highest value among x, y, and z
        val maxValue = maxOf(xValue, yValue, zValue)

        // Set circle color based on the highest value
        circlePaint.color = when {
            maxValue == xValue -> Color.RED
            maxValue == yValue -> Color.GREEN
            else -> Color.BLUE
        }

        // Calculate circle radius based on the view size
        val radius = (width / 2).toFloat()

        // Draw the circle
        canvas.drawCircle(width / 2f, height / 2f, radius, circlePaint)

        // Draw text inside the circle for x, y, z values
        canvas.drawText("X: $xValue", width / 2f - 40, height / 2f, textPaint)
        canvas.drawText("Y: $yValue", width / 2f - 40, height / 2f + 40, textPaint)
        canvas.drawText("Z: $zValue", width / 2f - 40, height / 2f + 80, textPaint)
    }
}