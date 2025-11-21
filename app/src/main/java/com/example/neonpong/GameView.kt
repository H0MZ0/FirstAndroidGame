package com.example.neonpong

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context, private var gameMode: GameMode = GameMode.VS_AI) : SurfaceView(context), Runnable {

    private var thread: Thread? = null
    private var isPlaying = false
    private var surfaceHolder: SurfaceHolder = holder
    
    // Game Objects (Initialized in onSizeChanged to get screen dimensions)
    private var playerPaddle: Paddle? = null
    private var aiPaddle: Paddle? = null
    private var ball: Ball? = null
    private val particleSystem = ParticleSystem()
    
    // Local 2-player
    private var player2TouchY = 0f

    // Screen Size
    private var screenX = 0
    private var screenY = 0

    // Game Logic
    private var playerScore = 0
    private var aiScore = 0
    private val winningScore = 5
    private var gameOver = false
    private var isPaused = false
    
    // Pause button area (top-right corner)
    private val pauseButtonSize = 100f
    private var pauseButtonX = 0f
    private var pauseButtonY = 0f
    
    // Scoring delay
    private var scoreDelay = 0
    private var isDelayActive = false
    
    // Sound Effects
    private var soundPool: android.media.SoundPool? = null
    private var paddleHitSound = 0
    private var wallHitSound = 0
    private var scoreSound = 0
    
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
        
        // Initialize sound effects
        initSounds()
    }
    
    private fun initSounds() {
        val audioAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_GAME)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
            
        soundPool = android.media.SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
            
        // Generate simple beep sounds using ToneGenerator
        // We'll use simple playback - Android will handle the sound generation
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
        
        // Pause button position (top-right corner)
        pauseButtonX = screenX - pauseButtonSize - 20f
        pauseButtonY = 20f
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
        if (gameOver || isPaused) return // Don't update when paused
        
        val paddle = playerPaddle
        val aiP = aiPaddle
        val gameBall = ball
        
        // Safety check - ensure all objects are initialized
        if (paddle == null || aiP == null || gameBall == null) return

        // Handle scoring delay
        if (isDelayActive) {
            scoreDelay++
            if (scoreDelay >= 120) { // 2 seconds at 60 FPS
                isDelayActive = false
                scoreDelay = 0
                gameBall.reset()
            }
            particleSystem.update()
            return // Don't update ball or paddles during delay
        }

        // Update Objects - AI on LEFT, Player on RIGHT
        gameBall.update(aiP, paddle, particleSystem, this)
        
        // Update top paddle based on game mode
        when (gameMode) {
            GameMode.VS_AI -> {
                aiP.updateAI(gameBall.rect.centerX(), gameBall.dxdy[1])
            }
            GameMode.LOCAL_2P -> {
                // Top paddle controlled by player 2 (top half touches)
                if (player2TouchY > 0) {
                    aiP.update(player2TouchY)
                }
            }
            GameMode.WIFI_MULTIPLAYER -> {
                // WiFi multiplayer - controlled through network (handled elsewhere)
            }
        }
        
        particleSystem.update()

        // Scoring Logic - Portrait Mode: Player at BOTTOM, AI at TOP
        // Player Scores (Ball goes past top)
        if (gameBall.rect.top < 0) {
            playerScore++
            playScoreSound()
            checkWin()
            if (!gameOver) {
                isDelayActive = true
                scoreDelay = 0
            }
        }
        // AI Scores (Ball goes past bottom)
        else if (gameBall.rect.bottom > screenY) {
            aiScore++
            playScoreSound()
            checkWin()
            if (!gameOver) {
                isDelayActive = true
                scoreDelay = 0
            }
        }
    }
    
    fun playPaddleSound() {
        try {
            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 80)
            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 50)
        } catch (e: Exception) {
            // Ignore if sound fails
        }
    }
    
    fun playWallSound() {
        try {
            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 60)
            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 30)
        } catch (e: Exception) {
            // Ignore if sound fails
        }
    }
    
    private fun playScoreSound() {
        try {
            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
            toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
        } catch (e: Exception) {
            // Ignore if sound fails
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
                // Draw Center Line (horizontal for portrait)
                canvas.drawLine(0f, screenY / 2f, screenX.toFloat(), screenY / 2f, centerLinePaint)

                // Draw Scores (left/right sides for portrait)
                canvas.drawText("$playerScore", 150f, screenY * 0.25f, textPaint)
                canvas.drawText("$aiScore", 150f, screenY * 0.75f, textPaint)

                // Draw Game Entities
                paddle.draw(canvas)
                aiP.draw(canvas)
                gameBall.draw(canvas)
                particleSystem.draw(canvas)

                // Draw Pause Button (top-right corner)
                if (!gameOver) {
                    val pausePaint = Paint().apply {
                        color = if (isPaused) Color.GREEN else Color.WHITE
                        style = Paint.Style.FILL
                        alpha = 180
                    }
                    canvas.drawRoundRect(
                        pauseButtonX, pauseButtonY, 
                        pauseButtonX + pauseButtonSize, pauseButtonY + pauseButtonSize,
                        15f, 15f, pausePaint
                    )
                    
                    // Draw pause symbol (two bars) or play symbol (triangle)
                    val symbolPaint = Paint().apply {
                        color = Color.BLACK
                        style = Paint.Style.FILL
                        strokeWidth = 8f
                    }
                    
                    if (isPaused) {
                        // Draw play triangle
                        val path = android.graphics.Path()
                        path.moveTo(pauseButtonX + 30f, pauseButtonY + 25f)
                        path.lineTo(pauseButtonX + 30f, pauseButtonY + 75f)
                        path.lineTo(pauseButtonX + 70f, pauseButtonY + 50f)
                        path.close()
                        canvas.drawPath(path, symbolPaint)
                    } else {
                        // Draw pause bars
                        canvas.drawRect(pauseButtonX + 30f, pauseButtonY + 25f, 
                                      pauseButtonX + 42f, pauseButtonY + 75f, symbolPaint)
                        canvas.drawRect(pauseButtonX + 58f, pauseButtonY + 25f, 
                                      pauseButtonX + 70f, pauseButtonY + 75f, symbolPaint)
                    }
                }
                
                // Draw Pause Overlay
                if (isPaused && !gameOver) {
                    val overlayPaint = Paint().apply {
                        color = Color.BLACK
                        alpha = 180
                    }
                    canvas.drawRect(0f, 0f, screenX.toFloat(), screenY.toFloat(), overlayPaint)
                    
                    textPaint.textSize = 120f
                    canvas.drawText("PAUSED", screenX / 2f, screenY / 2f - 100f, textPaint)
                    textPaint.textSize = 50f
                    canvas.drawText("Tap center to resume", screenX / 2f, screenY / 2f + 50f, textPaint)
                    textPaint.textSize = 100f // reset size
                }

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
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                val touchX = event.x
                val touchY = event.y
                
                // Check pause button tap (top-right corner)
                if (touchX >= pauseButtonX && touchX <= pauseButtonX + pauseButtonSize &&
                    touchY >= pauseButtonY && touchY <= pauseButtonY + pauseButtonSize && !gameOver) {
                    isPaused = !isPaused
                    return true
                }
                
                // If paused, allow resume by tapping center
                if (isPaused && !gameOver) {
                    val centerX = screenX / 2f
                    val centerY = screenY / 2f
                    if (Math.abs(touchX - centerX) < 200 && Math.abs(touchY - centerY) < 200) {
                        isPaused = false
                    }
                    return true
                }
                
                if (gameOver) {
                    // Reset Game on Tap
                    playerScore = 0
                    aiScore = 0
                    gameOver = false
                    isPaused = false
                    ball?.reset()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_POINTER_DOWN -> {
                // Don't move paddles when paused
                if (isPaused || gameOver) return true
                
                // Handle multiple touches for local 2-player
                if (gameMode == GameMode.LOCAL_2P) {
                    for (i in 0 until event.pointerCount) {
                        val x = event.getX(i)
                        val y = event.getY(i)
                        
                        // Top half = player 2 (top paddle), Bottom half = player 1 (bottom paddle)
                        if (y < screenY / 2) {
                            player2TouchY = x
                        } else {
                            playerPaddle?.update(x)
                        }
                    }
                } else {
                    // Single player modes - any touch controls player paddle
                    playerPaddle?.update(event.x)
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
    
    fun cleanup() {
        soundPool?.release()
        soundPool = null
    }
}