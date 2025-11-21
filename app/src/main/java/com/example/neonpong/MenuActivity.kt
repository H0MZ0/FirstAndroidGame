package com.example.neonpong

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class MenuActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create main layout with ScrollView for smaller screens
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(40, 60, 40, 40)
        }

        // Logo/Icon placeholder (uses launcher icon)
        val logoView = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            val size = 180
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, 20, 0, 30)
            }
        }
        
        // Title
        val title = TextView(this).apply {
            text = "NEON PONG"
            textSize = 48f
            setTextColor(Color.GREEN)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(20f, 0f, 0f, Color.GREEN)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 15)
        }
        
        // Subtitle
        val subtitle = TextView(this).apply {
            text = "Classic Arcade Action"
            textSize = 16f
            setTextColor(Color.parseColor("#00FFFF"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        // Buttons
        val btnVsAI = createMenuButton("PLAY VS AI")
        val btnLocal = createMenuButton("LOCAL 2 PLAYERS\n(Same Device)")
        val btnWiFi = createMenuButton("PLAY WITH FRIEND\n(WiFi)")

        // Button click listeners
        btnVsAI.setOnClickListener {
            startGame(GameMode.VS_AI)
        }

        btnLocal.setOnClickListener {
            startGame(GameMode.LOCAL_2P)
        }

        btnWiFi.setOnClickListener {
            // Go to WiFi lobby to find friends
            val intent = Intent(this, WiFiLobbyActivity::class.java)
            startActivity(intent)
        }

        // Spacer to push developer info to bottom
        val spacer = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        
        // Developer credit section
        val divider = android.view.View(this).apply {
            setBackgroundColor(Color.parseColor("#333333"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                setMargins(0, 30, 0, 20)
            }
        }
        
        val devLabel = TextView(this).apply {
            text = "DEVELOPED BY"
            textSize = 12f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }
        
        // GitHub button
        val btnGitHub = Button(this).apply {
            text = "⚡ H0MZ0 ⚡\n★ GitHub"
            textSize = 18f
            setTextColor(Color.parseColor("#00FF00"))
            setBackgroundColor(Color.parseColor("#1a1a1a"))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(8f, 0f, 0f, Color.GREEN)
            setPadding(40, 20, 40, 20)
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.gravity = Gravity.CENTER_HORIZONTAL
            params.setMargins(0, 0, 0, 20)
            layoutParams = params
        }
        
        btnGitHub.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/H0MZ0"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open browser", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Add views to layout
        layout.addView(logoView)
        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(btnVsAI)
        layout.addView(btnLocal)
        layout.addView(btnWiFi)
        layout.addView(spacer)
        layout.addView(divider)
        layout.addView(devLabel)
        layout.addView(btnGitHub)

        scrollView.addView(layout)
        setContentView(scrollView)
    }

    private fun createMenuButton(text: String): Button {
        return Button(this).apply {
            this.text = text
            textSize = 20f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1a1a1a"))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(10f, 0f, 0f, Color.CYAN)
            setPadding(50, 35, 50, 35)
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(30, 12, 30, 12)
            layoutParams = params
        }
    }

    private fun startGame(mode: GameMode) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("GAME_MODE", mode.name)
        startActivity(intent)
    }
}

enum class GameMode {
    VS_AI,
    LOCAL_2P,
    WIFI_MULTIPLAYER
}
