package cn.naughtyChild.guesture

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    var passWord = ""
    var setFirst = true
    val listener = object : LockPatternView.OnPatternListener {
        override fun onPatternStart() {
            Log.d("MainActivity", "onPatternStart: ")
        }

        override fun onPatternComplete(cells: MutableList<LockPatternView.Cell>) {
            Log.d("MainActivity", "onPatternComplete: ");
            if (setFirst) {
                cells.forEach {
                    passWord += it.index
                }
                tipTv.text = "设置成功"
                lockView.resetMode()
                setFirst = false
            } else {
                var tempPassWprd = ""
                cells.forEach {
                    tempPassWprd += it.index
                }
                if (tempPassWprd.equals(passWord)) {
                    tipTv.text = "验证成功"
                    lockView.resetMode()
                } else {
                    tipTv.text = "验证失败"
                    lockView.handleGestureError()
                    lockView.resetMode();
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lockView.setOnPatternListener(listener)
        tipTv.text = "请设置手势密码"
    }

    fun resetPassWord(view: View) {
        passWord = ""
        setFirst=true
        tipTv.text = "请设置手势密码"
    }
}