package com.example.neonpong

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class Paddle(
    private val screenX: Int,
    private val screenY: Int,
    private val isPlayer: Boolean // True = Left, False = Right
) {
    // Dimensions
    val width = screenX / 30f
    val height = screenY / 5f
    
    // Position
    val rect: RectF
    private val speed = screenY / 60f // AI Max Speed per frame

    // Styling
    private val paint = Paint()

    init {
        // Set initial position
        val startX = if (isPlayer) 20f else screenX - 20f - width
        val startY = screenY / 2f - height / 2f
        rect = RectF(startX, startY, startX + width, startY + height)

        // Neon Style
        paint.color = if (isPlayer) Color.CYAN else Color.MAGENTA
        paint.style = Paint.Style.FILL
        // Add a glow effect
        paint.setShadowLayer(20f, 0f, 0f, paint.color)
    }

    // Move paddle based on touch (Player)
    fun update(y: Float) {
        // Center paddle on finger
        var newTop = y - height / 2f
        
        // Screen bounds check
        if (newTop < 0) newTop = 0f
        if (newTop + height > screenY) newTop = screenY - height

        rect.offsetTo(rect.left, newTop)
    }

    // AI Logic: Move towards the ball
    fun updateAI(ballY: Float) {
        val centerY = rect.centerY()

        // Simple logic: if ball is above, move up. If below, move down.
        // We use 'speed' to limit how fast the AI can move to make it beatable.
        if (ballY < centerY - 10) {
            rect.top -= speed
            rect.bottom -= speed
        } else if (ballY > centerY + 10) {
            rect.top += speed
            rect.bottom += speed
        }

        // Screen bounds check for AI
        if (rect.top < 0) rect.offsetTo(rect.left, 0f)
        if (rect.bottom > screenY) rect.offsetTo(rect.left, screenY - height)
    }

    fun draw(canvas: Canvas) {
        canvas.drawRoundRect(rect, 10f, 10f, paint)
    }
}