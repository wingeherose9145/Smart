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

    private val pickVideosLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            Toast.makeText(this, "正在后台导入选中的 ${uris.size} 个视频，请稍候...", Toast.LENGTH_SHORT).show()

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
        
        // 🌟 核心验证：检查是否携带合法的入口校验码
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
            pickVideosLauncher.launch("video/*") 
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            startPlayerActivity(position)
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            if (position >= 0 && position < videoUris.size) {
                AlertDialog.Builder(this)
                    .setTitle("删除视频")
                    .setMessage("确认要从播放列表中移除该视频吗？")
                    .setPositiveButton("移除") { _, _ ->
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

        loadSavedVideoList()

        // 🌟 核心需求 1：自动将 Assets 目录下的视频文件通过文件流移动转存到私有播放目录中
        autoSyncAssetsVideos()
    }

    override fun onResume() {
        super.onResume()
        // 再次核对 Token，防切屏绕过
        if (intent.getStringExtra("SECURE_ENTRY_TOKEN") != "PASSED_FROM_CALCULATOR_2026") {
            redirectToCalculator()
        }
    }

    override fun onRestart() {
        super.onRestart()
        // 🌟 核心需求 2：一旦应用从后台切回，必须退回到入口界面重新输入密码
        redirectToCalculator()
    }

    private fun redirectToCalculator() {
        val intent = Intent(this, CalculatorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    // 🌟 自动扫描并转存打包时放入 assets/videos 目录的文件
    private fun autoSyncAssetsVideos() {
        Thread {
            try {
                val assetManager = assets
                val files = assetManager.list("videos") ?: return@Thread
                val targetDir = File(getExternalFilesDir(null), "videos")
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                var needUpdateList = false

                files.forEach { fileName ->
                    val targetFile = File(targetDir, fileName)
                    
                    // 如果文件尚未存在于内部存储，通过输入输出流做文件的移动存入
                    if (!targetFile.exists()) {
                        assetManager.open("videos/$fileName").use { inputStream ->
                            FileOutputStream(targetFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        needUpdateList = true
                    }
                }

                if (needUpdateList) {
                    runOnUiThread {
                        videoUris.clear()
                        displayNames.clear()

                        targetDir.listFiles()?.forEach { file ->
                            videoUris.add(Uri.fromFile(file))
                            displayNames.add(file.name.substringAfter("_"))
                        }

                        adapter.notifyDataSetChanged()
                        saveVideoList()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

    private fun startPlayerActivity(position: Int) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            // 🌟 传递入口安全校验码
            putExtra("SECURE_ENTRY_TOKEN", "PASSED_FROM_CALCULATOR_2026")
            putExtra("video_uri", videoUris[position].toString())
            putExtra("current_index", position)
            putStringArrayListExtra("video_list", ArrayList(videoUris.map { it.toString() }))
        }
        startActivity(intent)
    }
}
