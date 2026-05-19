package com.smarter.video

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smarter.video.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream

// 常量抽取
private const val PREF_NAME = "video_list"
private const val KEY_URIS = "uris"
private const val KEY_NAMES = "names"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val videoUris = mutableListOf<Uri>()
    private val displayNames = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    private val pickVideosLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) importVideos(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListView()
        loadAndScanVideos()

        binding.addButton.setOnClickListener {
            pickVideosLauncher.launch("video/*")
        }
    }

    private fun setupListView() {
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayNames)
        binding.videoListView.adapter = adapter

        binding.videoListView.setOnItemClickListener { _, _, position, _ ->
            startPlayerActivity(position)
        }

        binding.videoListView.setOnItemLongClickListener { _, _, position, _ ->
            if (position in videoUris.indices) {
                showDeleteDialog(position)
            }
            true
        }
    }

    private fun loadAndScanVideos() {
        videoUris.clear()
        displayNames.clear()

        loadSavedVideoList()
        scanInternalVideosFolder()

        // 排序
        val sorted = videoUris.zip(displayNames).sortedBy { it.second.lowercase() }
        videoUris.clear()
        displayNames.clear()
        sorted.forEach { (u, n) ->
            videoUris.add(u)
            displayNames.add(n)
        }

        adapter.notifyDataSetChanged()
        updateEmptyState()
        saveVideoList()
    }

    private fun updateEmptyState() {
        binding.emptyView.visibility = if (videoUris.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun scanInternalVideosFolder() { /* 同之前版本 */ 
        val videosDir = File(getExternalFilesDir(null), "videos")
        if (!videosDir.exists()) return

        val existing = videoUris.map { it.path }.toSet()
        videosDir.listFiles()?.forEach { file ->
            if (file.isFile && isVideoFile(file)) {
                val uri = Uri.fromFile(file)
                if (uri.path !in existing) {
                    videoUris.add(uri)
                    displayNames.add(file.name)
                }
            }
        }
    }

    private fun isVideoFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in listOf("mp4", "mkv", "mov", "avi", "wmv", "flv", "webm", "3gp", "m4v")
    }

    // 其他函数（importVideos、getFileNameFromUri、deleteVideo、save/load）保持和之前一致
    // ...（为节省篇幅，这里省略完全相同的部分，你可以直接复制上一个版本的对应函数）

    private fun startPlayerActivity(position: Int) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putStringArrayListExtra("video_list", ArrayList(videoUris.map { it.toString() }))
            putExtra("current_index", position)
        }
        startActivity(intent)
    }

    private fun showDeleteDialog(position: Int) { /* 同之前 */ }
}
