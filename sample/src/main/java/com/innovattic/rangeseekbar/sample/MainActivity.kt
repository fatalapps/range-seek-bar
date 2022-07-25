package com.innovattic.rangeseekbar.sample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.innovattic.rangeseekbar.RangeSlider
import com.innovattic.rangeseekbar.WindPicker
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
        rangebar.max = 200
        rangebar.min = 0
        rangebar.setMinThumbVal(0)
        rangebar.setMaxThumbVal(100)
        rangebar.labelFormatterListener = RangeSlider.LabelFormatter { value ->
            "v-$value"
        }

        rangebar.valueChangeListener = RangeSlider.ValueChangeListener { min, max ->

            Toast.makeText(applicationContext, windp.getSelection().toString(), Toast.LENGTH_SHORT).show()
        }
        windp.setDirections(setOf(WindPicker.Direction.N, WindPicker.Direction.S, WindPicker.Direction.SW))
    }
}
