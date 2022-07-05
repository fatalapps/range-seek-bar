package com.innovattic.rangeseekbar.sample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.innovattic.rangeseekbar.RangeSlider
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), RangeSlider.SeekBarChangeListener {
    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRangeSeekBar()
    }

    override fun onStartedSeeking() {
        Log.i(TAG, "Started seeking.")
    }

    override fun onStoppedSeeking() {
        Log.i(TAG, "Stopped seeking.")
    }

    private fun setupRangeSeekBar() {
        rangebar.max = 50
        rangebar.min = 30
        rangebar.setMinThumbVal(40)
        rangebar.setMaxThumbVal(45)
        rangebar.labelFormatterListener = RangeSlider.LabelFormatter { value ->
            "v$value"
        }

        rangebar.valueChangeListener = RangeSlider.ValueChangeListener { min, max ->
            Toast.makeText(this, "$min <-> $max", Toast.LENGTH_SHORT).show()
        }
    }
}
