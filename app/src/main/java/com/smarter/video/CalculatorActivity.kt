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
    private var currentInput = ""
    private val passwordSequence = listOf("15", "COS", "9", "inv")  // 密码序列，可自行修改

    private var inputHistory = mutableListOf<String>()

    private val quotes = listOf(
        "知识就是力量",
        "The only way to do great work is to love what you do",
        "E = mc²",
        "π ≈ 3.1415926",
        "F = ma",
        "成功源于坚持",
        "V = IR",
        "Stay curious",
        "量子纠缠",
        "熵增原理"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        display = findViewById(R.id.tv_display)

        setupLargeKeyboard()
    }

    private fun setupLargeKeyboard() {
        val grid = findViewById<GridLayout>(R.id.grid_buttons)
        grid.columnCount = 4

        // 48个按钮内容（可自行调整）
        val buttons = listOf(
            "sin", "cos", "tan", "log",
            "15", "Inv", "9", "inv",
            "7", "8", "9", "÷",
            "4", "5", "6", "×",
            "1", "2", "3", "−",
            "0", ".", "=", "+",
            "π", "e", "√", "%",
            "Δ", "∑", "∫", "∞",
            "kg", "m", "s", "N",
            "J", "W", "V", "A",
            "Hz", "Ω", "F", "H",
            "Pa", "mol", "cd", "K"
        )

        buttons.forEach { text ->
            val button = Button(this).apply {
                this.text = text
                textSize = 16f
                setBackgroundResource(R.drawable.calculator_button_background)
                setTextColor(0xFFFFFFFF.toInt())
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 85
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(4, 4, 4, 4)
                }
                setOnClickListener {
                    onButtonClicked(text)
                }
            }
            grid.addView(button)
        }
    }

    private fun onButtonClicked(text: String) {
        currentInput += text
        display.text = currentInput

        inputHistory.add(text)

        // 显示随机名言
        if (Random.nextInt(3) == 0) {
            display.append("\n\n" + quotes.random())
        }

        // 检查密码序列（连续输入）
        checkPasswordSequence()
    }

    private fun checkPasswordSequence() {
        val historyStr = inputHistory.joinToString("")
        if (historyStr.contains(passwordSequence.joinToString(""))) {
            enterRealPlayer()
        }

        // 保持历史记录在合理长度
        if (inputHistory.size > 20) {
            inputHistory.removeAt(0)
        }
    }

    private fun enterRealPlayer() {
        Toast.makeText(this, "验证通过，正在进入...", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // 进入后关闭伪装界面
    }
}
