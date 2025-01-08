package com.example.drifter_telemetry

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class RotationalRateGaugeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val indicatorPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 8f // Set the width of the line inside the gauge
        style = Paint.Style.STROKE
    }

    var rotationalRate: Float = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = Math.min(centerX, centerY) - 10

        // Draw top half of the gauge circle
        val oval = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        canvas.drawArc(oval, 180f, 180f, false, paint)

        // Draw rotational rate indicator
        val angle = Math.toRadians((rotationalRate - 90).toDouble())
        val indicatorX = (centerX - radius * Math.cos(angle)).toFloat()
        val indicatorY = (centerY + radius * Math.sin(angle)).toFloat()
        canvas.drawLine(centerX, centerY, indicatorX, indicatorY, indicatorPaint)
    }

    fun updateRotationalRate(rotationalRate: Float) {
        this.rotationalRate = (-0.75 * rotationalRate).toFloat()
        invalidate()
    }
}