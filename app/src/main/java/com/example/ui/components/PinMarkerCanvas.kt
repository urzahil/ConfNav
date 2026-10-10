package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

object PinMarkerCanvas {

    fun createPinBitmap(
        colorHex: String,
        count: Int,
        isSelected: Boolean = false,
        scale: Float = 1.0f
    ): BitmapDescriptor {
        val colorInt = PinColorHelper.parseColor(colorHex).toArgb()

        val width = (48 * scale).toInt()
        val height = (64 * scale).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw shadow at bottom
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x44000000
            style = Paint.Style.FILL
        }
        val shadowRect = RectF(
            width * 0.25f,
            height * 0.88f,
            width * 0.75f,
            height * 0.98f
        )
        canvas.drawOval(shadowRect, shadowPaint)

        // Outer teardrop pin path
        val pinPath = Path()
        val radius = width * 0.44f
        val centerX = width * 0.5f
        val centerY = radius + 4f

        pinPath.arcTo(
            RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
            -180f,
            180f,
            false
        )
        // Tip of the pin
        pinPath.lineTo(centerX, height * 0.90f)
        pinPath.close()

        // Selection highlight ring
        if (isSelected) {
            val selectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 6f * scale
            }
            canvas.drawPath(pinPath, selectPaint)
        }

        // Fill pin body
        paint.color = colorInt
        paint.style = Paint.Style.FILL
        canvas.drawPath(pinPath, paint)

        // Pin border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33000000
            style = Paint.Style.STROKE
            strokeWidth = 2f * scale
        }
        canvas.drawPath(pinPath, borderPaint)

        // Inner white circle
        val innerRadius = radius * 0.58f
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, innerRadius, innerPaint)

        // Badge text: event count
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
            textSize = 14f * scale
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val textY = centerY - ((textPaint.descent() + textPaint.ascent()) / 2)
        val label = if (count > 9) "9+" else count.toString()
        canvas.drawText(label, centerX, textY, textPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
