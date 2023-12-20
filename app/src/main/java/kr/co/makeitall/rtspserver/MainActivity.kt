package kr.co.makeitall.rtspserver

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import kr.co.makeitall.rtspserver.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)

        binding.bCameraDemo.setOnClickListener {
            if (!hasPermissions(this, *PERMISSIONS)) {
                Timber.e("Permission denied.")
                ActivityCompat.requestPermissions(this, PERMISSIONS, 1)
            } else {
                startActivity(Intent(this, CameraDemoActivity::class.java))
            }
        }
    }

    private fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }

    companion object {
        private val PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    }
}
