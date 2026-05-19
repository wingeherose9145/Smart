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

    // 🌟 修复关键点 1：增加一个状态锁，用于标识当前是否正在调用系统选择器去选取视频
    private var isPickingFile = false

    private val pickVideosLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        // 🌟 选完文件返回了，恢复标记
        isPickingFile = false
        
        if (uris.isNotEmpty()) {
            Toast.makeText(this, "正在后台导入选中的 ${uris.size} 个视频，请稍候...", Toast.LENGTH_SHORT).show()

            Thread {
                var addedCount = 0
                uris.forEach { sourceUri ->
                    val name = getFileNameFromUri(sourceUri)
                    val internalUri = copyVideoToInternalStorage(sourceUri, name)
                    if (internalUri != null) {
                        addedCount++
                    }
                }
                
                // 🌟 修复关键点 2：文件拷贝完成后，直接通过重新扫描整个物理文件夹来刷新列表
                runOnUiThread {
                    reloadAllLocalVideos()
                    Toast.makeText(this, "成功导入 $addedCount 个视频！", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (intent.getStringExtra("SECURE_ENTRY_TOKEN") != "PASSED_FROM_CALCULATOR_2026") {
            redirectToCalculator()
            return
        }

        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.videoListView)
        addButton = findViewById(R.id.addButton)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayNames)
        listView.adapter = adapter

        addButton.text = "添加视频"
        addButton.setOnClickListener { 
            // 🌟 在拉起选择器前上锁，告诉生命周期拦截器：“这是正常换页，不要踢我出去”
            isPickingFile = true
            pickVideosLauncher.launch("video/*") 
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            startPlayerActivity(position)
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            if (position >= 0 && position < videoUris.size) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("删除视频")
                    .setMessage("确认要从播放列表中移除该视频吗？")
                    .setPositiveButton("移除") { _, _ ->
                        deleteInternalFile(videoUris[position])
                        reloadAllLocalVideos()
                        Toast.makeText(this, "已移除", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
            true
        }

        // 先做资产文件的静默移动，移动完成后统一刷新显示
        autoSyncAssetsVideos()
    }

    override fun onResume() {
        super.onResume()
        // 🌟 修复关键点 3：如果是因为选视频进入后台又返回的，直接放行，不触发踢出逻辑
        if (isPickingFile) {
            return
        }
        
        if (intent.getStringExtra("SECURE_ENTRY_TOKEN") != "PASSED_FROM_CALCULATOR_2026") {
            redirectToCalculator()
        }
    }

    override fun onRestart() {
        super.onRestart()
        // 🌟 修复关键点 4：同样，如果是选视频触发的重启，不踢回入口
        if (isPickingFile) {
            return
        }
        redirectToCalculator()
    }

    private fun redirectToCalculator() {
        val intent = Intent(this, CalculatorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    // 🌟 统一数据源：直接扫描私有存储下的所有视频（包含手动添加的和GitHub Actions同步进去的），并刷新UI
    private fun reloadAllLocalVideos() {
        val targetDir = File(getExternalFilesDir(null), "videos")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        videoUris.clear()
        displayNames.clear()

        targetDir.listFiles()?.forEach { file ->
            if (file.isFile && file.length() > 0) {
                videoUris.add(Uri.fromFile(file))
                // 优雅过滤掉时间戳前缀，显示正常文件名
                val cleanName = file.name.substringAfter("_")
                displayNames.add(cleanName)
            }
        }
        adapter.notifyDataSetChanged()
        saveVideoList()
    }

    // 静默把 Assets 里的视频通过流移动方式存入私有目录
    private fun autoSyncAssetsVideos() {
        Thread {
            try {
                val assetManager = assets
                val files = assetManager.list("videos") ?: return@Thread
                val targetDir = File(getExternalFilesDir(null), "videos")
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                var hasNewFiles = false

                files.forEach { fileName ->
                    val targetFile = File(targetDir, fileName)
                    if (!targetFile.exists()) {
                        assetManager.open("videos/$fileName").use { inputStream ->
                            FileOutputStream(targetFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        hasNewFiles = true
                    }
                }

                // 不管有没有新移入的资源，首次进入都在主线程统一加载一次完整的物理目录
                runOnUiThread {
                    reloadAllLocalVideos()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    reloadAllLocalVideos()
                }
            }
        }.start()
    }

    private fun copyVideoToInternalStorage(sourceUri: Uri, fileName: String): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(sourceUri) ?: return null
            val videosDir = File(getExternalFilesDir(null), "videos")
            if (!videosDir.exists()) {
                videosDir.mkdirs()
            }

            // 使用时间戳前缀存入，防止重名
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

    private fun saveVideoList() {
        val prefs = getSharedPreferences("video_list", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("uris", Gson().toJson(videoUris.map { it.toString() }))
        editor.putString("names", Gson().toJson(displayNames))
        editor.apply()
    }

    private fun startPlayerActivity(position: Int) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("SECURE_ENTRY_TOKEN", "PASSED_FROM_CALCULATOR_2026")
            putExtra("video_uri", videoUris[position].toString())
            putExtra("current_index", position)
            putStringArrayListExtra("video_list", ArrayList(videoUris.map { it.toString() }))
        }
        startActivity(intent)
    }
}
