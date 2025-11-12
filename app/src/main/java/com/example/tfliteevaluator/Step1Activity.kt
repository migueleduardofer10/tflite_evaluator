package com.example.tfliteevaluator

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity

import com.example.tfliteevaluator.R

class Step1Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_step_1)

        val nextButton = findViewById<Button>(R.id.nextButton)

        nextButton.setOnClickListener {

            val intent = Intent(this, ClassSelectionActivity::class.java)
            startActivity(intent)
        }
    }
}
