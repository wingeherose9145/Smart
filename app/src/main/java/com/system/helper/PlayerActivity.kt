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
import androidx.media3.ui.PlayerView
import com.smarter.video.databinding.ActivityPlayerBinding

private const val AUTO_HIDE_DELAY = 3000L
private const val SEEK_UPDATE_INTERVAL = 500L

class PlayerActivity : BaseActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var player: ExoPlayer
    private lateinit var videoUris: ArrayList<String>
    private var currentIndex = 0

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var gestureDetector: GestureDetector

    private val hideSeekBarRunnable = Runnable {
        if (player.isPlaying) binding.seekBar.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR

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
            Toast.makeText(this, "没有视频可播放", Toast.LENGTH_SHORT).show()
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

    private fun updateSeekBarVisibility(isPlaying: Boolean) {
        handler.removeCallbacks(hideSeekBarRunnable)
        if (isPlaying) {
            binding.seekBar.visibility = View.VISIBLE
            handler.postDelayed(hideSeekBarRunnable, AUTO_HIDE_DELAY)
        } else {
            binding.seekBar.visibility = View.VISIBLE
        }
    }

    private fun setupGestureAndClick() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (kotlin.math.abs(velocityX) > 800 && kotlin.math.abs(velocityX) > kotlin.math.abs(velocityY) * 1.5f) {
                    if (velocityX > 0) playPreviousVideo() else playNextVideo()
                    return true
                }
                return false
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (player.isPlaying) {
                    if (binding.seekBar.visibility == View.VISIBLE) {
                        binding.seekBar.visibility = View.GONE
                    } else {
                        binding.seekBar.visibility = View.VISIBLE
                        handler.removeCallbacks(hideSeekBarRunnable)
                        handler.postDelayed(hideSeekBarRunnable, AUTO_HIDE_DELAY)
                    }
                }
                return true
            }
        })

        binding.playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        binding.playerView.setOnClickListener {
            if (player.isPlaying) player.pause() else {
                binding.seekBar.visibility = View.createView(View.VISIBLE) // 修正一个小语法兼容
                binding.seekBar.visibility = View.VISIBLE
                player.play()
            }
        }
    }

    private fun setupSeekBar() {
        handler.post(object : Runnable {
            override fun run() {
                if (binding.seekBar.visibility == View.VISIBLE && player.duration > 0) {
                    binding.seekBar.max = player.duration.toInt()
                    binding.seekBar.progress = player.currentPosition.toInt()
                }
                handler.postDelayed(this, SEEK_UPDATE_INTERVAL)
            }
        })

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) player.seekTo(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                handler.removeCallbacks(hideSeekBarRunnable)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (player.isPlaying) handler.postDelayed(hideSeekBarRunnable, AUTO_HIDE_DELAY)
            }
        })
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
            Toast.makeText(this, "播放失败: ${e.message}", Toast.LENGTH_SHORT).show()
            playNextVideo()
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
        player.pause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player.release()
        super.onDestroy()
    }
}
