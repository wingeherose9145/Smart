package com.smarter.video

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * 安全基础类
 * 只负责检查是否已经通过计算器验证
 * 不再使用危险的 isTaskRoot 强制跳转
 */
open class BaseActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()

        // 如果没有验证，则返回计算器
        if (!PasswordManager.isVerified(this)
            && this !is CalculatorActivity
        ) {

            val intent = Intent(this, CalculatorActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK

            startActivity(intent)
            finish()
        }
    }
}
