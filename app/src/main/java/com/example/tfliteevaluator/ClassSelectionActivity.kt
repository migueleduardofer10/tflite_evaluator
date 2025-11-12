package com.example.tfliteevaluator

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import com.example.tfliteevaluator.session.MasterSession

class ClassSelectionActivity : ComponentActivity() {

    private lateinit var modelSelector: Spinner
    private var selectedModel: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_selection)

        modelSelector = findViewById(R.id.model_selector)
        val nextButton: Button = findViewById(R.id.nextButton)
        val backButton: Button = findViewById(R.id.backButton)

        val modelList = assets.list("models")
            ?.filter { it.endsWith(".tflite") }
            ?.toMutableList() ?: mutableListOf()

        if (modelList.isEmpty()) {
            Toast.makeText(this, "No se encontraron modelos .tflite en assets/models", Toast.LENGTH_LONG).show()
            return
        }

        modelList.add(0, "TODOS LOS MODELOS")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modelSelector.adapter = adapter

        MasterSession.selectedModel?.let { saved ->
            if (modelList.contains(saved)) {
                modelSelector.setSelection(modelList.indexOf(saved))
                selectedModel = saved
            }
        } ?: run {
            selectedModel = modelList.first()
        }

        modelSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedModel = modelList[position]
                MasterSession.setSelectedModel(selectedModel)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        nextButton.setOnClickListener {
            MasterSession.setSelectedModel(selectedModel)
            val intent = Intent(this, Step2Activity::class.java)
            startActivity(intent)
        }

        backButton.setOnClickListener {
            val intent = Intent(this, Step1Activity::class.java)
            startActivity(intent)
        }
    }
}
