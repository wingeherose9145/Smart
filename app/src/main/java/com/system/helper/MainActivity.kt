package com.smarter.video

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var addButton: Button
    private val videoUris = mutableListOf<Uri>()
    private val displayNames = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    // 系统视频选择器
    private val pickVideosLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            Toast.makeText(this, "正在导入 ${uris.size} 个视频...", Toast.LENGTH_SHORT).show()
            importVideos(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.videoListView)
        addButton = findViewById(R.id.addButton)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayNames)
        listView.adapter = adapter

        addButton.text = "添加视频"
        addButton.setOnClickListener {
            pickVideosLauncher.launch("video/*")
        }

        // 点击播放
        listView.setOnItemClickListener { _, _, position, _ ->
            startPlayerActivity(position)
        }

        // 长按删除
        listView.setOnItemLongClickListener { _, _, position, _ ->
            if (position in videoUris.indices) {
                AlertDialog.Builder(this)
                    .setTitle("删除视频")
                    .setMessage("确认删除该视频吗？（文件将被永久删除）")
                    .setPositiveButton("删除") { _, _ ->
                        deleteVideo(position)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
            true
        }

        // 加载视频（核心：扫描 + 加载保存列表）
        loadAndScanVideos()
    }

    // ==================== 核心：扫描 + 加载 ====================
    private fun loadAndScanVideos() {
        videoUris.clear()
        displayNames.clear()

        loadSavedVideoList()
        scanInternalVideosFolder()

        // 按文件名排序
        val combined = videoUris.zip(displayNames)
            .sortedBy { it.second.lowercase() }

        videoUris.clear()
        displayNames.clear()
        combined.forEach { (uri, name) ->
            videoUris.add(uri)
            displayNames.add(name)
        }

        adapter.notifyDataSetChanged()
        saveVideoList() // 同步保存最新状态
    }

    // 扫描内部 videos 文件夹，补充手动放入的文件
    private fun scanInternalVideosFolder() {
        val videosDir = File(getExternalFilesDir(null), "videos")
        if (!videosDir.exists()) return

        val existingPaths = videoUris.map { it.path }.toSet()

        videosDir.listFiles()?.forEach { file ->
            if (file.isFile && isVideoFile(file)) {
                val uri = Uri.fromFile(file)
                if (!existingPaths.contains(uri.path)) {
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

    // 异步导入视频（拷贝到内部存储）
    private fun importVideos(uris: List<Uri>) {
        Thread {
            var added = 0
            val videosDir = File(getExternalFilesDir(null), "videos").apply { mkdirs() }

            uris.forEach { sourceUri ->
                val name = getFileNameFromUri(sourceUri)
                val targetFile = File(videosDir, "${System.currentTimeMillis()}_$name")

                try {
                    contentResolver.openInputStream(sourceUri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    runOnUiThread {
                        videoUris.add(Uri.fromFile(targetFile))
                        displayNames.add(targetFile.name)
                        adapter.notifyDataSetChanged()
                    }
                    added++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            runOnUiThread {
                saveVideoList()
                Toast.makeText(this, "成功导入 $added 个视频", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "video_${System.currentTimeMillis()}.mp4"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1 && cursor.moveToFirst()) {
                    cursor.getString(index)?.let { if (it.isNotBlank()) name = it }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return name
    }

    private fun deleteVideo(position: Int) {
        try {
            val uri = videoUris[position]
            if (uri.scheme == "file") {
                File(uri.path!!).delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        videoUris.removeAt(position)
        displayNames.removeAt(position)
        adapter.notifyDataSetChanged()
        saveVideoList()
        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
    }

    // 保存 / 加载（仅记录 Uri 字符串）
    private fun saveVideoList() {
        val prefs = getSharedPreferences("video_list", MODE_PRIVATE).edit()
        prefs.putString("uris", Gson().toJson(videoUris.map { it.toString() }))
        prefs.putString("names", Gson().toJson(displayNames))
        prefs.apply()
    }

    private fun loadSavedVideoList() {
        val prefs = getSharedPreferences("video_list", MODE_PRIVATE)
        val uriJson = prefs.getString("uris", null) ?: return
        val nameJson = prefs.getString("names", null) ?: return

        try {
            val uris: List<String> = Gson().fromJson(uriJson, object : TypeToken<List<String>>() {}.type)
            val names: List<String> = Gson().fromJson(nameJson, object : TypeToken<List<String>>() {}.type)

            uris.forEach { videoUris.add(Uri.parse(it)) }
            displayNames.addAll(names)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPlayerActivity(position: Int) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putStringArrayListExtra("video_list", ArrayList(videoUris.map { it.toString() }))
            putExtra("current_index", position)
        }
        startActivity(intent)
    }
}
