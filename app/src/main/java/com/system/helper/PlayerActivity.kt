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
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.smarter.video.databinding.ActivityPlayerBinding
import kotlin.math.abs

private const val AUTO_HIDE_DELAY = 3000L
private const val SEEK_UPDATE_INTERVAL = 500L

class PlayerActivity : BaseActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var videoUris = arrayListOf<String>()
    private var currentIndex = 0

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var gestureDetector: GestureDetector

    // 控制进度条自动隐藏的任务
    private val hideSeekBarRunnable = Runnable {
        binding.seekBar.visibility = View.GONE
    }

    // 轮询更新进度条界面的任务
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            val currentPlayer = player
            // 标准写法：直接使用 View.VISIBLE 判断
            if (currentPlayer != null && currentPlayer.isPlaying && binding.seekBar.visibility == View.VISIBLE) {
                if (currentPlayer.duration > 0) {
                    binding.seekBar.max = currentPlayer.duration.toInt()
                    binding.seekBar.progress = currentPlayer.currentPosition.toInt()
                }
            }
            // 只要页面没销毁，就持续保持轮询
            handler.postDelayed(this, SEEK_UPDATE_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 基础全屏与防截屏/防录屏安全设置
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 默认隐藏进度条
        binding.seekBar.visibility = View.GONE

        // 初始化数据源
        videoUris = intent.getStringArrayListExtra("video_list") ?: arrayListOf()
        currentIndex = intent.getIntExtra("current_index", 0)

        if (videoUris.isEmpty()) {
            Toast.makeText(this, "没有视频可播放", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initPlayer()
        setupGestureAndClick()
        setupSeekBar()
        
        playCurrentVideo()
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            binding.playerView.player = this
            binding.playerView.useController = false // 完全使用自定义 UI
            
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        playNextVideo()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateSeekBarVisibility(isPlaying)
                }

                override fun onPlayerError(error: PlaybackException) {
                    Toast.makeText(this@PlayerActivity, "播放错误: ${error.message}", Toast.LENGTH_SHORT).show()
                    playNextVideo()
                }
            })
        }
    }

    private fun updateSeekBarVisibility(isPlaying: Boolean) {
        handler.removeCallbacks(hideSeekBarRunnable)
        binding.seekBar.visibility = View.VISIBLE
        // 如果正在播放，则开启 3 秒后自动隐藏定时器；暂停时则持久显示
        if (isPlaying) {
            handler.postDelayed(hideSeekBarRunnable, AUTO_HIDE_DELAY)
        }
    }

    private fun setupGestureAndClick() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            // 响应左右轻扫手势（切换上下首视频）
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (abs(velocityX) > 800 && abs(velocityX) > abs(velocityY) * 1.5f) {
                    if (velocityX > 0) playPreviousVideo() else playNextVideo()
                    return true
                }
                return false
            }

            // 单击屏幕的交互：显示/隐藏进度条，或者从暂停中恢复播放
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val currentPlayer = player ?: return false
                if (currentPlayer.isPlaying) {
                    if (binding.seekBar.visibility == View.VISIBLE) {
                        binding.seekBar.visibility = View.GONE
                        handler.removeCallbacks(hideSeekBarRunnable)
                    } else {
                        binding.seekBar.visibility = View.VISIBLE
                        handler.postDelayed(hideSeekBarRunnable, AUTO_HIDE_DELAY)
                    }
                } else {
                    // 如果是暂停状态，单击屏幕直接恢复播放
                    binding.seekBar.visibility = View.VISIBLE
                    currentPlayer.play()
                }
                return true
            }
        })

        // 将触摸流代理给 GestureDetector
        binding.playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            // 手势抬起时，如果是正在播放状态且没有触发轻扫/单击，用于处理正常的暂停
            if (event.action == MotionEvent.ACTION_UP) {
                val currentPlayer = player
                if (currentPlayer != null && currentPlayer.isPlaying && binding.seekBar.visibility == View.VISIBLE) {
                    currentPlayer.pause()
                }
            }
            true
        }
    }

    private fun setupSeekBar() {
        // 启动进度条 UI 定时轮询更新
        handler.post(updateProgressRunnable)

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player?.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 用户开始拖动时，取消自动隐藏，防止拖动到一半 UI 消失
                handler.removeCallbacks(hideSeekBarRunnable)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 拖动结束后，如果仍在播放，恢复 3 秒自动隐藏
                if (player?.isPlaying == true) {
                    handler.postDelayed(hideSeekBarRunnable, AUTO_HIDE_DELAY)
                }
            }
        })
    }

    private fun playCurrentVideo() {
        val currentPlayer = player ?: return
        try {
            val uri = Uri.parse(videoUris[currentIndex])
            setVideoOrientation(uri)

            currentPlayer.stop()
            currentPlayer.setMediaItem(MediaItem.fromUri(uri))
            currentPlayer.prepare()
            currentPlayer.play()
        } catch (e: Exception) {
            Toast.makeText(this, "播放失败: ${e.message}", Toast.LENGTH_SHORT).show()
            playNextVideo()
        }
    }

    private fun setVideoOrientation(uri: Uri) {
        try {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(this, uri)
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0

                requestedOrientation = if (height > width) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }
        } catch (e: Exception) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }

    private fun playNextVideo() {
        if (currentIndex < videoUris.size - 1) {
            currentIndex++
            playCurrentVideo()
        } else {
            finish()
        }
    }

    private fun playPreviousVideo() {
        if (currentIndex > 0) {
            currentIndex--
            playCurrentVideo()
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
        // 页面不可见时，移除所有后台异步通知，彻底切断内存泄漏隐患
        handler.removeCallbacks(updateProgressRunnable)
        handler.removeCallbacks(hideSeekBarRunnable)
    }

    override fun onResume() {
        super.onResume()
        // 页面恢复可见时，重新拉起进度条轮询任务
        handler.post(updateProgressRunnable)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        super.onDestroy()
    }
}
