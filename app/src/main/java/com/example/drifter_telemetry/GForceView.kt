package com.example.drifter_telemetry

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class GForceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 5f
    }

    var gForceX: Float = 0f
    var gForceY: Float = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f

        // Draw horizontal line
        canvas.drawLine(centerX - 100, centerY, centerX + 100, centerY, paint)
        // Draw vertical line
        canvas.drawLine(centerX, centerY - 100, centerX, centerY + 100, paint)

        // Draw G-force indicator
        val indicatorX = centerX - gForceX * 25
        val indicatorY = centerY + gForceY * 25
        canvas.drawCircle(indicatorX, indicatorY, 25f, paint)
    }

    fun updateGForce(gForceX: Float, gForceY: Float) {
        this.gForceX = gForceY
        this.gForceY = gForceX
        invalidate()
    }

    private fun constraint(value: Float, min: Float, max: Float): Float {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }

}