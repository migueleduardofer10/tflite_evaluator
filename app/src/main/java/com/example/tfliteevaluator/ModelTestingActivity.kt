package com.example.tfliteevaluator

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity
import com.example.tfliteevaluator.session.MasterSession

class ModelTestingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_testing)

        val modelNameText = findViewById<TextView>(R.id.modelName)
        val accText = findViewById<TextView>(R.id.accuracyText)
        val f1Text = findViewById<TextView>(R.id.f1Text)
        val latText = findViewById<TextView>(R.id.latencyText)
        val thrText = findViewById<TextView>(R.id.throughputText)
        val recaptureButton = findViewById<Button>(R.id.recaptureButton)
        val tableContainer = findViewById<LinearLayout>(R.id.tableContainer)

        val modelName = MasterSession.selectedModel ?: "-"
        val report = MasterSession.evaluationResult ?: "Sin resultados"

        val regex = """Accuracy Top-1:\s([\d.]+).*F1-Score promedio:\s([\d.]+).*Tiempo promedio:\s([\d.]+).*Throughput:\s([\d.]+)"""
            .toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(report)

        if (match != null) {
            modelNameText.text = "Modelo: $modelName"
            accText.text = "Accuracy Top-1: ${match.groupValues[1]} %"
            f1Text.text = "F1-Score promedio: ${match.groupValues[2]} %"
            latText.text = "Latencia promedio: ${match.groupValues[3]} ms/img"
            thrText.text = "Throughput: ${match.groupValues[4]} img/s"
        }

        else if (report.contains("COMPARATIVA DE MODELOS")) {
            modelNameText.text = "Resultados de inferencia múltiple:"
            accText.text = ""
            f1Text.text = ""
            latText.text = ""
            thrText.text = ""

            val tableView = TextView(this).apply {
                text = report
                textSize = 12f
                typeface = android.graphics.Typeface.MONOSPACE
                setPadding(8, 16, 8, 16)
                setTextColor(resources.getColor(android.R.color.black, null))
                isVerticalScrollBarEnabled = true
            }

            tableContainer.addView(tableView)
        }

        else {
            modelNameText.text = "No se encontraron resultados"
            accText.text = "Accuracy Top-1: -"
            f1Text.text = "F1-Score promedio: -"
            latText.text = "Latencia promedio: -"
            thrText.text = "Throughput: -"
        }

        recaptureButton.setOnClickListener {
            MasterSession.clear()
            startActivity(Intent(this, ClassSelectionActivity::class.java))
            finish()
        }
    }
}
