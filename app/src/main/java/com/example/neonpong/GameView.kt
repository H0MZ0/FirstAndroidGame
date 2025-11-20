package com.example.neonpong

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), Runnable {

    private var thread: Thread? = null
    private var isPlaying = false
    private var surfaceHolder: SurfaceHolder = holder
    
    // Game Objects (Initialized in onSizeChanged to get screen dimensions)
    private var playerPaddle: Paddle? = null
    private var aiPaddle: Paddle? = null
    private var ball: Ball? = null
    private val particleSystem = ParticleSystem()

    // Screen Size
    private var screenX = 0
    private var screenY = 0

    // Game Logic
    private var playerScore = 0
    private var aiScore = 0
    private val winningScore = 5
    private var gameOver = false
    
    // FPS Control
    private var lastFrameTime = System.currentTimeMillis()

    // Paints
    private val textPaint = Paint()
    private val centerLinePaint = Paint()

    init {
        // Initialize text styles
        textPaint.color = Color.WHITE
        textPaint.textSize = 100f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.setShadowLayer(10f, 0f, 0f, Color.WHITE)

        centerLinePaint.color = Color.DKGRAY
        centerLinePaint.strokeWidth = 5f
    }

    // Called when the app starts and screen dimensions are calculated
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenX = w
        screenY = h
        
        // Initialize Game Objects
        playerPaddle = Paddle(screenX, screenY, isPlayer = true)
        aiPaddle = Paddle(screenX, screenY, isPlayer = false)
        ball = Ball(screenX, screenY)
    }

    // GAME LOOP
    override fun run() {
        while (isPlaying) {
            update()
            draw()
            control()
        }
    }

    private fun update() {
        if (gameOver) return
        
        val paddle = playerPaddle
        val aiP = aiPaddle
        val gameBall = ball
        
        // Safety check - ensure all objects are initialized
        if (paddle == null || aiP == null || gameBall == null) return

        // Update Objects
        gameBall.update(paddle, aiP, particleSystem)
        aiP.updateAI(gameBall.rect.centerY())
        particleSystem.update()

        // Scoring Logic
        // AI Scores (Ball goes past Left)
        if (gameBall.rect.right < 0) {
            aiScore++
            checkWin()
            gameBall.reset()
        }
        // Player Scores (Ball goes past Right)
        else if (gameBall.rect.left > screenX) {
            playerScore++
            checkWin()
            gameBall.reset()
        }
    }

    private fun checkWin() {
        if (playerScore >= winningScore || aiScore >= winningScore) {
            gameOver = true
            particleSystem.createExplosion(screenX / 2f, screenY / 2f, Color.WHITE)
        }
    }

    private fun draw() {
        if (surfaceHolder.surface.isValid) {
            val canvas: Canvas = surfaceHolder.lockCanvas()

            // 1. Clear Screen (Black background for Neon look)
            canvas.drawColor(Color.BLACK)

            val paddle = playerPaddle
            val aiP = aiPaddle
            val gameBall = ball
            
            // 2. Draw Objects
            if (paddle != null && aiP != null && gameBall != null) {
                // Draw Center Line
                canvas.drawLine(screenX / 2f, 0f, screenX / 2f, screenY.toFloat(), centerLinePaint)

                // Draw Scores
                canvas.drawText("$playerScore", screenX * 0.25f, 150f, textPaint)
                canvas.drawText("$aiScore", screenX * 0.75f, 150f, textPaint)

                // Draw Game Entities
                paddle.draw(canvas)
                aiP.draw(canvas)
                gameBall.draw(canvas)
                particleSystem.draw(canvas)

                // Draw Game Over text
                if (gameOver) {
                    val winner = if (playerScore > aiScore) "YOU WIN" else "AI WINS"
                    textPaint.textSize = 150f
                    canvas.drawText(winner, screenX / 2f, screenY / 2f, textPaint)
                    textPaint.textSize = 60f
                    canvas.drawText("Tap to Restart", screenX / 2f, screenY / 2f + 150, textPaint)
                    textPaint.textSize = 100f // reset size
                }
            }

            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }

    // Limit Frame Rate (~60 FPS)
    private fun control() {
        val targetTime = 1000 / 60L // 60 FPS = ~16.67ms per frame
        val timeMillis = System.currentTimeMillis()
        val timeElapsed = timeMillis - lastFrameTime
        val timeToSleep = targetTime - timeElapsed
        
        if (timeToSleep > 0) {
            try {
                Thread.sleep(timeToSleep)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        lastFrameTime = System.currentTimeMillis()
    }

    // Handle User Input
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (gameOver) {
                    // Reset Game on Tap
                    playerScore = 0
                    aiScore = 0
                    gameOver = false
                    ball?.reset()
                } else {
                    // Move Player Paddle
                    playerPaddle?.update(event.y)
                }
            }
        }
        return true // Consumes the touch event
    }

    // Lifecycle Methods
    fun resume() {
        isPlaying = true
        thread = Thread(this)
        thread?.start()
    }

    fun pause() {
        try {
            isPlaying = false
            thread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}