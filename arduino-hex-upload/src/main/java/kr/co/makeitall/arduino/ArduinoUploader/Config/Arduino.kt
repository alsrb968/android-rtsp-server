package kr.co.makeitall.arduino.ArduinoUploader.Config

import kr.co.makeitall.arduino.Boards

data class Arduino(
    var model: String,
    var mcu: McuIdentifier,
    var baudRate: Int,
    var protocol: Protocol,
    var preOpenResetBehavior: String? = null,
    var postOpenResetBehavior: String? = null,
    var closeResetBehavior: String? = null,
    var sleepAfterOpen: Int = if (protocol == Protocol.Avr109) 0 else 250,
    var readTimeout: Int = 1000,
    var writeTimeout: Int = 1000
) {
    constructor(board: Boards) : this(board.boardName, board.chipType, board.uploadBaudRate, board.uploadProtocol)
}
