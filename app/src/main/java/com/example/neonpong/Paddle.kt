package com.example.neonpong

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class Paddle(
    private val screenX: Int,
    private val screenY: Int,
    private val isPlayer: Boolean // True = Bottom (Player), False = Top (AI)
) {
    // Dimensions - Horizontal paddle for portrait mode
    val width = screenX / 4f  // Paddle width (horizontal)
    val height = screenY / 40f  // Paddle thickness
    
    // Position
    val rect: RectF
    private val speed = screenX / 60f // AI Max Speed per frame

    // Styling
    private val paint = Paint()

    init {
        // Set initial position - Player at BOTTOM, AI at TOP
        val startX = screenX / 2f - width / 2f
        val startY = if (isPlayer) screenY - 100f - height else 80f
        rect = RectF(startX, startY, startX + width, startY + height)

        // Neon Style - Player is MAGENTA (bottom), AI is CYAN (top)
        paint.color = if (isPlayer) Color.MAGENTA else Color.CYAN
        paint.style = Paint.Style.FILL
        // Add a glow effect
        paint.setShadowLayer(20f, 0f, 0f, paint.color)
    }

    // Move paddle based on touch (Player) - horizontal movement
    fun update(x: Float) {
        // Center paddle on finger
        var newLeft = x - width / 2f
        
        // Screen bounds check
        if (newLeft < 0) newLeft = 0f
        if (newLeft + width > screenX) newLeft = screenX - width

        rect.offsetTo(newLeft, rect.top)
    }

    // AI Logic: Improved difficulty with better prediction and reaction
    private var aiReactionDelay = 0
    private var aiTargetX = 0f
    private var aiDifficulty = 0.95f // 95% accuracy
    
    fun updateAI(ballX: Float, ballVelocityY: Float) {
        // Only react if ball is moving towards AI (at top)
        if (ballVelocityY < 0) return // Ball moving away from AI
        
        // Faster reaction - update every frame for harder AI
        aiReactionDelay++
        if (aiReactionDelay >= 2) { // Update target every 2 frames (was 3)
            aiReactionDelay = 0
            // Reduced error margin for harder AI (5% instead of 15%)
            val errorMargin = width * 0.05f * (Math.random().toFloat() - 0.5f)
            aiTargetX = ballX + errorMargin
        }
        
        val centerX = rect.centerX()
        val deadZone = width * 0.15f // Smaller dead zone (was 0.3f)

        // Move towards target with higher speed
        if (aiTargetX < centerX - deadZone) {
            rect.left -= speed * aiDifficulty // 95% speed (was 80%)
            rect.right -= speed * aiDifficulty
        } else if (aiTargetX > centerX + deadZone) {
            rect.left += speed * aiDifficulty
            rect.right += speed * aiDifficulty
        }

        // Screen bounds check for AI
        if (rect.left < 0) rect.offsetTo(0f, rect.top)
        if (rect.right > screenX) rect.offsetTo(screenX - width, rect.top)
    }

    fun draw(canvas: Canvas) {
        canvas.drawRoundRect(rect, 10f, 10f, paint)
    }
}