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
    private val secretCode = "123456"

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

        findViewById<Button>(R.id.btn_equals).setOnClickListener {
            checkSecretCode()
        }
    }

    private fun setupNumberButtons() {
        val numbers = listOf(R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9)

        numbers.forEach { id ->
            findViewById<Button>(id).setOnClickListener { btn ->
                currentInput += (btn as Button).text
                display.text = currentInput.ifEmpty { "0" }
            }
        }
    }

    private fun setupOperatorButtons() {
        val operators = listOf(
            R.id.btn_plus to "+",
            R.id.btn_minus to "−",
            R.id.btn_mul to "×",
            R.id.btn_div to "÷",
            R.id.btn_dot to ".",
            R.id.btn_plus_minus to "±",
            R.id.btn_percent to "%"
        )

        operators.forEach { (id, symbol) ->
            findViewById<Button>(id).setOnClickListener {
                if (currentInput.isNotEmpty() && currentInput != "0") {
                    currentInput += symbol
                    display.text = currentInput
                } else {
                    Toast.makeText(this, "请先输入数字", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkSecretCode() {
        if (currentInput == secretCode) {
            enterRealPlayer()
        } else {
            // 普通计算器行为
            if (currentInput.isNotEmpty()) {
                Toast.makeText(this, "计算结果: $currentInput", Toast.LENGTH_SHORT).show()
            }
            currentInput = ""
            display.text = "0"
        }
    }

    private fun enterRealPlayer() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}
