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
import com.smarter.video.databinding.ActivityPlayerBinding

// 常量
private const val AUTO_HIDE_DELAY = 3000L
private const val SEEK_BAR_UPDATE_INTERVAL = 500L

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var player: ExoPlayer
    private lateinit var videoUris: ArrayList<String>
    private var currentIndex = 0

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var gestureDetector: GestureDetector

    // 统一管理 SeekBar 可见性
    private val hideSeekBarRunnable = Runnable {
        if (player.isPlaying) binding.seekBar.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 方向切换时不重建 Activity
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR // 推荐使用 SENSOR

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.seekBar.visibility = View.GONE

        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
        binding.playerView.useController = false

        videoUris = intent.getStringArrayListExtra("video_list") ?: arrayListOf()
        currentIndex = intent.getIntExtra("current_index", 0)

        if (videoUris.isEmpty()) {
            finish()
            return
        }

        setupGestureAndClick()
        setupSeekBar()
        setupPlayerListener()

        playCurrentVideo()
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) playNextVideo()
                else if (state == Player.STATE_IDLE || state == Player.STATE_BUFFERING) {
                    // 可扩展错误处理
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateSeekBarVisibility(isPlaying)
            }

            override fun onPlayerError(error: PlayerException) {
                Toast.makeText(this@PlayerActivity, "播放错误: ${error.message}", Toast.LENGTH_SHORT).show()
                playNextVideo()
            }
        })
    }

    // ==================== 统一 SeekBar 可见性管理 ====================
    private fun updateSeekBarVisibility(isPlaying: Boolean) {
        handler.removeCallbacks(hideSeekBarRunnable)
        if (isPlaying) {
            handler.postDelayed(hideSeekBarRunnable, AUTO_HIDE_DELAY)
            binding.seekBar.visibility = View.VISIBLE // 先显示再自动隐藏
        } else {
            binding.seekBar.visibility = View.VISIBLE
        }
    }

    private fun setupGestureAndClick() {
        // Gesture + Click 逻辑（同之前优化版）
        // ...（可直接使用上一个版本的 setupGestureDetector 内容）
    }

    private fun setupSeekBar() { /* 同之前 */ }

    private fun playCurrentVideo() { /* 同之前，保持 try-catch */ }

    private fun playNextVideo() { /* 同之前 */ }
    private fun playPreviousVideo() { /* 同之前 */ }

    override fun onPause() {
        super.onPause()
        player.pause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player.release()
        super.onDestroy()
    }
}
