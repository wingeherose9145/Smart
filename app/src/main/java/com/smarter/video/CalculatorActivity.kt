package com.smarter.video

import android.content.Intent
import android.os.Bundle
import android.graphics.Typeface
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CalculatorActivity : AppCompatActivity() {

    private lateinit var display: TextView

    private var inputCount = 0

    private val secretSequence = listOf(
        "LG",
        "8",
        "COS",
        "∞",
        "Ω"
    )

    private var inputHistory = mutableListOf<String>()

    private val quotes = listOf(
        "F = ma",
        "E = mc²",
        "π ≈ 3.1416",
        "知识就是力量",
        "Stay curious",
        "量子力学",
        "熵增不灭",
        "关你屁事！",
        "G = 6.67430 × 10⁻¹¹",
        "成功是积累的结果"
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

    // ===== 上四行：短字符 =====

    "sin", "cos", "tan", "π",
    "lg", "ln", "eˣ", "%",
    "Σ", "∫", "∞", "Ω",
    "Δ", "√", "≈", "≠",

    // ===== 中四行：数字计算 =====

    "7", "8", "9", "÷",
    "4", "5", "6", "×",
    "1", "2", "3", "-",
    "0", ".", "=", "+",

    // ===== 下四行：长字符功能 =====

    "|x|", "1/x", "x²", "xʸ",
    "deg", "rad", "RND", "DEL",
    "VEC", "MAT", "OFF", "ANS",
    "lim", "d/dx", "∂/∂x", "∇f"
        )

        buttonTexts.forEachIndexed { index, text ->

            val btn = Button(this).apply {

                this.text = text

                textSize = 18f

                typeface = Typeface.DEFAULT_BOLD

                setTextColor(0xFF1E3A5F.toInt())

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

                    onButtonClick(text)
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

    private fun onButtonClick(text: String) {

        inputCount++

        inputHistory.add(text)

        if (inputCount >= 3) {

            display.text = quotes.random()

        } else {

            display.text = inputHistory.joinToString(" ")
        }

        if (inputHistory.size > 15) {

            inputHistory.removeAt(0)
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
