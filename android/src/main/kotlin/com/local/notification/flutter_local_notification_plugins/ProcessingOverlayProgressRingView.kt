package com.local.notification.flutter_local_notification_plugins

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class ProcessingOverlayProgressRingView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : View(context, attrs, defStyleAttr) {
        private val ringPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
        private val backgroundPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.argb(128, 255, 255, 255)
            }
        private val ringRect = RectF()
        private var ringShader: Shader? = null

        var progress: Int = 0
            set(value) {
                field = value.coerceIn(0, 100)
                invalidate()
            }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            ringShader =
                LinearGradient(
                    0f,
                    0f,
                    w.toFloat(),
                    h.toFloat(),
                    Color.parseColor("#B01111"),
                    Color.parseColor("#E53131"),
                    Shader.TileMode.CLAMP,
                )
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val size = min(width, height).toFloat()
            if (size <= 0f) {
                return
            }
            val iconSize = size * 56f / 68f
            val strokeWidth = (size - iconSize).coerceAtLeast(1f) / 2f
            val inset = strokeWidth / 2f
            val radius = size / 2f
            canvas.drawCircle(radius, radius, radius, backgroundPaint)

            ringPaint.strokeWidth = strokeWidth
            ringRect.set(inset, inset, size - inset, size - inset)

            ringPaint.shader = null
            ringPaint.color = Color.argb(128, 255, 255, 255)
            canvas.drawArc(ringRect, 0f, 360f, false, ringPaint)

            if (progress <= 0) {
                return
            }
            ringPaint.shader = ringShader
            canvas.drawArc(ringRect, -90f, progress / 100f * 360f, false, ringPaint)
        }

    }
