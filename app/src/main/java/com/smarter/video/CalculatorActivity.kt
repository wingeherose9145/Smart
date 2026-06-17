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

    // 核心修复：独立出来的暗号专用缓存，不受3次清空影响，始终只保留最近5次有效按键
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
            "ℒ", "√", "≈", "≠",

            // ===== 第二层 =====
            "☉", "θ", "∈", "λ",
            "~", "∂", "ℵ", "x̄",
            "∀", "@", "ε₀", "∅",
            "S", "M", "∨", "ℐ",

            // ===== 第三层 =====
            "|x|", "σ²", "H₀", "xʸ",
            "∡R", "℃", "∉", "∩",
            "⇔", "☄", "⊕", "∃",
            "OFF", "⊂", "⇌", "Γ"
        )

        buttonTexts.forEachIndexed { index, text ->
            val layer = when (index) {
                in 0..15 -> 1
                in 16..31 -> 2
                else -> 3
            }

            val btn = Button(this).apply {
                this.text = text
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(0xFF66CCFF.toInt())
                setBackgroundResource(R.drawable.calculator_button_orange)
                
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
        if (text != "DEL" && text != "OFF") {
            secretBuffer.add(text)
            if (secretBuffer.size > 5) {
                secretBuffer.removeAt(0)
            }
        }

        inputCount++
        inputHistory.add(text)
        recentLayers.add(layer)

        if (recentLayers.size > 3) {
            recentLayers.removeAt(0)
        }

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
                if (secretBuffer.isNotEmpty()) {
                    secretBuffer.removeAt(secretBuffer.lastIndex)
                }
            }
        }
    }

    private fun checkSecretSequence(): Boolean {
        val target = secretSequence.joinToString("").uppercase()
        val current = secretBuffer.joinToString("").uppercase()
        return current == target
    }

    private fun enterRealPlayer() {
        Toast.makeText(this, "验证通过...", Toast.LENGTH_SHORT).show()

        // 🌟 核心改动：注入安全口令校验码，只有通过该流转才能进入主界面
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("SECURE_ENTRY_TOKEN", "PASSED_FROM_CALCULATOR_2026")
        }
        startActivity(intent)
        finish()
    }
}
