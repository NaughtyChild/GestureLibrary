package cn.naughtyChild.guesture

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val listener = object : LockPatternView.OnPatternListener {
        override fun onPatternStart() {
            Log.d("MainActivity", "onPatternStart: ")
        }

        override fun onPatternComplete(cells: MutableList<LockPatternView.Cell>?) {
            Log.d("MainActivity", "onPatternComplete: ");
            lockView.handleGestureError();
            lockView.restMode();
//            lockView.mode=LockPatternView.DisplayMode.ERROR
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lockView.setOnPatternListener(listener)
    }
}