package com.example.neonpong

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class DeviceDiscoveryManager {
    
    private var discoverySocket: DatagramSocket? = null
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    
    companion object {
        const val DISCOVERY_PORT = 8888
        const val GAME_PORT = 8889
        const val BROADCAST_MESSAGE = "NEON_PONG_DISCOVER"
        const val RESPONSE_MESSAGE = "NEON_PONG_HERE"
    }
    
    fun startHosting(myIp: String, onClientConnected: (String) -> Unit) {
        isRunning = true
        
        // Start broadcast listener
        Thread {
            try {
                discoverySocket = DatagramSocket(DISCOVERY_PORT)
                val buffer = ByteArray(1024)
                
                while (isRunning) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    discoverySocket?.receive(packet)
                    
                    val message = String(packet.data, 0, packet.length)
                    if (message == BROADCAST_MESSAGE) {
                        // Respond to discovery
                        val response = RESPONSE_MESSAGE.toByteArray()
                        val responsePacket = DatagramPacket(
                            response,
                            response.size,
                            packet.address,
                            packet.port
                        )
                        discoverySocket?.send(responsePacket)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
        
        // Start game server
        Thread {
            try {
                serverSocket = ServerSocket(GAME_PORT)
                val clientSocket = serverSocket?.accept()
                val clientIp = clientSocket?.inetAddress?.hostAddress ?: ""
                onClientConnected(clientIp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
    
    fun findDevices(myIp: String, onDevicesFound: (List<String>) -> Unit) {
        Thread {
            val devices = mutableListOf<String>()
            
            try {
                val socket = DatagramSocket()
                socket.broadcast = true
                socket.soTimeout = 3000 // 3 second timeout
                
                // Get broadcast address
                val parts = myIp.split(".")
                if (parts.size == 4) {
                    val broadcastIp = "${parts[0]}.${parts[1]}.${parts[2]}.255"
                    
                    // Send broadcast
                    val message = BROADCAST_MESSAGE.toByteArray()
                    val packet = DatagramPacket(
                        message,
                        message.size,
                        InetAddress.getByName(broadcastIp),
                        DISCOVERY_PORT
                    )
                    socket.send(packet)
                    
                    // Listen for responses
                    val startTime = System.currentTimeMillis()
                    while (System.currentTimeMillis() - startTime < 3000) {
                        try {
                            val buffer = ByteArray(1024)
                            val responsePacket = DatagramPacket(buffer, buffer.size)
                            socket.receive(responsePacket)
                            
                            val response = String(responsePacket.data, 0, responsePacket.length)
                            if (response == RESPONSE_MESSAGE) {
                                val deviceIp = responsePacket.address.hostAddress
                                if (deviceIp != null && deviceIp != myIp && !devices.contains(deviceIp)) {
                                    devices.add(deviceIp)
                                }
                            }
                        } catch (e: Exception) {
                            // Timeout or error, continue
                        }
                    }
                }
                
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            onDevicesFound(devices)
        }.start()
    }
    
    fun stopDiscovery() {
        isRunning = false
        try {
            discoverySocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
