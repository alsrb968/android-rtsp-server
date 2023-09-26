package kr.co.makeitall.rtspserver

import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

class TcpPacketServer(private val port: Int) {
    private val serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var clientSocket: Socket? = null

    fun interface OnMessageListener {
        fun onMessage(message: String)
    }

    private var onMessageListener: OnMessageListener? = null

    val ipAddress: String
        get() = "${getIPAddress()}:$port"

    fun start() = serverScope.launch {
        val serverSocket = ServerSocket(port)
        while (isActive) {
            val clientSocket = serverSocket.accept()
            handleClient(clientSocket)
        }
    }

    fun stop() {
        serverScope.cancel()
    }

    private fun Socket.getReader() = BufferedReader(InputStreamReader(getInputStream()))

    private fun Socket.getWriter() = PrintWriter(getOutputStream(), true)

    private fun handleClient(clientSocket: Socket) = serverScope.launch {
        try {
            clientSocket.use { socket ->
                this@TcpPacketServer.clientSocket = socket
                Log.d(TAG, "Client connected: ${socket.inetAddress}")

                val buf = CharArray(1024)
                while (isActive && socket.isConnected) {
                    val len = socket.getReader().read(buf)
                    if (len == -1) {
                        Log.d(TAG, "Client disconnected: ${socket.inetAddress}")
                        break
                    }

                    val recv = String(buf, 0, len)
                    Log.i(TAG, "Received message: $len, $recv")
                    CoroutineScope(Dispatchers.Main).launch {
                        onMessageListener?.onMessage(recv)
                    }

//                    send(recv)
//                    Log.i(TAG, "sending message: $recv")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            clientSocket.close()
            this@TcpPacketServer.clientSocket = null
        }
    }

    fun setOnMessageListener(listener: OnMessageListener) {
        onMessageListener = listener
    }

    fun send(message: String) = serverScope.launch {
        clientSocket?.getWriter()?.println(message)
    }

    private fun getIPAddress(): String {
        val interfaces: List<NetworkInterface> = NetworkInterface.getNetworkInterfaces().toList()
        val vpnInterfaces = interfaces.filter { it.displayName.contains(VPN_INTERFACE) }
        val address: String by lazy { interfaces.findAddress().firstOrNull() ?: DEFAULT_IP }
        return if (vpnInterfaces.isNotEmpty()) {
            val vpnAddresses = vpnInterfaces.findAddress()
            vpnAddresses.firstOrNull() ?: address
        } else {
            address
        }
    }

    private fun List<NetworkInterface>.findAddress(): List<String?> = this.asSequence()
        .map { addresses -> addresses.inetAddresses.asSequence() }
        .flatten()
        .filter { address -> !address.isLoopbackAddress }
        .map { it.hostAddress }
        .filter { address -> address?.contains(":") == false }
        .toList()

    companion object {
        private const val TAG = "TcpPacketServer"

        private const val VPN_INTERFACE = "tun"
        private const val DEFAULT_IP = "0.0.0.0"
    }
}
