package com.example.tfliteevaluator

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.airbnb.lottie.LottieAnimationView
import com.example.tfliteevaluator.session.MasterSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrainingLoadActivity : ComponentActivity() {

    private lateinit var loader: LottieAnimationView
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_training_load)

        loader = findViewById(R.id.loadingAnimation)
        statusText = findViewById(R.id.statusText)

        val selectedModel = MasterSession.selectedModel
        if (selectedModel == null) {
            Toast.makeText(this, "No se seleccionó ningún modelo", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loader.playAnimation()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result: String

                if (selectedModel == "TODOS LOS MODELOS") {
                    val allModels = assets.list("models")
                        ?.filter { it.endsWith(".tflite") }
                        ?: emptyList()

                    result = evaluateAllModels(allModels)
                } else {
                    result = evaluateSingleModel(selectedModel)
                }

                // Guardar resultado global
                MasterSession.setEvaluationResult(result)

                withContext(Dispatchers.Main) {
                    loader.cancelAnimation()
                    loader.pauseAnimation()
                    Toast.makeText(
                        this@TrainingLoadActivity,
                        "Evaluación completada",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this@TrainingLoadActivity, Step3Activity::class.java)
                    startActivity(intent)
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loader.cancelAnimation()
                    statusText.text = "Error en la inferencia"
                    Toast.makeText(
                        this@TrainingLoadActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun evaluateSingleModel(model: String): String {
        withContext(Dispatchers.Main) {
            statusText.text = "Ejecutando inferencia con: $model"
        }

        val evaluator = Evaluator(this@TrainingLoadActivity, model)
        val result = evaluator.runEvaluation()
        return result
    }

    private suspend fun evaluateAllModels(models: List<String>): String {
        val results = mutableListOf<Map<String, Any>>()

        for ((index, model) in models.withIndex()) {
            try {

                withContext(Dispatchers.Main) {
                    statusText.text = """
                        Evaluando modelo ${index + 1} de ${models.size}
                        → $model
                    """.trimIndent()
                }

                val evaluator = Evaluator(this@TrainingLoadActivity, model)
                val metricsText = evaluator.runEvaluation()

                // Parsear métricas del texto
                val parsed = parseMetricsFromReport(metricsText)
                parsed["modelo"] = model
                results.add(parsed)

                withContext(Dispatchers.Main) {
                    statusText.text = """
                        Modelo ${index + 1}/${models.size} completado
                        $model
                    """.trimIndent()
                }

            } catch (e: Exception) {
                results.add(mapOf("modelo" to model, "error" to e.message.toString()))
            }
        }

        return buildComparisonTable(results)
    }

    private fun parseMetricsFromReport(report: String): MutableMap<String, Any> {
        val regex = """Accuracy Top-1:\s([\d.]+).*F1-Score promedio:\s([\d.]+).*Tiempo promedio:\s([\d.]+).*Throughput:\s([\d.]+)"""
            .toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(report)
        return mutableMapOf(
            "accuracy" to (match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0),
            "f1" to (match?.groupValues?.get(2)?.toDoubleOrNull() ?: 0.0),
            "latency" to (match?.groupValues?.get(3)?.toDoubleOrNull() ?: 0.0),
            "throughput" to (match?.groupValues?.get(4)?.toDoubleOrNull() ?: 0.0)
        )
    }

    private fun buildComparisonTable(results: List<Map<String, Any>>): String {
        val header = """
            ╔════════════════════════════════════════════════════════════════╗
            ║                  COMPARATIVA DE MODELOS                        ║
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
