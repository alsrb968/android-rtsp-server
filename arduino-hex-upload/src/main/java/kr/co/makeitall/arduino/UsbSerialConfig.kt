package kr.co.makeitall.arduino

import com.felhr.usbserial.UsbSerialInterface

data class UsbSerialConfig(
    var baudRate: Int = UsbSerialManager.BAUD_RATE,
    var dataBits: Int = UsbSerialInterface.DATA_BITS_8,
    var stopBits: Int = UsbSerialInterface.STOP_BITS_1,
    var parity: Int = UsbSerialInterface.PARITY_NONE,
    var flowControl: Int = UsbSerialInterface.FLOW_CONTROL_OFF
)
