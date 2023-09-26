package kr.co.makeitall.rtspserver

interface ClientListener {
    fun onDisconnected(client: ServerClient)
}
