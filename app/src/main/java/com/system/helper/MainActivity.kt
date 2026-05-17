package com.smarter.video

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            loadAllVideos()
        } else {
            Toast.makeText(this, "需要权限才能读取视频", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasSavedVideoList()) {
            startRandomPlayback()
            return
        }

        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.videoListView)
        addButton = findViewById(R.id.addButton)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayNames)
        listView.adapter = adapter

        addButton.text = "添加视频"
        addButton.setOnClickListener { addVideos() }

        listView.setOnItemClickListener { _, _, position, _ ->
            startPlayerActivity(position)
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            if (position >= 0 && position < videoUris.size) {
                AlertDialog.Builder(this)
                    .setTitle("删除")
                    .setMessage("从列表中移除？")
                    .setPositiveButton("移除") { _, _ ->
                        videoUris.removeAt(position)
                        displayNames.removeAt(position)
                        adapter.notifyDataSetChanged()
                        saveVideoList()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
            true
        }

        loadSavedListOrRefresh()
    }

    private fun addVideos() {
        if (hasPermission()) {
            loadAllVideos()
        } else {
            requestPermission()
        }
    }

    private fun loadAllVideos() {
        videoUris.clear()
        displayNames.clear()

        val projection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME)

        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "未知视频"
                val sourceUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())

                // 关键：复制到 App 内部存储
                val internalUri = copyVideoToInternalStorage(sourceUri, name)

                if (internalUri != null) {
                    videoUris.add(internalUri)
                    displayNames.add(name)
                }
            }
        }

        adapter.notifyDataSetChanged()
        saveVideoList()

        Toast.makeText(this, "已添加 ${displayNames.size} 个视频（已复制到内部）", Toast.LENGTH_SHORT).show()
    }

    // 复制视频到 App 内部私有目录
    private fun copyVideoToInternalStorage(sourceUri: Uri, fileName: String): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(sourceUri) ?: return null
            
            val internalDir = getExternalFilesDir(null) ?: filesDir
            val targetFile = File(internalDir, "videos/${System.currentTimeMillis()}_$fileName")
            targetFile.parentFile?.mkdirs()

            FileOutputStream(targetFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()

            Uri.fromFile(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "复制失败: $fileName", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // 其他函数保持不变（hasPermission、loadSavedListOrRefresh、saveVideoList 等）
    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestPermission() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestPermission.launch(perms)
    }

    private fun loadSavedListOrRefresh() {
        if (loadSavedVideoList()) {
            Toast.makeText(this, "已恢复 ${displayNames.size} 个视频", Toast.LENGTH_SHORT).show()
        } else if (hasPermission()) {
            loadAllVideos()
        } else {
            requestPermission()
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

        try {
            val uriList: List<String> = Gson().fromJson(uriJson, object : TypeToken<List<String>>() {}.type)
            val nameList: List<String> = Gson().fromJson(nameJson, object : TypeToken<List<String>>() {}.type)

            videoUris.clear()
            displayNames.clear()
            uriList.forEach { videoUris.add(Uri.parse(it)) }
            displayNames.addAll(nameList)

            adapter.notifyDataSetChanged()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun startPlayerActivity(position: Int) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("video_uri", videoUris[position].toString())
            putExtra("current_index", position)
            putStringArrayListExtra("video_list", ArrayList(videoUris.map { it.toString() }))
        }
        startActivity(intent)
    }

    private fun hasSavedVideoList(): Boolean {
        val prefs = getSharedPreferences("video_list", MODE_PRIVATE)
        return prefs.contains("uris")
    }

    private fun startRandomPlayback() {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putStringArrayListExtra("video_list", ArrayList(loadSavedUris()))
        startActivity(intent)
        finish()
    }

    private fun loadSavedUris(): List<String> {
        val prefs = getSharedPreferences("video_list", MODE_PRIVATE)
        val json = prefs.getString("uris", null) ?: return emptyList()
        return try {
            Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
