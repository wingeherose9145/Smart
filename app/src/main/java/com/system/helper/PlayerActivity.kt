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
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var seekBar: SeekBar
    private lateinit var timeTextView: TextView  // 新增：左上角时间显示

    private lateinit var videoUris: ArrayList<String>
    private var currentIndex = 0

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var gestureDetector: GestureDetector

    private val hideSeekBarRunnable = Runnable {
        if (player.isPlaying) {
            seekBar.visibility = View.GONE
            timeTextView.visibility = View.GONE  // 同步隐藏时间显示
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🌟 核心验证：检查播放器调起时是否携带计算器认证 Token
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
        timeTextView = findViewById(R.id.timeTextView)  // 初始化时间显示控件

        seekBar.visibility = View.GONE
        timeTextView.visibility = View.GONE

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
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    handler.removeCallbacks(hideSeekBarRunnable)
                    handler.postDelayed(hideSeekBarRunnable, 3000)
                } else {
                    handler.removeCallbacks(hideSeekBarRunnable)
                    seekBar.visibility = View.VISIBLE
                    timeTextView.visibility = View.VISIBLE  // 同步显示时间
                }
            }
        })

        playCurrentVideo()

        playerView.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
            } else {
                seekBar.visibility = View.VISIBLE
                timeTextView.visibility = View.VISIBLE  // 同步显示时间
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
        // 🌟 核心需求 2：防止通过最近任务或切换后台直接回到播放页面，强制退出锁死
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
                        if (seekBar.visibility == View.VISIBLE) {
                            seekBar.visibility = View.GONE
                            timeTextView.visibility = View.GONE  // 同步隐藏
                        } else {
                            seekBar.visibility = View.VISIBLE
                            timeTextView.visibility = View.VISIBLE  // 同步显示
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
                val duration = player.duration
                val position = player.currentPosition

                if (duration > 0) {
                    seekBar.max = duration.toInt()
                    seekBar.progress = position.toInt()

                    // 更新左上角时间显示：当前时间 / 总时长
                    val currentTime = formatTime(position)
                    val totalTime = formatTime(duration)
                    timeTextView.text = "$currentTime / $totalTime"

                    // 保持与 SeekBar 显示状态同步
                    if (seekBar.visibility == View.VISIBLE) {
                        timeTextView.visibility = View.VISIBLE
                    }
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

    // 时间格式化：毫秒 → MM:SS
    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
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
