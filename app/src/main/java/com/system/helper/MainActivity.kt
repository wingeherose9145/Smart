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
    private val videoUris = mutableListOf<Uri>()
    private val displayNames = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var addButton: Button

    // 🌟 核心改进 1：利用系统原生选择器，无需任何高危存储权限，支持多选
    private val pickVideosLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            Toast.makeText(this, "正在后台导入选中的 ${uris.size} 个视频，请稍候...", Toast.LENGTH_SHORT).show()

            // 🌟 核心改进 2：开辟子线程进行文件拷贝，防止主线程卡死（ANR）
            Thread {
                var addedCount = 0
                uris.forEach { sourceUri ->
                    val name = getFileNameFromUri(sourceUri)
                    val internalUri = copyVideoToInternalStorage(sourceUri, name)
                    if (internalUri != null) {
                        runOnUiThread {
                            videoUris.add(internalUri)
                            displayNames.add(name)
                        }
                        addedCount++
                    }
                }
                // 拷贝完成后回归主线程刷新UI并保存
                runOnUiThread {
                    adapter.notifyDataSetChanged()
                    saveVideoList()
                    Toast.makeText(this, "成功导入 $addedCount 个视频！", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🌟 核心改进 3：取消原先 onCreate 里的直接重定向拦截，允许进入主界面管理视频
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.videoListView)
        addButton = findViewById(R.id.addButton)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayNames)
        listView.adapter = adapter

        addButton.text = "添加视频"
        addButton.setOnClickListener { 
            // 直接拉起系统视频选择器
            pickVideosLauncher.launch("video/*") 
        }

        // 点击列表中的任意视频开始播放
        listView.setOnItemClickListener { _, _, position, _ ->
            startPlayerActivity(position)
        }

        // 长按删除视频
        listView.setOnItemLongClickListener { _, _, position, _ ->
            if (position >= 0 && position < videoUris.size) {
                AlertDialog.Builder(this)
                    .setTitle("删除视频")
                    .setMessage("确认要从播放列表中移除该视频吗？")
                    .setPositiveButton("移除") { _, _ ->
                        // 🌟 核心改进 4：顺便删除内部存储中的物理文件，防止流氓占用手机空间
                        deleteInternalFile(videoUris[position])
                        
                        videoUris.removeAt(position)
                        displayNames.removeAt(position)
                        adapter.notifyDataSetChanged()
                        saveVideoList()
                        Toast.makeText(this, "已移除", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
            true
        }

        // 页面打开时自动加载之前保存过的视频
        loadSavedVideoList()
    }

    // 异步安全地将视频复制到App私有目录
    private fun copyVideoToInternalStorage(sourceUri: Uri, fileName: String): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(sourceUri) ?: return null
            val videosDir = File(getExternalFilesDir(null), "videos")
            if (!videosDir.exists()) {
                videosDir.mkdirs()
            }

            // 使用时间戳防止重名冲突
            val targetFile = File(videosDir, "${System.currentTimeMillis()}_$fileName")

            FileOutputStream(targetFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()

            Uri.fromFile(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 从内容提供者中解析出选中的视频真实文件名
    private fun getFileNameFromUri(uri: Uri): String {
        var name = "视频_${System.currentTimeMillis()}.mp4"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val displayName = cursor.getString(nameIndex)
                    if (!displayName.isNullOrBlank()) {
                        name = displayName
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return name
    }

    // 物理删除拷贝到内部的视频文件
    private fun deleteInternalFile(uri: Uri) {
        try {
            if (uri.scheme == "file") {
                uri.path?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 保存列表到本地
    private fun saveVideoList() {
        val prefs = getSharedPreferences("video_list", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("uris", Gson().toJson(videoUris.map { it.toString() }))
        editor.putString("names", Gson().toJson(displayNames))
        editor.apply()
    }

    // 读取本地保存的列表
    private fun loadSavedVideoList(): Boolean {
        val prefs = getSharedPreferences("video_list", MODE_PRIVATE)
        val uriJson = prefs.getString("uris", null) ?: return false
        val nameJson = prefs.getString("names", null) ?: return false

        return try {
            val uriType = object : TypeToken<List<String>>() {}.type
            val nameType = object : TypeToken<List<String>>() {}.type

            val savedUris: List<String> = Gson().fromJson(uriJson, uriType)
            val savedNames: List<String> = Gson().fromJson(nameJson, nameType)

            videoUris.clear()
            displayNames.clear()
            savedUris.forEach { videoUris.add(Uri.parse(it)) }
            displayNames.addAll(savedNames)

            adapter.notifyDataSetChanged()
            true
        } catch (e: Exception) {
            false
        }
    }

    // 唤起播放器Activity
    private fun startPlayerActivity(position: Int) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("video_uri", videoUris[position].toString())
            putExtra("current_index", position)
            putStringArrayListExtra("video_list", ArrayList(videoUris.map { it.toString() }))
        }
        startActivity(intent)
    }
}
