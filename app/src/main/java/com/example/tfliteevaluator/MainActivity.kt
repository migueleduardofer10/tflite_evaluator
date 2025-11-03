package com.example.tfliteevaluator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private lateinit var evaluator: Evaluator
    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultText = findViewById(R.id.result_text)
        val startEvalButton: Button = findViewById(R.id.start_eval)
        evaluator = Evaluator(this)

        startEvalButton.setOnClickListener {
            resultText.text = "Evaluando modelo..."
            Thread {
                val result = evaluator.runEvaluation()
                runOnUiThread {
                    resultText.text = result
                }
            }.start()
        }
    }
}
