package com.example.tfliteevaluator

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var resultText: TextView
    private lateinit var modelSelector: Spinner
    private var selectedModel: String = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultText = findViewById(R.id.result_text)
        modelSelector = findViewById(R.id.model_selector)
        val startEvalButton: Button = findViewById(R.id.start_eval)

        // === Cargar lista de modelos ===
        val modelList = assets.list("models")?.filter { it.endsWith(".tflite") }?.toMutableList() ?: mutableListOf()
        if (modelList.isEmpty()) {
            Toast.makeText(this, "No se encontraron modelos .tflite en assets/models", Toast.LENGTH_LONG).show()
            return
        }

        runOnUiThread {
            resultText.text = "Modelos detectados en assets/models:\n\n" +
                    modelList.joinToString(separator = "\n") { "• $it" }
        }

        // Agregar opción global
        modelList.add(0, "TODOS LOS MODELOS")

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

        // === Acción del botón ===
        startEvalButton.setOnClickListener {
            resultText.text = "Evaluando $selectedModel ..."

            Thread {
                val result = if (selectedModel == "TODOS LOS MODELOS")
                    evaluateAllModels(modelList.drop(1)) // Quita la opción "todos"
                else
                    Evaluator(this, selectedModel).runEvaluation()

                runOnUiThread { resultText.text = result }
            }.start()
        }
    }

    // === Evaluación masiva ===
    private fun evaluateAllModels(models: List<String>): String {
        val results = mutableListOf<Map<String, Any>>()

        for ((index, model) in models.withIndex()) {
            try {
                // Mostrar progreso en pantalla
                runOnUiThread {
                    resultText.text = """
                        Evaluando modelo ${index + 1} de ${models.size}
                        → $model
                    """.trimIndent()
                }

                val evaluator = Evaluator(this, model)
                val metrics = evaluator.runEvaluation()
                // Extraer métricas numéricas del texto
                val parsed = parseMetricsFromReport(metrics)
                parsed["modelo"] = model
                results.add(parsed)

                runOnUiThread {
                    resultText.text = """
                        Modelo ${index + 1}/${models.size} completado:
                        $model
                    """.trimIndent()
                }

            } catch (e: Exception) {
                results.add(mapOf("modelo" to model, "error" to e.message.toString()))
            }
        }

        return buildComparisonTable(results)
    }

    // === Parseo simple de texto (accuracy, f1, latency, throughput) ===
    private fun parseMetricsFromReport(report: String): MutableMap<String, Any> {
        val regex = """Accuracy Top-1:\s([\d.]+).*F1-Score promedio:\s([\d.]+).*Tiempo promedio:\s([\d.]+).*Throughput:\s([\d.]+)""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(report)
        return mutableMapOf(
            "accuracy" to (match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0),
            "f1" to (match?.groupValues?.get(2)?.toDoubleOrNull() ?: 0.0),
            "latency" to (match?.groupValues?.get(3)?.toDoubleOrNull() ?: 0.0),
            "throughput" to (match?.groupValues?.get(4)?.toDoubleOrNull() ?: 0.0)
        )
    }

    // === Generar tabla comparativa ===
    private fun buildComparisonTable(results: List<Map<String, Any>>): String {
        val header = """
            ╔════════════════════════════════════════════════════════════════╗
            ║                  COMPARATIVA DE MODELOS                     ║
            ╠════════════════════════════════════════════════════════════════╣
            ║ Modelo                              | Acc(%) | F1(%) | Lat(ms) | Thr(img/s) ║
            ╠════════════════════════════════════════════════════════════════╣
        """.trimIndent()

        val rows = results.joinToString("\n") { r ->
            val model = r["modelo"]
            val acc = "%.2f".format(r["accuracy"] ?: 0.0)
            val f1 = "%.2f".format(r["f1"] ?: 0.0)
            val lat = "%.2f".format(r["latency"] ?: 0.0)
            val thr = "%.2f".format(r["throughput"] ?: 0.0)
            "║ ${model.toString().padEnd(35)} | ${acc.padStart(6)} | ${f1.padStart(6)} | ${lat.padStart(7)} | ${thr.padStart(10)} ║"
        }

        val footer = "\n╚════════════════════════════════════════════════════════════════╝"

        return "$header\n$rows$footer"
    }
}
