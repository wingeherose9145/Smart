package com.smarter.video

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CalculatorActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private var currentInput = ""
    private val secretCode = "15COS9inv"   // 新密码

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        display = findViewById(R.id.tv_display)

        setupNumberButtons()
        setupOperatorButtons()

        findViewById<Button>(R.id.btn_clear).setOnClickListener {
            currentInput = ""
            display.text = "0"
        }

        // 不再使用 = 按钮触发，改为实时检测
    }

    private fun setupNumberButtons() {
        val numbers = listOf(
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3,
            R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7,
            R.id.btn_8, R.id.btn_9
        )

        numbers.forEach { id ->
            findViewById<Button>(id).setOnClickListener { btn ->
                currentInput += (btn as Button).text.toString()
                display.text = currentInput
                checkIfPasswordEntered()
            }
        }
    }

    private fun setupOperatorButtons() {
        val operators = mapOf(
            R.id.btn_plus to "+", 
            R.id.btn_minus to "−",
            R.id.btn_mul to "×", 
            R.id.btn_div to "÷",
            R.id.btn_dot to "."
        )

        operators.forEach { (id, symbol) ->
            findViewById<Button>(id).setOnClickListener {
                if (currentInput.isNotEmpty()) {
                    currentInput += symbol
                    display.text = currentInput
                }
            }
        }

        // 科学按钮仅显示效果
        listOf(R.id.btn_inv, R.id.btn_sin, R.id.btn_cos).forEach { id ->
            findViewById<Button>(id)?.setOnClickListener {
                Toast.makeText(this, "功能开发中...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 实时检测密码
    private fun checkIfPasswordEntered() {
        if (currentInput.contains(secretCode)) {
            enterRealPlayer()
        }
    }

    private fun enterRealPlayer() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        currentInput = ""   // 重置
        display.text = "0"
    }
}
