package com.smarter.video

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * 基础类：实现「无论从哪里返回都必须先进入计算器」的核心逻辑
 */
open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        redirectToCalculatorIfNeeded()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        redirectToCalculatorIfNeeded()
    }

    private fun redirectToCalculatorIfNeeded() {
        // 如果不是任务根（从桌面图标、最近任务、通知栏返回等），强制跳转到计算器
        if (!isTaskRoot) {
            startActivity(Intent(this, CalculatorActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
        }
    }
}
