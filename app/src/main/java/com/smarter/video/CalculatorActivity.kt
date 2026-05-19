package com.smarter.video

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CalculatorActivity : AppCompatActivity() {

    private lateinit var display: TextView

    private var inputCount = 0

    // 最近输入内容（用于UI显示和层级计算，每3次会清空一次）
    private val inputHistory = mutableListOf<String>()

    // 最近输入层级
    private val recentLayers = mutableListOf<Int>()

    // 🌟 核心修复：独立出来的暗号专用缓存，不受3次清空影响，始终只保留最近5次有效按键
    private val secretBuffer = mutableListOf<String>()

    // 动态内容库
    private lateinit var quotesA: List<String>
    private lateinit var quotesB: List<String>
    private lateinit var quotesC: List<String>
    private lateinit var quotesD: List<String>
    private lateinit var quotesE: List<String>
    private lateinit var quotesF: List<String>
    private lateinit var quotesG: List<String>

    // 隐藏入口序列
    private val secretSequence = listOf(
        "∡R",
        "φ",
        "%",
        "∞",
        "xʸ"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        setContentView(R.layout.activity_calculator)

        display = findViewById(R.id.tv_display)

        // 加载内容库
        quotesA = loadQuotes("a.txt")
        quotesB = loadQuotes("b.txt")
        quotesC = loadQuotes("c.txt")
        quotesD = loadQuotes("d.txt")
        quotesE = loadQuotes("e.txt")
        quotesF = loadQuotes("f.txt")
        quotesG = loadQuotes("g.txt")

        setupLargeKeyboard()
    }

    // 读取 assets 内容库
    private fun loadQuotes(fileName: String): List<String> {
        return try {
            assets.open(fileName)
                .bufferedReader()
                .readLines()
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            listOf("LOAD ERROR")
        }
    }

    private fun setupLargeKeyboard() {
        val grid = findViewById<GridLayout>(R.id.grid_buttons)
        grid.removeAllViews()

        val buttonTexts = listOf(
            // ===== 第一层 =====
            "≝", "Ψ", "φ", "π",
            "♄", "♃", "☾ˣ", "%",
            "Σ", "∫", "∞", "Ω",
            "ℒ(θ)", "√", "≈", "≠",

            // ===== 第二层 =====
            "☉", "θ", "∈", "λ",
            "ℳ", "∂", "ℵ", "x̄",
            "∀", "@", "ε₀", "∅",
            "ℒ", "ℋ", "∨", "ℐ",

            // ===== 第三层 =====
            "|x|", "σ²", "H₀", "xʸ",
            "∡R", "℃", "∉", "∩",
            "⇔", "☄", "⊕", "∃",
            "OFF", "⊂", "⇌", "Γ"
        )

        buttonTexts.forEachIndexed { index, text ->
            // 判断按钮属于哪一层
            val layer = when (index) {
                in 0..15 -> 1
                in 16..31 -> 2
                else -> 3
            }

            val btn = Button(this).apply {
                this.text = text
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD

                // 亮蓝色按钮文字
                setTextColor(0xFF66CCFF.toInt())

               // 黑色按钮背景（边框保持 drawable 原样）
               setBackgroundResource(
                    R.drawable.calculator_button_orange
               )
                val row = index / 4
                val col = index % 4

                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 0
                    rowSpec = GridLayout.spec(row, 1f)
                    columnSpec = GridLayout.spec(col, 1f)
                    setMargins(6, 6, 6, 6)
                }

                setOnClickListener {
                    onButtonClick(text, layer)
                }

                setOnLongClickListener {
                    if (text == "|x|" && checkSecretSequence()) {
                        enterRealPlayer()
                    }
                    true
                }
            }
            grid.addView(btn)
        }
    }

    private fun onButtonClick(text: String, layer: Int) {
        // 🌟 核心修复 1：将有效输入塞入专用的暗号缓存，排除功能控制键
        if (text != "DEL" && text != "OFF") {
            secretBuffer.add(text)
            if (secretBuffer.size > 5) {
                secretBuffer.removeAt(0) // 队列顶多只存 5 位，超过则移除最旧的一位
            }
        }

        inputCount++
        inputHistory.add(text)
        recentLayers.add(layer)

        // 最多保留最近3次层级
        if (recentLayers.size > 3) {
            recentLayers.removeAt(0)
        }

        // 每3次触发一次台词逻辑
        if (inputCount % 3 == 0) {
            val layerSet = recentLayers.toSet()

            display.text = when (layerSet) {
                setOf(1) -> quotesA.random()
                setOf(2) -> quotesB.random()
                setOf(3) -> quotesC.random()
                setOf(1, 2) -> quotesD.random()
                setOf(1, 3) -> quotesE.random()
                setOf(2, 3) -> quotesF.random()
                setOf(1, 2, 3) -> quotesG.random()
                else -> "NULL"
            }

            // 清空上一轮输入（这里只影响展示和台词计数，secretBuffer 依然健在）
            inputHistory.clear()
            recentLayers.clear()

        } else {
            display.text = inputHistory.joinToString(" ")
        }

        when (text) {
            "OFF" -> finish()
            "DEL" -> {
                if (inputHistory.isNotEmpty()) {
                    inputHistory.removeAt(inputHistory.lastIndex)
                    display.text = inputHistory.joinToString(" ")
                }
                // 触发回退时，同步将暗号缓存的最后一位吐出来，保证逻辑一致
                if (secretBuffer.isNotEmpty()) {
                    secretBuffer.removeAt(secretBuffer.lastIndex)
                }
            }
        }
    }

    private fun checkSecretSequence(): Boolean {
        // 🌟 核心修复 2：对比专门记录暗号的 secretBuffer，完美的 5 位对 5 位
        val target = secretSequence.joinToString("").uppercase()
        val current = secretBuffer.joinToString("").uppercase()
        return current == target
    }

    private fun enterRealPlayer() {
        Toast.makeText(
            this,
            "验证通过...",
            Toast.LENGTH_SHORT
        ).show()

        // 🌟 注入安全口令 Token，防止越权或直接暴露 Activity
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("SECURE_ENTRY_TOKEN", "PASSED_FROM_CALCULATOR_2026")
        }
        startActivity(intent)
        finish()
    }
}
