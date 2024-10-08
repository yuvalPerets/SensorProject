package com.example.test3

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class AccelerometerView : View {
    private var textValue: String = ""
    private var cleanPercent: String = ""

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.GREEN // Default circle color
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 40f
        color = Color.BLACK
        textAlign = Paint.Align.CENTER // Align text to center
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun setTextValue(tag: String, predictionChance: String) {
        cleanPercent = getIntegerPart(predictionChance)
        textValue = "$tag : $cleanPercent %"
        invalidate() // Redraw the view to reflect the new string
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calculate circle radius based on the view size
        val radius = (width / 2).toFloat()

        // Draw the circle
        canvas.drawCircle(width / 2f, height / 2f, radius, circlePaint)

        // Draw the string inside the circle
        val textLines = textValue.split("\n")
        for (i in textLines.indices) {
            canvas.drawText(
                textLines[i],
                width / 2f,
                height / 2f + (i * textPaint.textSize), // Adjust vertical position
                textPaint
            )
        }
    }
    fun getIntegerPart(value: String): String {
        return value.split(".")[0] // Split the string at the decimal point and take the first part
    }
     fun setBackgroundColor(color: String) {
        circlePaint.color = Color.parseColor(color) // Update the color of the circle
        invalidate() // Redraw the view to reflect the new color
    }
    // New method to get the current circle color
    fun getBackgroundColor(): Int {
        return circlePaint.color // Return the current color of the circle
    }
}
