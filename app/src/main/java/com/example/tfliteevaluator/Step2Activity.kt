package com.example.tfliteevaluator

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.tfliteevaluator.session.MasterSession

class Step2Activity : ComponentActivity() {

    private lateinit var modelText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step_2)

        val nextButton = findViewById<Button>(R.id.nextButton)
        val backButton = findViewById<Button>(R.id.backButton)
        modelText = findViewById(R.id.model_name_text)

        val selectedModel = MasterSession.selectedModel
        modelText.text = if (selectedModel != null)
            "Modelo seleccionado: $selectedModel"
        else
            "⚠️ No hay modelo seleccionado en sesión"

        backButton.setOnClickListener {
            // Regresa al selector
            val intent = Intent(this, ClassSelectionActivity::class.java)
            startActivity(intent)
            finish()
        }

        nextButton.setOnClickListener {
            val intent = Intent(this, TrainingLoadActivity::class.java)
            startActivity(intent)
        }
    }
}
