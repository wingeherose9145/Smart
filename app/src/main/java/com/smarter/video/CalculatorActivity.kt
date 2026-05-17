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

    // 隐藏入口序列
    private val secretSequence = listOf(
        "LG",
        "8",
        "COS",
        "∞",
        "Ω"
    )

    // ===== A 内容 =====
    private val quotesA = listOf(
        "基础函数",
        "sin(x)",
        "cos(x)",
        "π ≈ 3.1416"
    )

    // ===== B 内容 =====
    private val quotesB = listOf(
        "数字计算",
        "E = mc²",
        "7 × 8 = 56",
        "普通运算"
    )

    // ===== C 内容 =====
    private val quotesC = listOf(
        "高级模式",
        "矩阵运算",
        "向量分析",
        "∂/∂x"
    )

    // ===== D 内容 =====
    private val quotesD = listOf(
        "函数+数字",
        "混合运算",
        "工程模式",
        "Hybrid A"
    )

    // ===== E 内容 =====
    private val quotesE = listOf(
        "符号+高级",
        "隐藏模式",
        "分析系统",
        "Hybrid B"
    )

    // ===== F 内容 =====
    private val quotesF = listOf(
        "数字+高级",
        "高级计算",
        "实验功能",
        "Hybrid C"
    )

    // ===== G 内容 =====
    private val quotesG = listOf(
        "完全模式",
        "ALL SYSTEM",
        "科学核心",
        "Ultimate"
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

        setupLargeKeyboard()
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

                // 按钮文字改黑色
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
