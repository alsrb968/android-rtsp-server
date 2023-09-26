package kr.co.makeitall.rtspserver

import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import kr.co.makeitall.rtspserver.databinding.ActivityDatagramBinding
import java.io.IOException
import java.net.*
import kotlin.math.abs
import kotlin.math.ceil

class DatagramActivity : AppCompatActivity(), SurfaceHolder.Callback {
    private lateinit var binding: ActivityDatagramBinding
    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader
    private lateinit var backgroundHandler: Handler
    private lateinit var backgroundThread: HandlerThread
    private lateinit var udpSocket: DatagramSocket
    private lateinit var destinationAddress: InetAddress

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this@DatagramActivity, R.layout.activity_datagram)

        surfaceView = binding.surfaceView
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val ipAddress = getIPAddress()
        Log.e(TAG, "ipAddress: $ipAddress")
        binding.textView.text = "$ipAddress:$UDP_PORT"

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        try {
            destinationAddress = InetAddress.getByName(ipAddress) // Replace with your server IP address
            udpSocket = DatagramSocket()
        } catch (e: SocketException) {
            e.printStackTrace()
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()

    }

    private fun openCamera() {
        try {
            val cameraId = cameraManager.cameraIdList[0]
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)

            val streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val previewSizes = streamConfigurationMap?.getOutputSizes(SurfaceHolder::class.java)

            // Find the closest supported size to 640x480
            val targetSize = getClosestSize(previewSizes, 640, 480)

            imageReader = ImageReader.newInstance(targetSize.width, targetSize.height, ImageFormat.JPEG, 2)
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                image?.let {
                    val planes = image.planes
                    val buffer = planes[0].buffer

                    // Convert the YUV image data to a byte array
                    val data = ByteArray(buffer.remaining())
                    buffer.get(data)

                    // Send the frame via UDP
//                    val packet = DatagramPacket(data, data.size, destinationAddress, UDP_PORT)
//                    udpSocket.send(packet)
                    sendFrameViaUDP(data, destinationAddress, UDP_PORT)

                    image.close()
                }
            }, backgroundHandler)

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession(targetSize)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    cameraDevice.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    cameraDevice.close()
                }
            }, null)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Error accessing camera", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission not granted", e)
        } catch (e: IOException) {
            Log.e(TAG, "Error creating image reader", e)
        }
    }

    private fun createCaptureSession(targetSize: Size) {
        val surfaces = arrayListOf(surfaceHolder.surface, imageReader.surface)
        cameraDevice.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                cameraCaptureSession = session
                startPreview(targetSize)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e(TAG, "Failed to configure capture session")
            }
        }, backgroundHandler)
    }

    private fun startPreview(targetSize: Size) {
        val previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewRequestBuilder.addTarget(surfaceHolder.surface)
        previewRequestBuilder.addTarget(imageReader.surface)

        // Set the desired target resolution
//        previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, getZoomRect(targetSize))

        cameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler)
    }

    private fun closeCamera() {
        cameraCaptureSession.close()
        cameraDevice.close()
        imageReader.close()
        udpSocket.close()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Start the background thread
        backgroundThread = HandlerThread("CameraBackgroundThread")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)

        openCamera()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Stop the background thread
        backgroundThread.quitSafely()
        try {
            backgroundThread.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }

        closeCamera()
    }

    private fun getZoomRect(targetSize: Size): Rect {
        val cameraId = cameraManager.cameraIdList[0]
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
        val sensorSize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: Rect()
        val sensorAspectRatio = sensorSize.width().toFloat() / sensorSize.height()

        val targetAspectRatio = targetSize.width.toFloat() / targetSize.height
        val xDiff = (sensorSize.width() - targetSize.width) / 2
        val yDiff = (sensorSize.height() - targetSize.height) / 2

        return if (targetAspectRatio > sensorAspectRatio) {
            Rect(xDiff, yDiff, sensorSize.width() - xDiff, sensorSize.height() - yDiff)
        } else {
            Rect(yDiff, xDiff, sensorSize.height() - yDiff, sensorSize.width() - xDiff)
        }
    }

    private fun getClosestSize(sizes: Array<Size>?, targetWidth: Int, targetHeight: Int): Size {
        var closestSize: Size? = null
        var closestDistance = Int.MAX_VALUE

        sizes?.forEach { size ->
            val widthDistance = abs(size.width - targetWidth)
            val heightDistance = abs(size.height - targetHeight)
            val distance = widthDistance + heightDistance

            if (distance < closestDistance) {
                closestSize = size
                closestDistance = distance
            }
            Log.e(TAG, "closestSize: $closestSize, closestDistance: $closestDistance")
        }

        return closestSize ?: sizes?.first() ?: Size(0, 0)
    }

    private fun getIPAddress(): String {
        return "192.168.0.11"
//        val interfaces: List<NetworkInterface> = NetworkInterface.getNetworkInterfaces().toList()
//        val vpnInterfaces = interfaces.filter { it.displayName.contains(VPN_INTERFACE) }
//        val address: String by lazy { interfaces.findAddress().firstOrNull() ?: DEFAULT_IP }
//        return if (vpnInterfaces.isNotEmpty()) {
//            val vpnAddresses = vpnInterfaces.findAddress()
//            vpnAddresses.firstOrNull() ?: address
//        } else {
//            address
//        }
    }

    private fun List<NetworkInterface>.findAddress(): List<String?> = this.asSequence()
        .map { addresses -> addresses.inetAddresses.asSequence() }
        .flatten()
        .filter { address -> !address.isLoopbackAddress }
        .map { it.hostAddress }
        .filter { address -> address?.contains(":") == false }
        .toList()

    private fun sendFrameViaUDP(frameData: ByteArray, destinationAddress: InetAddress, port: Int) {
        val packetCount = ceil(frameData.size.toDouble() / MAX_PACKET_SIZE).toInt()

        for (i in 0 until packetCount) {
            val offset = i * MAX_PACKET_SIZE
            val length = (frameData.size - offset).coerceAtMost(MAX_PACKET_SIZE)
            val packetData = frameData.copyOfRange(offset, offset + length)

            val packet = DatagramPacket(packetData, length, destinationAddress, port)
            udpSocket.send(packet)
        }
    }

    companion object {
        private const val TAG = "CameraLiveStreamExample"
        private const val UDP_PORT = 5005
        private const val VPN_INTERFACE = "tun"
        private const val DEFAULT_IP = "0.0.0.0"

        private const val MAX_PACKET_SIZE = 65507
    }
}
