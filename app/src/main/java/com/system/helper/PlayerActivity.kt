package com.smarter.video

import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.util.Formatter
import java.util.Locale

class PlayerActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var seekBar: SeekBar
    
    private lateinit var controllerLayout: LinearLayout
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalDuration: TextView

    private lateinit var videoUris: ArrayList<String>
    private var currentIndex = 0

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var gestureDetector: GestureDetector

    private val hideSeekBarRunnable = Runnable {
        if (player.isPlaying) {
            controllerLayout.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.getStringExtra("SECURE_ENTRY_TOKEN") != "PASSED_FROM_CALCULATOR_2026") {
            redirectToCalculator()
            return
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)
        seekBar = findViewById(R.id.seekBar)
        
        controllerLayout = findViewById(R.id.controllerLayout)
        tvCurrentTime = findViewById(R.id.tv_current_time)
        tvTotalDuration = findViewById(R.id.tv_total_duration)

        // 默认让其可见，视频开始播放后自动倒计时隐藏
        controllerLayout.visibility = View.VISIBLE

        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        playerView.useController = false

        videoUris = intent.getStringArrayListExtra("video_list") ?: arrayListOf()
        currentIndex = intent.getIntExtra("current_index", 0)

        if (videoUris.isEmpty()) {
            Toast.makeText(this, "没有视频可播放", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupGestureDetector()
        setupSeekBar()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    playNextVideo()
                } else if (state == Player.STATE_READY) {
                    // 🌟 核心增强：视频一旦解析准备就绪，立刻无延迟刷新一次总时间文本
                    val duration = player.duration
                    if (duration > 0) {
                        seekBar.max = duration.toInt()
                        tvTotalDuration.text = formatTime(duration)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    controllerLayout.visibility = View.VISIBLE
                    handler.removeCallbacks(hideSeekBarRunnable)
                    handler.postDelayed(hideSeekBarRunnable, 3000)
                } else {
                    handler.removeCallbacks(hideSeekBarRunnable)
                    controllerLayout.visibility = View.VISIBLE
                }
            }
        })

        playCurrentVideo()

        playerView.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
            } else {
                controllerLayout.visibility = View.VISIBLE
                player.play()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (intent.getStringExtra("SECURE_ENTRY_TOKEN") != "PASSED_FROM_CALCULATOR_2026") {
            redirectToCalculator()
        }
    }

    override fun onRestart() {
        super.onRestart()
        redirectToCalculator()
    }

    private fun redirectToCalculator() {
        player.pause()
        val intent = Intent(this, CalculatorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun playCurrentVideo() {
        try {
            val uri = Uri.parse(videoUris[currentIndex])
            setVideoOrientation(uri)

            player.stop()
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.play()

        } catch (e: Exception) {
            Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setVideoOrientation(uri: Uri) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)

            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0

            requestedOrientation = if (height > width) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            retriever.release()
        } catch (e: Exception) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private fun playNextVideo() {
        if (currentIndex < videoUris.size - 1) {
            currentIndex++
            playCurrentVideo()
        }
    }

    private fun playPreviousVideo() {
        if (currentIndex > 0) {
            currentIndex--
            playCurrentVideo()
        }
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (kotlin.math.abs(velocityX) > 700) {
                        if (velocityX > 0) {
                            playPreviousVideo()
                        } else {
                            playNextVideo()
                        }
                        return true
                    }
                    return false
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (player.isPlaying) {
                        if (controllerLayout.visibility == View.VISIBLE) {
                            controllerLayout.visibility = View.GONE
                        } else {
                            controllerLayout.visibility = View.VISIBLE
                            handler.removeCallbacks(hideSeekBarRunnable)
                            handler.postDelayed(hideSeekBarRunnable, 3000)
                        }
                        return true
                    }
                    return false
                }
            }
        )

        playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun setupSeekBar() {
        handler.post(object : Runnable {
            override fun run() {
                // 🌟 核心改进：移除控制条必须可见的苛刻限制，让进度和时间在后台静默高频更新。
                // 这样无论任何时候点击屏幕唤醒控制条，时间数据都是完美的，不会显示 00:00
                val duration = player.duration
                if (duration > 0) {
                    seekBar.max = duration.toInt()
                    seekBar.progress = player.currentPosition.toInt()
                    
                    tvCurrentTime.text = formatTime(player.currentPosition)
                    tvTotalDuration.text = formatTime(duration)
                }
                handler.postDelayed(this, 500)
            }
        })

        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        player.seekTo(progress.toLong())
                        tvCurrentTime.text = formatTime(progress.toLong())
                        
                        if (player.isPlaying) {
                            handler.removeCallbacks(hideSeekBarRunnable)
                            handler.postDelayed(hideSeekBarRunnable, 3000)
                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    handler.removeCallbacks(hideSeekBarRunnable)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (player.isPlaying) {
                        handler.removeCallbacks(hideSeekBarRunnable)
                        handler.postDelayed(hideSeekBarRunnable, 3000)
                    }
                }
            }
        )
    }

    private fun formatTime(timeMs: Long): String {
        if (timeMs < 0) return "00:00"
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600

        val sb = java.lang.StringBuilder()
        val formatter = Formatter(sb, Locale.getDefault())
        return if (hours > 0) {
            formatter.format("%02d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            formatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    override fun onPause() {
        super.onPause()
        player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        player.release()
    }
}
