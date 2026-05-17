package com.smarter.video

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

    private lateinit var videoUris: ArrayList<String>
    private var currentIndex = 0

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var gestureDetector: GestureDetector

    // 🌟 新增：用于自动隐藏进度条的计时器任务
    private val hideSeekBarRunnable = Runnable {
        if (player.isPlaying) {
            seekBar.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        // 🌟 初始化：一进来视频默认开始播放，进度条先默认隐藏
        seekBar.visibility = View.GONE

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

        // 🌟 核心改进：绑定播放器状态监听器
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    playNextVideo()
                }
            }

            // 监听播放/暂停的真正状态切换
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    // 如果进入【播放】状态：触发倒计时 3 秒后自动隐藏进度条
                    handler.removeCallbacks(hideSeekBarRunnable)
                    handler.postDelayed(hideSeekBarRunnable, 3000)
                } else {
                    // 如果进入【暂停】状态：强行让进度条【永远可见】
                    handler.removeCallbacks(hideSeekBarRunnable)
                    seekBar.visibility = View.VISIBLE
                }
            }
        })

        playCurrentVideo()

        // 🌟 核心改进：点击屏幕的交互逻辑
        playerView.setOnClickListener {
            if (player.isPlaying) {
                // 如果当前正在播放：点击屏幕 -> 触发暂停（触发后会由于上面的监听自动显示进度条）
                player.pause()
            } else {
                // 如果当前是暂停的：点击屏幕 -> 触发恢复播放
                // 先把进度条唤醒亮起，然后开始 3 秒倒计时自动隐去
                seekBar.visibility = View.VISIBLE
                player.play()
            }
        }
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

                // 🌟 新增：单击屏幕时如果正在播放，短暂亮起进度条再自动隐藏
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (player.isPlaying) {
                        if (seekBar.visibility == View.VISIBLE) {
                            seekBar.visibility = View.GONE
                        } else {
                            seekBar.visibility = View.VISIBLE
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
            // 🌟 关键改动：返回 false，确保 setOnClickListener 的点击事件不被手势完全吞掉
            false
        }
    }

    private fun setupSeekBar() {
        handler.post(object : Runnable {
            override fun run() {
                // 🌟 只有当进度条可见时，才浪费性能去刷新位置，不可见时静默，优化性能
                if (player.duration > 0 && seekBar.visibility == View.VISIBLE) {
                    seekBar.max = player.duration.toInt()
                    seekBar.progress = player.currentPosition.toInt()
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
                        // 🌟 用户手动拖动进度条时，重新刷新 3 秒隐藏的倒计时，防止拖到一半突然隐形
                        if (player.isPlaying) {
                            handler.removeCallbacks(hideSeekBarRunnable)
                            handler.postDelayed(hideSeekBarRunnable, 3000)
                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    // 用户开始拖动，移除隐藏任务
                    handler.removeCallbacks(hideSeekBarRunnable)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // 用户松开手，如果是播放中，恢复 3 秒后隐藏
                    if (player.isPlaying) {
                        handler.removeCallbacks(hideSeekBarRunnable)
                        handler.postDelayed(hideSeekBarRunnable, 3000)
                    }
                }
            }
        )
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
