package kr.co.makeitall.arduino

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import androidx.annotation.IntDef
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import com.felhr.usbserial.*
import kotlinx.coroutines.*
import kr.co.makeitall.arduino.ArduinoUploader.ArduinoSketchUploader
import kr.co.makeitall.arduino.ArduinoUploader.Config.Arduino
import kr.co.makeitall.arduino.ArduinoUploader.IArduinoUploaderLogger
import timber.log.Timber
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader

class UsbSerialManager(private val context: Context, lifecycle: Lifecycle) : LifecycleObserver {

    companion object {
        const val BAUD_RATE = 9600 // BaudRate. Change this value if you need

        const val ACTION_USB_PERMISSION_REQUEST = "kr.co.makeitall.arduino.action.USB_PERMISSION_REQUEST"

        const val USB_PERMISSION_GRANTED = 0
        const val USB_PERMISSION_DENIED = 1

        const val USB_STATE_READY = 2
        const val USB_STATE_CONNECTED = 3
        const val USB_STATE_DISCONNECTED = 4
        const val USB_STATE_READ_START = 5
        const val USB_STATE_READ_END = 6

        const val USB_ERROR_NOT_SUPPORTED = -1
        const val USB_ERROR_NO_USB = -2
        const val USB_ERROR_USB_DEVICE_NOT_WORKING = -3
        const val USB_ERROR_CDC_DRIVER_NOT_WORKING = -4
    }

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        USB_PERMISSION_GRANTED,
        USB_PERMISSION_DENIED
    )
    annotation class UsbPermission

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        USB_STATE_READY,
        USB_STATE_CONNECTED,
        USB_STATE_DISCONNECTED,
        USB_STATE_READ_START,
        USB_STATE_READ_END
    )
    annotation class UsbState

    private interface OnErrorListener {
        fun onError(error: UsbSerialException)
    }

    private fun OnErrorListener(block: (UsbSerialException) -> Unit): OnErrorListener =
        object : OnErrorListener {
            override fun onError(error: UsbSerialException) {
                CoroutineScope(Dispatchers.Main).launch { block(error) }
            }
        }

    private interface OnPermissionListener {
        fun onPermission(@UsbPermission permission: Int)
    }

    private fun OnPermissionListener(block: (Int) -> Unit): OnPermissionListener =
        object : OnPermissionListener {
            override fun onPermission(@UsbPermission permission: Int) {
                CoroutineScope(Dispatchers.Main).launch { block(permission) }
            }
        }

    private interface OnStateListener {
        fun onState(@UsbState state: Int)
    }

    private fun OnStateListener(block: (Int) -> Unit): OnStateListener =
        object : OnStateListener {
            override fun onState(@UsbState state: Int) {
                CoroutineScope(Dispatchers.Main).launch { block(state) }
            }
        }

    private interface OnUsbReadListener {
        fun onRead(data: ByteArray)
    }

    private fun OnUsbReadListener(block: (ByteArray) -> Unit): OnUsbReadListener =
        object : OnUsbReadListener {
            override fun onRead(data: ByteArray) {
                CoroutineScope(Dispatchers.Main).launch { block(data) }
            }
        }

    init {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                ON_CREATE -> {
                    Timber.i("onCreated")
                    registerUsbHardwareReceiver()
                    updateDevice()
                }

                ON_RESUME -> Timber.i("onResume")

                ON_PAUSE -> Timber.i("onPause")

                ON_DESTROY -> {
                    Timber.i("onDestroy")
                    removeOnStateListener()
                    removeOnPermissionListener()
                    removeOnErrorListener()
                    removeOnUsbReadListener()
                    removeOnUsbCtsListener()
                    removeOnUsbDsrListener()
                    unregisterUsbHardwareReceiver()
                    stopRead()
                }

                else -> Timber.i("else")
            }
        })
    }

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var onErrorListener: OnErrorListener? = null
    private var onPermissionListener: OnPermissionListener? = null
    private var onStateListener: OnStateListener? = null

    var usbDevice: UsbDevice? = null
    var usbSerialDevice: UsbSerialDevice? = null
    private var connection: UsbDeviceConnection? = null

    private var usbSerialConfig = UsbSerialConfig()

    private var readJob: Job? = null

    private var serialInputStream: SerialInputStream? = null
    private var serialOutputStream: SerialOutputStream? = null

    private var onUsbReadListener: OnUsbReadListener? = null

    /**
     * State changes in the CTS line will be received here
     */
    private var usbCtsCallback: UsbSerialInterface.UsbCTSCallback? = null

    /**
     * State changes in the DSR line will be received here
     */
    private var usbDsrCallback: UsbSerialInterface.UsbDSRCallback? = null

    private val usbHardwareReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION_REQUEST -> {
                    val isGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    Timber.i("isGranted: $isGranted")
                    intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)?.let { device ->
                        if (isGranted) {
                            connection = usbManager.openDevice(device)
                            Timber.i("ready")
                            // Everything went as expected. Send an intent to MainActivity
                            onStateListener?.onState(USB_STATE_READY)
                        }
                        onPermissionListener?.onPermission(
                            if (isGranted) USB_PERMISSION_GRANTED
                            else USB_PERMISSION_DENIED
                        )
                    }
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)?.let { device ->
                        if (!isReading()) {
                            updateDevice()
                        }
                        onStateListener?.onState(USB_STATE_CONNECTED)
                    }
                    Timber.i("usb attached")
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)?.let { device ->
                        stopRead()
                        onStateListener?.onState(USB_STATE_DISCONNECTED)
                        updateDevice()
                    }
                    Timber.i("usb detached")
                }

                UsbManager.ACTION_USB_ACCESSORY_ATTACHED -> {
                    intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)?.let { accessory ->

                    }
                    Timber.i("accessory attached")
                }

                UsbManager.ACTION_USB_ACCESSORY_DETACHED -> {
                    intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)?.let { accessory ->

                    }
                    Timber.i("accessory detached")
                }
            }
        }
    }

    private fun registerUsbHardwareReceiver() {
        Timber.i("registerUsbHardwareReceiver")
        context.registerReceiver(usbHardwareReceiver,
            IntentFilter().apply {
                addAction(ACTION_USB_PERMISSION_REQUEST)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
                addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
            }
        )
    }

    private fun unregisterUsbHardwareReceiver() {
        context.unregisterReceiver(usbHardwareReceiver)
    }

    fun addOnErrorListener(listener: (UsbSerialException) -> Unit): UsbSerialManager {
        onErrorListener = OnErrorListener { listener(it) }
        return this@UsbSerialManager
    }

    fun removeOnErrorListener() {
        onErrorListener = null
    }

    fun addOnPermissionListener(listener: (Int) -> Unit): UsbSerialManager {
        onPermissionListener = OnPermissionListener { listener(it) }
        return this@UsbSerialManager
    }

    fun removeOnPermissionListener() {
        onPermissionListener = null
    }

    fun addOnStateListener(listener: (Int) -> Unit): UsbSerialManager {
        onStateListener = OnStateListener { listener(it) }
        return this@UsbSerialManager
    }

    fun removeOnStateListener() {
        onStateListener = null
    }

    fun addOnUsbReadListener(listener: (ByteArray) -> Unit): UsbSerialManager {
        onUsbReadListener = OnUsbReadListener { listener(it) }
        return this@UsbSerialManager
    }

    fun removeOnUsbReadListener() {
        onUsbReadListener = null
    }

    fun addOnUsbCtsListener(listener: (Boolean) -> Unit): UsbSerialManager {
        usbCtsCallback = UsbSerialInterface.UsbCTSCallback { listener(it) }
        return this@UsbSerialManager
    }

    fun removeOnUsbCtsListener() {
        usbCtsCallback = null
    }

    fun addOnUsbDsrListener(listener: (Boolean) -> Unit): UsbSerialManager {
        usbDsrCallback = UsbSerialInterface.UsbDSRCallback { listener(it) }
        return this@UsbSerialManager
    }

    fun removeOnUsbDsrListener() {
        usbDsrCallback = null
    }

    fun hasPermission(): Boolean {
        return usbDevice?.let { usbManager.hasPermission(it) } ?: false
    }

    fun requestPermission() {
        // java.lang.IllegalArgumentException: com.example.myapp: Targeting S+ (version 31 and above) requires
        // that one of FLAG_IMMUTABLE or FLAG_MUTABLE be specified when creating a PendingIntent.
        usbManager.requestPermission(
            usbDevice,
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION_REQUEST),
                PendingIntent.FLAG_MUTABLE
            )
        )
    }

    fun writeBytes(data: ByteArray) {
        Timber.w("data: ${data.map { "%02X".format(it) }}")
        serialOutputStream?.write(data)
    }

    fun writeString(data: String) {
        Timber.w("data: $data")
        serialOutputStream?.write(data.toByteArray())
    }

    fun updateConfig(config: UsbSerialConfig) {
        usbSerialConfig = config
        Timber.i("usbSerialConfig: $usbSerialConfig")
    }

    fun updateBaudRate(baudRate: Int) {
        usbSerialConfig.baudRate = baudRate
        usbSerialDevice?.setBaudRate(baudRate)
        Timber.i("usbSerialConfig: $usbSerialConfig")
    }

    fun isUsbSerialDevice(): Boolean =
        usbDevice?.let {
            // no usable interfaces
            if (it.interfaceCount <= 0) {
                return false
            }
            val vendorId = it.vendorId
            val productId = it.productId
            // There is a device connected to our Android device. Try to open it as a Serial Port.
            vendorId != 0x1d6b && productId != 0x0001 && productId != 0x0002 && productId != 0x0003
        } ?: false

    private fun updateDevice() {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        val usbDevices = usbManager.deviceList
        if (usbDevices.isNotEmpty()) {
            for ((_, device) in usbDevices) {
                Timber.d(
                    String.format(
                        "UsbDevice{vendorId: %X, productId: %X, isSupported: %b, deviceClass: %X, deviceSubclass: %X, deviceName: %s}",
                        device.vendorId, device.productId, UsbSerialDevice.isSupported(device),
                        device.deviceClass, device.deviceSubclass, device.deviceName
                    )
                )

                usbDevice = device

                if (isUsbSerialDevice()) {
//                if (UsbSerialDevice.isSupported(device)) {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    requestPermission()
                    break
                } else {
                    connection = null
                    usbDevice = null
                }
            }
            if (usbDevice == null) {
                onErrorListener?.onError(UsbSerialException(USB_ERROR_NO_USB))
            }
        } else {
            Timber.e("usbManager returned empty device list.")
            connection = null
            usbDevice = null
            // There is no USB devices connected. Send an intent to MainActivity
            onErrorListener?.onError(UsbSerialException(USB_ERROR_NO_USB))
        }
    }

    /**
     * A simple thread to open a serial port.
     * Although it should be a fast operation. moving usb operations away from UI thread is a good thing.
     */
    fun startRead(lateStartTimeMillis: Long = 0) {
        CoroutineScope(Dispatchers.Main).launch {
            usbSerialDevice = UsbSerialDevice.createUsbSerialDevice(usbDevice, connection)
            Timber.i("usbSerialDevice: $usbSerialDevice")
            usbSerialDevice?.let { serial ->
                if (serial.syncOpen()) {
                    Timber.i("syncOpen() success readJob: $readJob")
                    serial.apply {
                        setBaudRate(usbSerialConfig.baudRate)
                        setDataBits(usbSerialConfig.dataBits)
                        setStopBits(usbSerialConfig.stopBits)
                        setParity(usbSerialConfig.parity)

                        // Current flow control Options:
                        // UsbSerialInterface.FLOW_CONTROL_OFF
                        // UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
                        // UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
                        setFlowControl(usbSerialConfig.flowControl)

                        // InputStream and OutputStream will be null if you are using async api.
                        serialInputStream = serial.inputStream
                        serialOutputStream = serial.outputStream
                    }

                    serialInputStream?.readString(lateStartTimeMillis) {
                        onUsbReadListener?.onRead(it)
                    }

                } else {
                    // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                    // Send an Intent to Main Activity
                    onErrorListener?.onError(
                        UsbSerialException(
                            if (serial is CDCSerialDevice)
                                USB_ERROR_CDC_DRIVER_NOT_WORKING
                            else
                                USB_ERROR_USB_DEVICE_NOT_WORKING
                        )
                    )
                }
            } ?: run {
                // No driver for given device, even generic CDC driver could not be loaded
                onErrorListener?.onError(UsbSerialException(USB_ERROR_NOT_SUPPORTED))
            }
        }
    }

    private suspend fun SerialInputStream.readString(lateStartTimeMillis: Long, callback: (ByteArray) -> Unit) {
        readJob?.let {
            if (it.isActive) {
                Timber.w("readJob is active, cancel it.")
                it.cancelAndJoin()
                Timber.d("readJob is canceled.")
            }
        }
        val buffer = ByteArray(1024)
        setTimeout(1)
        readJob = CoroutineScope(Dispatchers.IO).launch {
            // Some Arduinos would need some sleep because firmware wait some time to know whether a new sketch is going
            // to be uploaded or not
            if (lateStartTimeMillis > 0) {
                delay(lateStartTimeMillis) // sleep some. YMMV with different chips.
            }

            Timber.i("readJob start")
            onStateListener?.onState(USB_STATE_READ_START)

            while (true) {
                val length = read(buffer)
                if (length > 0) {
                    val data = buffer.copyOfRange(0, length)
                    Timber.d("length: $length, data: ${data.map { "%02X".format(it) }}")
                    callback(data)
                }
                delay(1)
            }
        }
    }

    fun stopRead() {
        readJob?.let {
            it.cancel()
            Timber.d("readJob is canceled.")
        }

        serialInputStream?.close()
        serialOutputStream?.close()
        usbSerialDevice?.syncClose()

        usbSerialDevice = null
        serialInputStream = null
        serialOutputStream = null

        Timber.i("end")
        onStateListener?.onState(USB_STATE_READ_END)
    }

    fun isReading(): Boolean = readJob?.isActive ?: false

    suspend fun uploadHex(
        hexCode: String? = null,
        firmware: InputStream? = null,
        board: Boards,
        percentCallback: (percent: Int) -> Unit,
        messageCallback: (message: String) -> Unit,
        completeCallback: (complete: Boolean) -> Unit
    ) {
        val arduinoBoard = Arduino(board)

        try {
            val hexFileContents = LineReader(
                hexCode?.let { StringReader(it) } ?: firmware?.let { InputStreamReader(it) }
                ?: throw Exception("code and firmware is null")
            ).readLines()

            val uploader = ArduinoSketchUploader(
                context,
                SerialPortStreamImpl::class.java,
                null,
                object : IArduinoUploaderLogger {
                    override fun onError(message: String, exception: Exception) {
                        messageCallback(message)
                    }

                    override fun onWarn(message: String) {
                        messageCallback(message)
                    }

                    override fun onInfo(message: String) {
                        messageCallback(message)
                    }

                    override fun onDebug(message: String) {
                        messageCallback(message)
                    }

                    override fun onTrace(message: String) {}
                }
            ) {
                percentCallback((it * 100).toInt())
            }

            withContext(Dispatchers.Default) {
                stopRead()

                Timber.i("start upload")
                usbDevice?.deviceName?.let { uploader.uploadSketch(hexFileContents, arduinoBoard, it) }

                startRead(2000)
            }

            percentCallback(100)
            completeCallback(true)
        } catch (e: Exception) {
            e.printStackTrace()
            completeCallback(false)
        }
    }
}
