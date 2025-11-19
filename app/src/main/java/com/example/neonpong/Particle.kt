package com.example.neonpong

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.util.Random

// Single Particle
class Particle(var x: Float, var y: Float, color: Int) {
    private val velocityX: Float
    private val velocityY: Float
    private var alpha: Int = 255
    private val paint = Paint()
    var isAlive = true

    init {
        val random = Random()
        // Random explosion velocity
        velocityX = (random.nextFloat() * 10) - 5
        velocityY = (random.nextFloat() * 10) - 5
        
        paint.color = color
        paint.style = Paint.Style.FILL
        // Small glow
        paint.setShadowLayer(5f, 0f, 0f, color)
    }

    fun update() {
        x += velocityX
        y += velocityY
        
        // Fade out
        alpha -= 10
        if (alpha <= 0) {
            isAlive = false
            alpha = 0
        }
        paint.alpha = alpha
    }

    fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, 5f, paint)
    }
}

// Manager Class
class ParticleSystem {
    private val particles = ArrayList<Particle>()

    fun createExplosion(x: Float, y: Float, color: Int) {
        for (i in 0..15) { // Create 15 particles
            particles.add(Particle(x, y, color))
        }
    }

    fun update() {
        // Iterate backwards to remove safely
        for (i in particles.indices.reversed()) {
            particles[i].update()
            if (!particles[i].isAlive) {
                particles.removeAt(i)
            }
        }
    }

    fun draw(canvas: Canvas) {
        for (p in particles) {
            p.draw(canvas)
        }
    }
}