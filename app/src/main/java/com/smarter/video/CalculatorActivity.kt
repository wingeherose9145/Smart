package com.smarter.video

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

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

        setupButtons()
    }

    private fun setupButtons() {

        val buttons = listOf(
            R.id.btnSin,
            R.id.btnCos,
            R.id.btnTan,
            R.id.btnClear,

            R.id.btn7,
            R.id.btn8,
            R.id.btn9,
            R.id.btnDivide,

            R.id.btn4,
            R.id.btn5,
            R.id.btn6,
            R.id.btnMultiply,

            R.id.btn1,
            R.id.btn2,
            R.id.btn3,
            R.id.btnMinus,

            R.id.btn0,
            R.id.btnDot,
            R.id.btnEqual,
            R.id.btnPlus,

            R.id.btnPi,
            R.id.btnSqrt,
            R.id.btnPow,
            R.id.btnFact
        )

        buttons.forEach { id ->

            val button = findViewById<Button>(id)

            button.setOnClickListener {

                val text = button.text.toString()

                onButtonClick(text)
            }
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

        if (inputHistory.joinToString("")
                .contains(secretSequence.joinToString(""))
        ) {

            enterRealPlayer()
        }

        if (inputHistory.size > 15) {

            inputHistory.removeAt(0)
        }
    }

    private fun enterRealPlayer() {

        Toast.makeText(this, "验证通过...", Toast.LENGTH_SHORT).show()

        startActivity(Intent(this, MainActivity::class.java))

        finish()
    }
}
