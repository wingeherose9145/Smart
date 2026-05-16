package com.smarter.video

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CalculatorActivity : AppCompatActivity() {

    private lateinit var display: TextView

    private var inputCount = 0

    private val secretSequence = listOf("7", "COS", "9", "π")

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

        setContentView(R.layout.activity_calculator)

        display = findViewById(R.id.tv_display)

        setupLargeKeyboard()
    }

    private fun setupLargeKeyboard() {

        val grid = findViewById<GridLayout>(R.id.grid_buttons)

        grid.removeAllViews()

        val buttonTexts = listOf(

            "sin", "cos", "tan", "π",

            "lg", "ln", "eˣ", "%",

            "7", "8", "9", "÷",

            "4", "5", "6", "×",

            "1", "2", "3", "-",

            "0", ".", "=", "+",

            "deg", "rad", "|x|", "1/x",

            "Σ", "∫", "∞", "DEL",

            "kg", "mol", "A", "K",

            "Hz", "Ω", "F", "H",

            "VEC", "MAT", "RND", "OFF"

        )

        buttonTexts.forEachIndexed { index, text ->

            val btn = Button(this)

            btn.text = text

            btn.textSize = 13f

            btn.setTextColor(0xFFFFFFFF.toInt())

            if (
                text == "+" ||
                text == "-" ||
                text == "×" ||
                text == "÷" ||
                text == "=" ||
                text == "OFF" ||
                text == "Σ" ||
                text == "∫"
            ) {

                btn.setBackgroundResource(
                    R.drawable.calculator_button_orange
                )

            } else {

                btn.setBackgroundResource(
                    R.drawable.calculator_button_dark
                )
            }

            val row = index / 4

            val col = index % 4

            val params = GridLayout.LayoutParams()

            params.width = 0

            params.height = 0

            params.columnSpec = GridLayout.spec(col, 1f)

            params.rowSpec = GridLayout.spec(row, 1f)

            params.setMargins(4, 4, 4, 4)

            btn.layoutParams = params

            btn.setOnClickListener {

                onButtonClick(text)
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

        val secretInput =
            inputHistory.joinToString("").uppercase()

        val target =
            secretSequence.joinToString("").uppercase()

        if (secretInput.contains(target)) {

            enterRealPlayer()
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
