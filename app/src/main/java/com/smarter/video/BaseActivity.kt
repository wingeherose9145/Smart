package com.smarter.video

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// 全局静态计数器，用于追踪当前处于前台的 Activity 数量
private var foregroundActivityCount = 0

/**
 * 基础类：实现「无论从哪里返回都必须先进入计算器」的核心逻辑
 */
open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 刚创建时检查是否需要去计算器解锁
        redirectToCalculatorIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        // 从后台切回或者从其他页面返回时，再次检查锁定状态
        redirectToCalculatorIfNeeded()
    }

    override fun onStart() {
        super.onStart()
        // 页面进入前台，计数加 1
        foregroundActivityCount++
    }

    override fun onStop() {
        super.onStop()
        // 页面移出前台，计数减 1
        foregroundActivityCount--
        
        // 当计数归零，说明整个 App 没有任何界面在前端了（即 App 退到了后台或被挂起）
        if (foregroundActivityCount <= 0) {
            foregroundActivityCount = 0
            // 核心安全机制：退到后台立刻自动锁定！
            AuthManager.isUnlocked = false
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        redirectToCalculatorIfNeeded()
    }

    private fun redirectToCalculatorIfNeeded() {
        // 如果未解锁，且当前页面不是计算器本身，则强制拦截并跳转到计算器
        if (!AuthManager.isUnlocked && this !is CalculatorActivity) {
            startActivity(Intent(this, CalculatorActivity::class.java).apply {
                // 确保计算器处于栈顶，并清除它上方的所有其他 Activity 隐私界面
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
            finish()
        }
    }
}
