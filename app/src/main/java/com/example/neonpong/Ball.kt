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

    fun update(topPaddle: Paddle, bottomPaddle: Paddle, particleSystem: ParticleSystem, gameView: com.example.neonpong.GameView? = null) {
        // Move rect
        rect.offset(dxdy[0], dxdy[1])

        // 1. Wall Collisions (Left and Right sides in portrait)
        if (rect.left < 0 || rect.right > screenX) {
            dxdy[0] = -dxdy[0] // Reverse X
            // Keep inside screen
            if (rect.left < 0) rect.offsetTo(0f, rect.top)
            if (rect.right > screenX) rect.offsetTo(screenX - size, rect.top)
            gameView?.playWallSound()
        }

        // 2. Paddle Collisions - AI at TOP, Player at BOTTOM
        // Check overlap with AI (Top paddle)
        if (RectF.intersects(rect, topPaddle.rect)) {
            handlePaddleCollision(topPaddle, 1, particleSystem, gameView)
        }

        // Check overlap with Player (Bottom paddle)
        if (RectF.intersects(rect, bottomPaddle.rect)) {
            handlePaddleCollision(bottomPaddle, -1, particleSystem, gameView)
        }
    }

    private fun handlePaddleCollision(paddle: Paddle, directionMult: Int, particleSystem: ParticleSystem, gameView: com.example.neonpong.GameView? = null) {
        // Reverse Y direction (vertical movement in portrait)
        dxdy[1] = -dxdy[1]
        
        // Increase speed slightly for difficulty, but cap at max speed
        val maxSpeedMultiplier = 2.5f
        val currentSpeed = kotlin.math.sqrt(dxdy[0] * dxdy[0] + dxdy[1] * dxdy[1])
        
        if (currentSpeed < speed * maxSpeedMultiplier) {
            dxdy[0] *= 1.05f 
            dxdy[1] *= 1.05f
        }

        // Push ball out of paddle to prevent sticking (vertical in portrait)
        val newY = if (directionMult == 1) paddle.rect.bottom + 2f else paddle.rect.top - size - 2f
        rect.offsetTo(rect.left, newY)

        // Play sound
        gameView?.playPaddleSound()
        
        // Trigger particles with paddle color
        particleSystem.createExplosion(rect.centerX(), rect.centerY(), Color.WHITE)
    }

    fun draw(canvas: Canvas) {
        canvas.drawOval(rect, paint)
    }
}