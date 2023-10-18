package kr.co.makeitall.rtspserver

import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.encoder.input.video.CameraOpenException
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.co.makeitall.arduino.UsbSerialManager
import kr.co.makeitall.rtspserver.databinding.ActivityCameraDemoBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CameraDemoActivity : AppCompatActivity(), ConnectCheckerRtsp, View.OnClickListener,
    SurfaceHolder.Callback {

    private lateinit var binding: ActivityCameraDemoBinding
    private lateinit var rtspServerCamera1: RtspServerCamera1
    private val tcpPacketServer = TcpPacketServer(PACKET_PORT)
    private lateinit var usbSerialManager: UsbSerialManager

    private var currentDateAndTime = ""
    private lateinit var folder: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = DataBindingUtil.setContentView(this@CameraDemoActivity, R.layout.activity_camera_demo)
        folder = File(getExternalFilesDir(null)!!.absolutePath + "/rtmp-rtsp-stream-client-java")

        binding.apply {
            bStartStop.setOnClickListener(this@CameraDemoActivity)
            bRecord.setOnClickListener(this@CameraDemoActivity)
            switchCamera.setOnClickListener(this@CameraDemoActivity)
            rtspServerCamera1 = RtspServerCamera1(surfaceView, this@CameraDemoActivity, RTSP_PORT)
            surfaceView.holder.addCallback(this@CameraDemoActivity)
            tvLogs.movementMethod = ScrollingMovementMethod()
        }

        UsbSerialManager(this@CameraDemoActivity, lifecycle).also {
            usbSerialManager = it
        }.addOnUsbReadListener { data ->
            val str = String(data)
            Log.i(TAG, "usb rx: $str")
            tcpPacketServer.send(str)
            binding.tvLogs.append("usb rx: $str\n")
        }.addOnStateListener { state ->
            Log.i(TAG, "state: $state")
        }.addOnPermissionListener { granted ->
            Log.i(TAG, "permission: $granted")
            CoroutineScope(Dispatchers.Default).launch {
                delay(3000L)
                usbSerialManager.startRead()
            }
        }

        tcpPacketServer.start()
        tcpPacketServer.setOnMessageListener { message ->
            Log.i(TAG, "tcp rx: $message")
            binding.tvLogs.append("tcp rx: $message\n")
            usbSerialManager.writeString("$message\n")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tcpPacketServer.stop()
    }

    override fun onNewBitrateRtsp(bitrate: Long) {

    }

    override fun onConnectionSuccessRtsp() {
        runOnUiThread {
            Toast.makeText(this@CameraDemoActivity, "Connection success", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionFailedRtsp(reason: String) {
        runOnUiThread {
            Toast.makeText(this@CameraDemoActivity, "Connection failed. $reason", Toast.LENGTH_SHORT)
                .show()
            rtspServerCamera1.stopStream()
            binding.bStartStop.setText(R.string.start_button)
        }
    }

    override fun onConnectionStartedRtsp(rtspUrl: String) {
    }

    override fun onDisconnectRtsp() {
        runOnUiThread {
            Toast.makeText(this@CameraDemoActivity, "Disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAuthErrorRtsp() {
        runOnUiThread {
            Toast.makeText(this@CameraDemoActivity, "Auth error", Toast.LENGTH_SHORT).show()
            rtspServerCamera1.stopStream()
            binding.bStartStop.setText(R.string.start_button)
            binding.tvUrl.text = ""
        }
    }

    override fun onAuthSuccessRtsp() {
        runOnUiThread {
            Toast.makeText(this@CameraDemoActivity, "Auth success", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.b_start_stop -> if (!rtspServerCamera1.isStreaming) {
                if (rtspServerCamera1.isRecording || rtspServerCamera1.prepareAudio() && rtspServerCamera1.prepareVideo(640, 480, 30, 1200 * 1024, CameraHelper.getCameraOrientation(this@CameraDemoActivity))) {
                    binding.bStartStop.setText(R.string.stop_button)
                    rtspServerCamera1.startStream()
                    binding.tvUrl.text = rtspServerCamera1.getEndPointConnection()
                    binding.tvIp.text = tcpPacketServer.ipAddress
                } else {
                    Toast.makeText(this, "Error preparing stream, This device cant do it", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                binding.bStartStop.setText(R.string.start_button)
                rtspServerCamera1.stopStream()
                binding.tvUrl.text = ""
            }

            R.id.switch_camera -> try {
                rtspServerCamera1.switchCamera()
            } catch (e: CameraOpenException) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }

            R.id.b_record -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    if (!rtspServerCamera1.isRecording) {
                        try {
                            if (!folder.exists()) {
                                folder.mkdir()
                            }
                            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                            currentDateAndTime = sdf.format(Date())
                            if (!rtspServerCamera1.isStreaming) {
                                if (rtspServerCamera1.prepareAudio() && rtspServerCamera1.prepareVideo()) {
                                    rtspServerCamera1.startRecord(folder.absolutePath + "/" + currentDateAndTime + ".mp4")
                                    binding.bRecord.setText(R.string.stop_record)
                                    Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(
                                        this, "Error preparing stream, This device cant do it",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                rtspServerCamera1.startRecord(folder.absolutePath + "/" + currentDateAndTime + ".mp4")
                                binding.bRecord.setText(R.string.stop_record)
                                Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: IOException) {
                            rtspServerCamera1.stopRecord()
                            binding.bRecord.setText(R.string.start_record)
                            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        rtspServerCamera1.stopRecord()
                        binding.bRecord.setText(R.string.start_record)
                        Toast.makeText(
                            this, "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(this, "You need min JELLY_BEAN_MR2(API 18) for do it...", Toast.LENGTH_SHORT).show()
                }
            }

            else -> {
            }
        }
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {
        rtspServerCamera1.startPreview()
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (rtspServerCamera1.isRecording) {
                rtspServerCamera1.stopRecord()
                binding.bRecord.setText(R.string.start_record)
                Toast.makeText(
                    this,
                    "file " + currentDateAndTime + ".mp4 saved in " + folder.absolutePath,
                    Toast.LENGTH_SHORT
                ).show()
                currentDateAndTime = ""
            }
        }
        if (rtspServerCamera1.isStreaming) {
            rtspServerCamera1.stopStream()
            binding.bStartStop.text = resources.getString(R.string.start_button)
            binding.tvUrl.text = ""
        }
        rtspServerCamera1.stopPreview()
    }

    companion object {
        private const val TAG = "CameraDemoActivity"

        private const val RTSP_PORT = 5000
        private const val PACKET_PORT = 5001
    }
}
