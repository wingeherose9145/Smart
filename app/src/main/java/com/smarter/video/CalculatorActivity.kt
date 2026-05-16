package com.smarter.video

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class CalculatorActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private var inputCount = 0
    private val secretSequence = listOf("15", "COS", "9", "inv")

    private var inputHistory = mutableListOf<String>()

    private val quotes = listOf(
        "F = ma",
        "E = mc²",
        "π ≈ 3.1416",
        "知识就是力量",
        "Stay curious",
        "量子力学",
        "熵增不灭",
        "The universe is under no obligation to make sense to you",
        "G = 6.67430 × 10⁻¹¹",
        "成功是积累的结果"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        display = findViewById(R.id.tv_display)
        setupLargeKeyboard()
    }

    private fun setupLargeKeyboard() {
        val grid = findViewById<GridLayout>(R.id.grid_buttons)

        val buttonTexts = listOf(
            "sin","cos","tan","log","15","Inv","9","inv",
            "7","8","9","÷","4","5","6","×",
            "1","2","3","−","0",".","AC","+",
            "π","e","√","%","Δ","∑","∫","∞",
            "kg","m","s","N","J","W","V","A",
            "Hz","Ω","F","H","Pa","mol","cd","K"
        )

        buttonTexts.forEach { text ->
            val btn = Button(this).apply {
                this.text = text
                textSize = 18f
                setBackgroundResource(R.drawable.calculator_button_background)
                setTextColor(0xFFFFFFFF.toInt())
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 78
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }
                setOnClickListener { onButtonClick(text) }
            }
            grid.addView(btn)
        }
    }

    private fun onButtonClick(text: String) {
        inputCount++
        inputHistory.add(text)

        // 输入3个以上后，只显示随机名言
        if (inputCount >= 3) {
            display.text = quotes.random()
        } else {
            display.text = inputHistory.joinToString(" ")
        }

        // 检查密码序列
        if (inputHistory.joinToString("").contains(secretSequence.joinToString(""))) {
            enterRealPlayer()
        }

        // 限制历史长度
        if (inputHistory.size > 15) inputHistory.removeAt(0)
    }

    private fun enterRealPlayer() {
        Toast.makeText(this, "验证通过...", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
