package com.example.neonpong

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import java.net.InetAddress
import java.net.NetworkInterface

class WiFiLobbyActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var devicesList: LinearLayout
    private lateinit var btnHost: Button
    private lateinit var btnRefresh: Button
    private val discoveryManager = DeviceDiscoveryManager()
    private val handler = Handler(Looper.getMainLooper())
    private var isHosting = false
    private var myIpAddress = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(40, 60, 40, 60)
        }

        // Title
        val title = TextView(this).apply {
            text = "FIND FRIENDS"
            textSize = 36f
            setTextColor(Color.GREEN)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(15f, 0f, 0f, Color.GREEN)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 25)
        }

        // Status text
        statusText = TextView(this).apply {
            text = "Getting your IP address..."
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(20, 10, 20, 30)
        }

        // Host button
        btnHost = Button(this).apply {
            text = "HOST A GAME"
            textSize = 20f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#00aa00"))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(30, 20, 30, 20)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 10, 0, 20)
            layoutParams = params
        }

        btnHost.setOnClickListener {
            if (!isHosting) {
                startHosting()
            }
        }

        // Refresh button
        btnRefresh = Button(this).apply {
            text = "REFRESH"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1a1a1a"))
            setPadding(30, 15, 30, 15)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 20)
            layoutParams = params
        }

        btnRefresh.setOnClickListener {
            searchForDevices()
        }

        // Devices label
        val devicesLabel = TextView(this).apply {
            text = "AVAILABLE PLAYERS:"
            textSize = 20f
            setTextColor(Color.CYAN)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, 0, 0, 15)
        }

        // Scroll view for devices
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        devicesList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scrollView.addView(devicesList)

        // Back button
        val btnBack = Button(this).apply {
            text = "BACK"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#aa0000"))
            setPadding(30, 15, 30, 15)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 20, 0, 0)
            layoutParams = params
        }

        btnBack.setOnClickListener {
            discoveryManager.stopDiscovery()
            finish()
        }

        mainLayout.addView(title)
        mainLayout.addView(statusText)
        mainLayout.addView(btnHost)
        mainLayout.addView(btnRefresh)
        mainLayout.addView(devicesLabel)
        mainLayout.addView(scrollView)
        mainLayout.addView(btnBack)

        setContentView(mainLayout)

        // Get IP and start discovery
        initializeNetwork()
    }

    private fun initializeNetwork() {
        Thread {
            myIpAddress = getLocalIpAddress()
            handler.post {
                statusText.text = "Your IP: $myIpAddress\nSearching for players..."
                searchForDevices()
            }
        }.start()
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress ?: ""
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "Unknown"
    }

    private fun startHosting() {
        isHosting = true
        btnHost.text = "WAITING FOR PLAYER..."
        btnHost.isEnabled = false
        
        // Start hosting and immediately launch game for host
        Thread {
            discoveryManager.startHosting(myIpAddress) { clientIp ->
                handler.post {
                    // Client joins - they will connect to host
                    // No need to do anything here, client handles connection
                }
            }
            
            // Host immediately starts game
            handler.post {
                startMultiplayerGame(true, "")
            }
        }.start()
    }

    private fun searchForDevices() {
        devicesList.removeAllViews()
        
        val searchingText = TextView(this).apply {
            text = "Searching..."
            textSize = 16f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }
        devicesList.addView(searchingText)

        discoveryManager.findDevices(myIpAddress) { devices ->
            handler.post {
                displayDevices(devices)
            }
        }
    }

    private fun displayDevices(devices: List<String>) {
        devicesList.removeAllViews()

        if (devices.isEmpty()) {
            val noDevices = TextView(this).apply {
                text = "No players found.\nMake sure your friend pressed 'HOST A GAME'"
                textSize = 16f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
                setPadding(0, 30, 0, 30)
            }
            devicesList.addView(noDevices)
            return
        }

        devices.forEach { ip ->
            val deviceButton = Button(this).apply {
                text = "Player at $ip\nTAP TO JOIN"
                textSize = 16f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#1a1a1a"))
                setShadowLayer(10f, 0f, 0f, Color.MAGENTA)
                setPadding(30, 25, 30, 25)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 10, 0, 10)
                layoutParams = params
            }

            deviceButton.setOnClickListener {
                connectToDevice(ip)
            }

            devicesList.addView(deviceButton)
        }
    }

    private fun connectToDevice(ip: String) {
        statusText.text = "Connecting to $ip..."
        startMultiplayerGame(false, ip)
    }

    private fun startMultiplayerGame(isHost: Boolean, opponentIp: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("GAME_MODE", GameMode.WIFI_MULTIPLAYER.name)
        intent.putExtra("IS_HOST", isHost)
        intent.putExtra("OPPONENT_IP", opponentIp)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        discoveryManager.stopDiscovery()
    }
}
