package com.example.bonfire.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bonfire.R
import dagger.hilt.android.AndroidEntryPoint
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import kotlinx.android.synthetic.main.activity_video_call.*
import kotlin.random.Random

@AndroidEntryPoint
class VideoCallActivity : AppCompatActivity() {

    private var oldDirection: Int = 0

    private var mRtcEngine: RtcEngine? = null
    private val mRtcEventHandler = object : IRtcEngineEventHandler() {

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread { onRemoteUserLeft() }
        }

        override fun onUserMuteVideo(uid: Int, muted: Boolean) {
            runOnUiThread { onRemoteUserVideoMuted(uid, muted) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        initAgoraEngineAndJoinChannel()

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) && checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA)) {
            initAgoraEngineAndJoinChannel()
        }

        button.setOnClickListener {
            truthAndDare()
        }
    }

    private fun initAgoraEngineAndJoinChannel() {
        initializeAgoraEngine()
        setupVideoProfile()
        setupLocalVideo()
        joinChannel()
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        Log.i(LOG_TAG, "checkSelfPermission $permission $requestCode")
        if (ContextCompat.checkSelfPermission(this,
                permission) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(permission),
                requestCode)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i(LOG_TAG, "onRequestPermissionsResult " + grantResults[0] + " " + requestCode)

        when (requestCode) {
            PERMISSION_REQ_ID_RECORD_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA)
                } else {
                    showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO)
                    finish()
                }
            }
            PERMISSION_REQ_ID_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initAgoraEngineAndJoinChannel()
                } else {
                    showLongToast("No permission for " + Manifest.permission.CAMERA)
                    finish()
                }
            }
        }
    }

    private fun showLongToast(msg: String) {
        this.runOnUiThread { Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show() }
    }

    override fun onDestroy() {
        super.onDestroy()

        leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null
    }

    fun onLocalVideoMuteClicked(view: View) {
        val iv = view as ImageView
        if (iv.isSelected) {
            iv.isSelected = false
            iv.clearColorFilter()
        } else {
            iv.isSelected = true
            iv.setColorFilter(resources.getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY)
        }

        // Stops/Resumes sending the local video stream.
        mRtcEngine!!.muteLocalVideoStream(iv.isSelected)

        val container = findViewById(R.id.local_video_view_container) as FrameLayout
        val surfaceView = container.getChildAt(0) as SurfaceView
        surfaceView.setZOrderMediaOverlay(!iv.isSelected)
        surfaceView.visibility = if (iv.isSelected) View.GONE else View.VISIBLE
    }

    fun onLocalAudioMuteClicked(view: View) {
        val iv = view as ImageView
        if (iv.isSelected) {
            iv.isSelected = false
            iv.clearColorFilter()
        } else {
            iv.isSelected = true
            iv.setColorFilter(resources.getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY)
        }

        // Stops/Resumes sending the local audio stream.
        mRtcEngine!!.muteLocalAudioStream(iv.isSelected)
    }

    fun onSwitchCameraClicked(view: View) {
        // Switches between front and rear cameras.
        mRtcEngine!!.switchCamera()
    }

    fun onEncCallClicked(view: View) {
        finish()
    }

    private fun initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(baseContext, getString(R.string.agora_app_id), mRtcEventHandler)
        } catch (e: Exception) {
            Log.e(LOG_TAG, Log.getStackTraceString(e))

            throw RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e))
        }
    }

    private fun setupVideoProfile() {
        mRtcEngine!!.enableVideo()
//      mRtcEngine!!.setVideoProfile(Constants.VIDEO_PROFILE_360P, false) // Earlier than 2.3.0

        mRtcEngine!!.setVideoEncoderConfiguration(VideoEncoderConfiguration(VideoEncoderConfiguration.VD_640x360,
            VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
            VideoEncoderConfiguration.STANDARD_BITRATE,
            VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT))
    }

    private fun setupLocalVideo() {
        val container = findViewById(R.id.local_video_view_container) as FrameLayout
        val surfaceView = RtcEngine.CreateRendererView(baseContext)
        surfaceView.setZOrderMediaOverlay(true)
        container.addView(surfaceView)
        mRtcEngine!!.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
    }

    private fun joinChannel() {
        var token: String? = getString(R.string.agora_access_token)
        if (token!!.isEmpty()) {
            token = null
        }
        mRtcEngine!!.joinChannel(token, "demoChannel1", "Extra Optional Data", 0) // if you do not specify the uid, we will generate the uid for you
    }

    private fun setupRemoteVideo(uid: Int) {
        val container = findViewById(R.id.remote_video_view_container) as FrameLayout

        if (container.childCount >= 1) {
            return
        }

        val surfaceView = RtcEngine.CreateRendererView(baseContext)
        container.addView(surfaceView)
        // Initializes the video view of a remote user.
        mRtcEngine!!.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))

        surfaceView.tag = uid // for mark purpose
        val tipMsg = findViewById<TextView>(R.id.quick_tips_when_use_agora_sdk) // optional UI
        tipMsg.visibility = View.GONE
    }

    private fun leaveChannel() {
        mRtcEngine!!.leaveChannel()
    }

    private fun onRemoteUserLeft() {
        val container = findViewById(R.id.remote_video_view_container) as FrameLayout
        container.removeAllViews()

        val tipMsg = findViewById<TextView>(R.id.quick_tips_when_use_agora_sdk) // optional UI
        tipMsg.visibility = View.VISIBLE
    }

    private fun onRemoteUserVideoMuted(uid: Int, muted: Boolean) {
        val container = findViewById(R.id.remote_video_view_container) as FrameLayout

        val surfaceView = container.getChildAt(0) as SurfaceView

        val tag = surfaceView.tag
        if (tag != null && tag as Int == uid) {
            surfaceView.visibility = if (muted) View.GONE else View.VISIBLE
        }
    }

    private fun truthAndDare() {
        val newDirection = Random(System.nanoTime()).nextInt(3600) + 360
        val pivotX = bottleImageView.width / 2
        val pivotY = bottleImageView.height / 2
        val rotate = RotateAnimation(oldDirection.toFloat(), newDirection.toFloat(), pivotX.toFloat(), pivotY.toFloat())
        rotate.duration = 2000
        rotate.fillAfter = true

        oldDirection = newDirection

        rotate.setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                bottleImageView.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(p0: Animation?) {
                Toast.makeText(this@VideoCallActivity, "ENDED", Toast.LENGTH_SHORT).show()
                bottleImageView.visibility = View.INVISIBLE
            }

            override fun onAnimationRepeat(p0: Animation?) {
                TODO("Not yet implemented")
            }

        })

        bottleImageView.startAnimation(rotate)
    }

    companion object {

        private val LOG_TAG = VideoCallActivity::class.java.simpleName

        private const val PERMISSION_REQ_ID_RECORD_AUDIO = 22
        private const val PERMISSION_REQ_ID_CAMERA = PERMISSION_REQ_ID_RECORD_AUDIO + 1
    }
}