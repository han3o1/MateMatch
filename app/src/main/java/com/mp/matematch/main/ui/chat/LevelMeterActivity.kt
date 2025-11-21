package com.mp.matematch.main.ui.chat

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.mp.matematch.R
import kotlin.math.abs
import kotlin.math.atan2
import android.content.Intent

class LevelMeterActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var txtX: TextView
    private lateinit var txtY: TextView
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_meter)

        txtX = findViewById(R.id.txtTiltX)
        txtY = findViewById(R.id.txtTiltY)
        txtStatus = findViewById(R.id.txtStatus)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)

        // ✅ Jetpack 최신 문법: 반드시 객체로 콜백 생성해야 함
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val resultIntent = Intent().apply {
                        putExtra("levelResult", txtStatus.text.toString())
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val x = event!!.values[0].toDouble()
        val y = event.values[1].toDouble()

        val z = event.values[2].toDouble()

        val tiltX = Math.toDegrees(atan2(x, z))
        val tiltY = Math.toDegrees(atan2(y, z))

        txtX.text = "좌우 기울기: %.1f°".format(tiltX)
        txtY.text = "앞뒤 기울기: %.1f°".format(tiltY)

        val status = when {
            abs(tiltX) < 1 && abs(tiltY) < 1 -> "완벽한 수평입니다 👍"
            abs(tiltX) < 3 && abs(tiltY) < 3 -> "약간 기울어져 있어요 😅"
            else -> "주의! 많이 기울어졌어요 ⚠️"
        }

        txtStatus.text = status
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}
