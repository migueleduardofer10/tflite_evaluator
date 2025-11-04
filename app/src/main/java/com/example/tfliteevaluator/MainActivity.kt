package com.example.tfliteevaluator

import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private lateinit var evaluator: Evaluator
    private lateinit var resultText: TextView
    private lateinit var modelSelector: Spinner
    private var selectedModel: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultText = findViewById(R.id.result_text)
        modelSelector = findViewById(R.id.model_selector)
        val startEvalButton: Button = findViewById(R.id.start_eval)


        val modelList = assets.list("models")?.filter { it.endsWith(".tflite") } ?: emptyList()

        if (modelList.isEmpty()) {
            Toast.makeText(this, "No se encontraron modelos .tflite en assets/", Toast.LENGTH_LONG).show()
            return
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modelSelector.adapter = adapter

        selectedModel = modelList.first()

        modelSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long
            ) {
                selectedModel = modelList[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        startEvalButton.setOnClickListener {
            resultText.text = "Evaluando modelo: $selectedModel..."
            evaluator = Evaluator(this, selectedModel)

            Thread {
                val result = evaluator.runEvaluation()
                runOnUiThread {
                    resultText.text = result
                }
            }.start()
        }
    }
}
