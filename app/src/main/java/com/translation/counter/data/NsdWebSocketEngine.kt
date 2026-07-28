package com.translation.counter.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

class NsdWebSocketEngine(private val context: Context) {

    private val TAG = "NsdWebSocketEngine"
    private val SERVICE_TYPE = "_counter_trans._tcp."
    private val SERVICE_NAME = "CounterTranslator"
    private val PORT = 8887

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var serverSocket: ServerSocket? = null
    private val clientSockets = CopyOnWriteArrayList<Socket>()
    private var clientSocket: Socket? = null

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    // Event Callbacks for MainViewModel
    var onSessionStarted: ((langCode: String) -> Unit)? = null
    var onChatMessageReceived: ((ChatMessage) -> Unit)? = null
    var onRemoteTouristMicTriggered: (() -> Unit)? = null
    var onSessionEnded: (() -> Unit)? = null

    // 1. Staff Mode: Register NSD Service & Start Local Server
    fun startStaffServer() {
        scope.launch {
            try {
                serverSocket?.close()
                serverSocket = ServerSocket(PORT)
                Log.d(TAG, "Staff Server started on port $PORT")

                registerNsdService()

                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    clientSockets.add(socket)
                    Log.d(TAG, "Client connected: ${socket.inetAddress.hostAddress}")
                    handleClientConnection(socket)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
            }
        }
    }

    private fun registerNsdService() {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            port = PORT
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                Log.d(TAG, "NSD Service Registered: ${NsdServiceInfo.serviceName}")
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "NSD Registration Failed: $errorCode")
            }
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "NSD Register exception", e)
        }
    }

    // 2. Tourist Mode: Discover NSD Service & Connect as Client
    fun startTouristClient() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "NSD Service Discovery Started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "NSD Service Found: ${service.serviceName}")
                if (service.serviceType.contains("_counter_trans")) {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e(TAG, "NSD Resolve Failed: $errorCode")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val hostAddress = serviceInfo.host?.hostAddress ?: return
                            Log.d(TAG, "NSD Service Resolved: $hostAddress:${serviceInfo.port}")
                            connectToServer(hostAddress, serviceInfo.port)
                        }
                    })
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "NSD Discover exception", e)
        }
    }

    private fun connectToServer(hostIp: String, port: Int) {
        scope.launch {
            try {
                clientSocket?.close()
                val socket = Socket(hostIp, port)
                clientSocket = socket
                Log.d(TAG, "Connected to Staff Server at $hostIp:$port")

                handleClientConnection(socket)
            } catch (e: Exception) {
                Log.e(TAG, "Client Connection Error", e)
            }
        }
    }

    private fun handleClientConnection(socket: Socket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val messageText = line ?: continue
                    parseAndProcessIncomingMessage(messageText)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Socket Disconnected: ${e.message}")
            } finally {
                clientSockets.remove(socket)
            }
        }
    }

    private fun parseAndProcessIncomingMessage(jsonStr: String) {
        try {
            val obj = JsonParser.parseString(jsonStr).asJsonObject
            val type = obj.get("type")?.asString ?: return

            when (type) {
                "START_SESSION" -> {
                    val lang = obj.get("lang")?.asString ?: "en-US"
                    onSessionStarted?.invoke(lang)
                }
                "CHAT_MESSAGE" -> {
                    val msgJson = obj.getAsJsonObject("data")
                    val msg = gson.fromJson(msgJson, ChatMessage::class.java)
                    onChatMessageReceived?.invoke(msg)
                }
                "TRIGGER_TOURIST_MIC" -> {
                    // Remote trigger received on tourist phone
                    onRemoteTouristMicTriggered?.invoke()
                }
                "END_SESSION" -> {
                    onSessionEnded?.invoke()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing incoming socket message", e)
        }
    }

    fun broadcastMessage(type: String, data: Any? = null) {
        scope.launch {
            val root = JsonObject()
            root.addProperty("type", type)
            if (data != null) {
                root.add("data", gson.toJsonTree(data))
            }
            val payload = root.toString() + "\n"

            // Send to all connected sockets
            clientSockets.forEach { socket ->
                try {
                    val writer = OutputStreamWriter(socket.getOutputStream(), "UTF-8")
                    writer.write(payload)
                    writer.flush()
                } catch (e: Exception) {
                    Log.w(TAG, "Error sending to client socket", e)
                }
            }

            // Send via client socket if connected
            clientSocket?.let { socket ->
                try {
                    val writer = OutputStreamWriter(socket.getOutputStream(), "UTF-8")
                    writer.write(payload)
                    writer.flush()
                } catch (e: Exception) {
                    Log.w(TAG, "Error sending from client socket", e)
                }
            }
        }
    }

    fun stop() {
        try {
            registrationListener?.let { nsdManager.unregisterService(it) }
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering NSD", e)
        }
        clientSockets.forEach { try { it.close() } catch (e: Exception) {} }
        clientSockets.clear()
        try { clientSocket?.close() } catch (e: Exception) {}
        try { serverSocket?.close() } catch (e: Exception) {}
    }
}
