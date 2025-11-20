package com.example.neonpong

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import java.util.Random

class Ball(private val screenX: Int, private val screenY: Int) {

    val rect: RectF
    private val size = screenX / 60f
    
    // Velocity
    var dxdy = FloatArray(2) // Index 0 = dx, Index 1 = dy
    private var speed = screenX / 100f

    private val paint = Paint()
    private val random = Random()

    init {
        // Center the ball
        rect = RectF(0f, 0f, size, size)
        reset()

        // Neon Style
        paint.color = Color.GREEN
        paint.style = Paint.Style.FILL
        paint.setShadowLayer(15f, 0f, 0f, Color.GREEN)
    }

    fun reset() {
        rect.offsetTo(screenX / 2f - size / 2f, screenY / 2f - size / 2f)
        
        // Randomize start direction
        val directionX = if (random.nextBoolean()) 1 else -1
        val directionY = if (random.nextBoolean()) 1 else -1
        
        dxdy[0] = speed * directionX
        dxdy[1] = (speed * 0.7f) * directionY // Slightly slower vertical movement
    }

    fun update(leftPaddle: Paddle, rightPaddle: Paddle, particleSystem: ParticleSystem) {
        // Move rect
        rect.offset(dxdy[0], dxdy[1])

        // 1. Wall Collisions (Top and Bottom)
        if (rect.top < 0 || rect.bottom > screenY) {
            dxdy[1] = -dxdy[1] // Reverse Y
            // Keep inside screen
            if (rect.top < 0) rect.offsetTo(rect.left, 0f)
            if (rect.bottom > screenY) rect.offsetTo(rect.left, screenY - size)
        }

        // 2. Paddle Collisions
        // Check overlap with Player (Left)
        if (RectF.intersects(rect, leftPaddle.rect)) {
            handlePaddleCollision(leftPaddle, 1, particleSystem)
        }

        // Check overlap with AI (Right)
        if (RectF.intersects(rect, rightPaddle.rect)) {
            handlePaddleCollision(rightPaddle, -1, particleSystem)
        }
    }

    private fun handlePaddleCollision(paddle: Paddle, directionMult: Int, particleSystem: ParticleSystem) {
        // Reverse X direction
        dxdy[0] = -dxdy[0]
        
        // Increase speed slightly for difficulty, but cap at max speed
        val maxSpeedMultiplier = 2.5f
        val currentSpeed = kotlin.math.sqrt(dxdy[0] * dxdy[0] + dxdy[1] * dxdy[1])
        
        if (currentSpeed < speed * maxSpeedMultiplier) {
            dxdy[0] *= 1.05f 
            dxdy[1] *= 1.05f
        }

        // Push ball out of paddle to prevent sticking
        val newX = if (directionMult == 1) paddle.rect.right + 2f else paddle.rect.left - size - 2f
        rect.offsetTo(newX, rect.top)

        // Trigger particles
        particleSystem.createExplosion(rect.centerX(), rect.centerY(), Color.YELLOW)
    }

    fun draw(canvas: Canvas) {
        canvas.drawOval(rect, paint)
    }
}