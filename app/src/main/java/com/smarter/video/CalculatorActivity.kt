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

    // 最近输入内容
    private val inputHistory = mutableListOf<String>()

    // 最近输入层级
    private val recentLayers = mutableListOf<Int>()

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
        "LG",
        "8",
        "COS",
        "∞",
        "Ω"
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

            "sin", "cos", "tan", "π",
            "lg", "ln", "eˣ", "%",
            "Σ", "∫", "∞", "Ω",
            "Δ", "√", "≈", "≠",

            // ===== 第二层 =====

            "7", "8", "9", "÷",
            "4", "5", "6", "×",
            "1", "2", "3", "-",
            "0", ".", "=", "+",

            // ===== 第三层 =====

            "|x|", "1/x", "x²", "xʸ",
            "deg", "rad", "RND", "DEL",
            "VEC", "MAT", "OFF", "ANS",
            "lim", "d/dx", "∂/∂x", "∇f"
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

                // 黑色按钮文字
                setTextColor(0xFF000000.toInt())

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

                    if (
                        text == "|x|" &&
                        checkSecretSequence()
                    ) {

                        enterRealPlayer()
                    }

                    true
                }
            }

            grid.addView(btn)
        }
    }

    private fun onButtonClick(
        text: String,
        layer: Int
    ) {

        inputCount++

        inputHistory.add(text)

        recentLayers.add(layer)

        // 最多保留最近3次层级
        if (recentLayers.size > 3) {
            recentLayers.removeAt(0)
        }

        // 最多保留最近15次输入
        if (inputHistory.size > 15) {
            inputHistory.removeAt(0)
        }

        // 每3次触发一次
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

        } else {

            display.text =
                inputHistory.joinToString(" ")
        }

        when (text) {

            "OFF" -> finish()

            "DEL" -> {

                if (inputHistory.isNotEmpty()) {

                    inputHistory.removeAt(
                        inputHistory.lastIndex
                    )

                    display.text =
                        inputHistory.joinToString(" ")
                }
            }
        }
    }

    private fun checkSecretSequence(): Boolean {

        val target =
            secretSequence.joinToString("").uppercase()

        val current =
            inputHistory.joinToString("").uppercase()

        return current.contains(target)
    }

    private fun enterRealPlayer() {

        Toast.makeText(
            this,
            "验证通过...",
            Toast.LENGTH_SHORT
        ).show()

        startActivity(
            Intent(
                this,
                MainActivity::class.java
            )
        )

        finish()
    }
}
