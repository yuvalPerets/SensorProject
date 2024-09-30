package com.example.test3

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class AccelerometerView : View {
    private var textValue: String = ""

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.CYAN // Default circle color
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

    fun setTextValue(text: String) {
        textValue = text
        invalidate() // Redraw the view to reflect the new string
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calculate circle radius based on the view size
        val radius = (width / 2).toFloat()

        // Draw the circle
        canvas.drawCircle(width / 2f, height / 2f, radius, circlePaint)

        // Draw the string inside the circle
        canvas.drawText(textValue, width / 2f, height / 2f, textPaint)
    }
}
