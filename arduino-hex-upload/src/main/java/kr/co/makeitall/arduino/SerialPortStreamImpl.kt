package kr.co.makeitall.arduino

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.felhr.usbserial.UsbSerialInterface.UsbReadCallback
import kr.co.makeitall.arduino.ArduinoUploader.Help.ISerialPortStream
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class SerialPortStreamImpl(
    private var _context: Context,
    private var _portName: String, //PortKey
    private var _baudRate: Int
) : ISerialPortStream {

    companion object {
        private const val DEFAULT_READ_BUFFER_SIZE = 16 * 1024
    }

    private var serialPort: UsbSerialDevice
    private var readTimeout = 0
    private var writeTimeout = 0
    private val usbManager: UsbManager = _context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val connection: UsbDeviceConnection
    private var usbDevices: HashMap<String, UsbDevice> = usbManager.deviceList
    private var data = ArrayBlockingQueue<Int>(DEFAULT_READ_BUFFER_SIZE)

    @Volatile
    private var isOpen = false
    private val mReadCallback = UsbReadCallback { new_data ->
        for (b in new_data) {
            try {
                data.put(b.toInt() and 0xff) //Khong put truc tiep byte vi co gia tri -1 neu ko nhan duoc
            } catch (e: InterruptedException) {
                e.printStackTrace()
                // ignore, possibly losing bytes when buffer is full
            }
        }
    }

    init {
        val usbDevice = usbDevices[_portName]
        connection = usbManager.openDevice(usbDevice)
        serialPort = UsbSerialDevice.createUsbSerialDevice(usbDevice, connection).apply {
            setDataBits(UsbSerialInterface.DATA_BITS_8)
            setStopBits(UsbSerialInterface.STOP_BITS_1)
            setParity(UsbSerialInterface.PARITY_NONE)
        }
    }

    override fun getPortName() = _portName

    override fun getPortNames(): Array<String> {
        val portNames: MutableList<String> = ArrayList()

        usbDevices.forEach { (deviceKey, usbDevice) ->
            val deviceVID = usbDevice.vendorId
            val devicePID = usbDevice.productId
            val deviceName = usbDevice.deviceName
            if (deviceVID != 0x1d6b && devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003) {
                // There is a device connected to our Android device. Try to open it as a Serial Port.
                portNames.add(deviceKey)
                Log.d("deviceKey:$deviceKey -> $usbDevice")
            }
        }

        return portNames.toTypedArray()
    }

    override fun getContext() = _context

    override fun setContext(context: Context) {
        _context = context
    }

    override fun setBaudRate(newBaudRate: Int) = synchronized(this) { serialPort.setBaudRate(newBaudRate) }

    override fun setReadTimeout(miliseconds: Int) = synchronized(this) { readTimeout = miliseconds }

    override fun setWriteTimeout(miliseconds: Int) = synchronized(this) { writeTimeout = miliseconds }

    override fun open() =
        synchronized(this) {
            serialPort.open()
            serialPort.setBaudRate(_baudRate)
            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
            data.clear()
            serialPort.read(mReadCallback)
            isOpen = true
        }

    override fun close() =
        synchronized(this) {
            serialPort.close()
            isOpen = false
        }

    override fun setDtrEnable(enable: Boolean) = synchronized(this) { serialPort.setDTR(enable) }

    override fun setRtsEnable(enable: Boolean) = synchronized(this) { serialPort.setRTS(enable) }

    override fun setNumDataBits(newDataBits: Int) = synchronized(this) { serialPort.setDataBits(newDataBits) }

    override fun setNumStopBits(newStopBits: Int) = synchronized(this) { serialPort.setStopBits(newStopBits) }

    override fun setParity(newParity: Int) = synchronized(this) { serialPort.setParity(newParity) }

    override fun readBytes(buffer: ByteArray, bytesToRead: Int): Int {
        synchronized(this) {
            var index = 0
            var count = 0
            while (isOpen && index < bytesToRead) {
                try {
                    val readByte = data.poll(readTimeout.toLong(), TimeUnit.MILLISECONDS)
                    if (readByte != -1) {
                        buffer[index] = readByte.toByte()
                        count++
                        index++
                    } else return -1
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                    return -1
                }
            }
            return count
        }
    }

    override fun readBytes(buffer: ByteArray, bytesToRead: Int, offset: Int): Int {
        synchronized(this) {
            var index = offset
            var numRead = 0
            while (isOpen && index < bytesToRead) {
                try {
                    val readByte = data.poll(readTimeout.toLong(), TimeUnit.MILLISECONDS)
                    if (readByte != -1) {
                        buffer[index] = readByte.toByte()
                        index++
                        numRead++
                    } else return -1
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                    return -1
                }
            }
            return numRead
        }
    }

    override fun writeBytes(buffer: ByteArray, bytesToWrite: Int) =
        synchronized(this) {
            val writeBuffer = ByteArray(bytesToWrite)
            System.arraycopy(buffer, 0, writeBuffer, 0, bytesToWrite)
            serialPort.write(writeBuffer)
            bytesToWrite
        }

    override fun writeBytes(buffer: ByteArray, bytesToWrite: Int, offset: Int) =
        synchronized(this) {
            val writeBuffer = ByteArray(bytesToWrite)
            System.arraycopy(buffer, offset, writeBuffer, 0, bytesToWrite)
            serialPort.write(writeBuffer)
            bytesToWrite
        }

    override fun DiscardInBuffer() = data.clear()
}
