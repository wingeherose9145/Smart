package com.smarter.video

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smarter.video.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream

private const val PREF_NAME = "video_list"
private const val KEY_URIS = "uris"
private const val KEY_NAMES = "names"

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private val videoUris = mutableListOf<Uri>()
    private val displayNames = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    private val pickVideosLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            importVideos(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListView()

        // 启动时刷新
        refreshVideoList()

        binding.addButton.setOnClickListener {
            pickVideosLauncher.launch("video/*")
        }
    }

    private fun setupListView() {

        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            displayNames
        )

        binding.videoListView.adapter = adapter

        binding.videoListView.setOnItemClickListener { _, _, position, _ ->
            if (position in videoUris.indices) {
                startPlayerActivity(position)
            }
        }

        binding.videoListView.setOnItemLongClickListener { _, _, position, _ ->

            if (position in videoUris.indices) {
                showDeleteDialog(position)
            }

            true
        }
    }

    /**
     * 刷新视频列表
     */
    private fun refreshVideoList() {

        videoUris.clear()
        displayNames.clear()

        loadSavedVideoList()

        scanInternalVideosFolder()

        val combined = videoUris.zip(displayNames)
            .distinctBy { it.first.path }
            .sortedBy { it.second.lowercase() }

        videoUris.clear()
        displayNames.clear()

        combined.forEach { (uri, name) ->
            videoUris.add(uri)
            displayNames.add(name)
        }

        adapter.notifyDataSetChanged()

        saveVideoList()
    }

    /**
     * 扫描内部视频目录
     */
    private fun scanInternalVideosFolder() {

        val videosDir = File(getExternalFilesDir(null), "videos")

        if (!videosDir.exists() || !videosDir.isDirectory) {
            return
        }

        val existingPaths =
            videoUris.mapNotNull { it.path }.toMutableSet()

        videosDir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.forEach { file ->

                if (file.isFile && isVideoFile(file)) {

                    val fileUri = Uri.fromFile(file)
                    val filePath = fileUri.path

                    if (filePath != null && filePath !in existingPaths) {

                        videoUris.add(fileUri)
                        displayNames.add(file.name)

                        existingPaths.add(filePath)
                    }
                }
            }
    }

    private fun isVideoFile(file: File): Boolean {

        val ext = file.extension.lowercase()

        return ext in listOf(
            "mp4",
            "mkv",
            "mov",
            "avi",
            "wmv",
            "flv",
            "webm",
            "3gp",
            "m4v",
            "ts"
        )
    }

    /**
     * 导入视频
     */
    private fun importVideos(uris: List<Uri>) {

        Thread {

            var added = 0

            val videosDir =
                File(getExternalFilesDir(null), "videos")
                    .apply { mkdirs() }

            uris.forEach { sourceUri ->

                val name = getFileNameFromUri(sourceUri)

                val targetFile = File(
                    videosDir,
                    "${System.currentTimeMillis()}_$name"
                )

                try {

                    contentResolver.openInputStream(sourceUri)
                        ?.use { input ->

                            FileOutputStream(targetFile)
                                .use { output ->
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

                Toast.makeText(
                    this,
                    "成功导入 $added 个视频",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }.start()
    }

    private fun getFileNameFromUri(uri: Uri): String {

        var name = "video_${System.currentTimeMillis()}.mp4"

        try {

            contentResolver.query(
                uri,
                null,
                null,
                null,
                null
            )?.use { cursor ->

                val index =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                if (index != -1 && cursor.moveToFirst()) {

                    cursor.getString(index)?.let {

                        if (it.isNotBlank()) {
                            name = it
                        }
                    }
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

        Toast.makeText(
            this,
            "已删除",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showDeleteDialog(position: Int) {

        AlertDialog.Builder(this)
            .setTitle("删除视频")
            .setMessage("确认永久删除？")
            .setPositiveButton("删除") { _, _ ->
                deleteVideo(position)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveVideoList() {

        val prefs =
            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .edit()

        prefs.putString(
            KEY_URIS,
            Gson().toJson(videoUris.map { it.toString() })
        )

        prefs.putString(
            KEY_NAMES,
            Gson().toJson(displayNames)
        )

        prefs.apply()
    }

    private fun loadSavedVideoList() {

        val prefs =
            getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        val uriJson =
            prefs.getString(KEY_URIS, null) ?: return

        val nameJson =
            prefs.getString(KEY_NAMES, null) ?: return

        try {

            val uris: List<String> =
                Gson().fromJson(
                    uriJson,
                    object : TypeToken<List<String>>() {}.type
                )

            val names: List<String> =
                Gson().fromJson(
                    nameJson,
                    object : TypeToken<List<String>>() {}.type
                )

            uris.forEach {
                videoUris.add(Uri.parse(it))
            }

            displayNames.addAll(names)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPlayerActivity(position: Int) {

        val intent =
            Intent(this, PlayerActivity::class.java).apply {

                putStringArrayListExtra(
                    "video_list",
                    ArrayList(videoUris.map { it.toString() })
                )

                putExtra("current_index", position)
            }

        startActivity(intent)
    }
}
