package com.example.neonpong

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController

class MainActivity : Activity() {

    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get game mode from intent
        val modeName = intent.getStringExtra("GAME_MODE") ?: GameMode.VS_AI.name
        val gameMode = try {
            GameMode.valueOf(modeName)
        } catch (e: Exception) {
            GameMode.VS_AI
        }

        // Initialize the custom GameView with selected mode
        gameView = GameView(this, gameMode)
        setContentView(gameView)

        // Hide system bars for full immersion
        hideSystemUI()
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        gameView.cleanup()
    }

    private fun hideSystemUI() {
        window.setDecorFitsSystemWindows(false)
        val controller = window.insetsController
        if (controller != null) {
            controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}