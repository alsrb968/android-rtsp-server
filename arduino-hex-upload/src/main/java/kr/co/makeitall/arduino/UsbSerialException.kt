package kr.co.makeitall.arduino

import kr.co.makeitall.arduino.UsbSerialManager.Companion.USB_ERROR_CDC_DRIVER_NOT_WORKING
import kr.co.makeitall.arduino.UsbSerialManager.Companion.USB_ERROR_NOT_SUPPORTED
import kr.co.makeitall.arduino.UsbSerialManager.Companion.USB_ERROR_NO_USB
import kr.co.makeitall.arduino.UsbSerialManager.Companion.USB_ERROR_USB_DEVICE_NOT_WORKING

class UsbSerialException(private val type: Int) : RuntimeException() {

    override fun toString(): String =
        when (type) {
            USB_ERROR_NOT_SUPPORTED -> "UsbNotSupportException"
            USB_ERROR_NO_USB -> "NoUsbException"
            USB_ERROR_USB_DEVICE_NOT_WORKING -> "UsbDeviceNotWorkingException"
            USB_ERROR_CDC_DRIVER_NOT_WORKING -> "CdcDriverNotWorkingException"
            else -> "UnknownException"
        }
}
