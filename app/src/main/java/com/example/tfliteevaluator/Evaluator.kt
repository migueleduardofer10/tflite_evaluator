package com.example.tfliteevaluator

import android.content.Context
import android.graphics.BitmapFactory
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.DataType

class Evaluator(private val context: Context, private val modelFile: String) {
    private val modelPath = "models/$modelFile"
    private val modelLabelsPath = "model_output_labels.txt"
    private val datasetPath = "cifar10_images"

    private val numClasses = 10

    // Datos del modelo
    private lateinit var interpreter: Interpreter
    private lateinit var inputType: DataType
    private lateinit var outputType: DataType
    private lateinit var labels: List<String>

    // Métricas acumuladas
    private var totalImages = 0
    private var correctTop1 = 0
    private val times = mutableListOf<Long>()
    private val truePos = IntArray(numClasses)
    private val falsePos = IntArray(numClasses)
    private val falseNeg = IntArray(numClasses)

    // ===============================
    // INICIALIZACIÓN
    // ===============================
    private fun loadInterpreter() {
        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model, Interpreter.Options().apply {
            setUseXNNPACK(true)
            setNumThreads(4)
        })

        inputType = interpreter.getInputTensor(0).dataType()
        outputType = interpreter.getOutputTensor(0).dataType()
        labels = FileUtil.loadLabels(context, modelLabelsPath)
    }

    // ===============================
    // EVALUACIÓN DEL MODELO
    // ===============================
    private fun evaluateModel() {
        val classDirs = context.assets.list(datasetPath) ?: return

        for ((classIndex, className) in classDirs.withIndex()) {
            val imageFiles = context.assets.list("$datasetPath/$className") ?: continue
            for (fileName in imageFiles) {
                val inputStream = context.assets.open("$datasetPath/$className/$fileName")
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val input = ImageUtils.preprocess(bitmap, inputType)

                val outputFloat = Array(1) { FloatArray(numClasses) }
                val outputByte = Array(1) { ByteArray(numClasses) }

                val start = System.nanoTime()
                when (outputType) {
                    DataType.FLOAT32 -> interpreter.run(input, outputFloat)
                    DataType.INT8, DataType.UINT8 -> interpreter.run(input, outputByte)
                    else -> throw IllegalArgumentException("Tipo de salida no soportado: $outputType")
                }
                val end = System.nanoTime()

                val prediction = when (outputType) {
                    DataType.FLOAT32 -> outputFloat[0]
                    DataType.UINT8 -> outputByte[0].map { it.toUByte().toFloat() }.toFloatArray()
                    DataType.INT8 -> outputByte[0].map { it.toInt().toFloat() }.toFloatArray()
                    else -> FloatArray(numClasses)
                }

                val top1 = prediction.indices.maxByOrNull { prediction[it] } ?: -1
                val predictedClass = labels[top1]

                if (predictedClass == className) {
                    correctTop1++
                    truePos[classIndex]++
                } else {
                    falsePos[top1]++
                    falseNeg[classIndex]++
                }

                times.add(end - start)
                totalImages++
            }
        }
    }

    // ===============================
    // CÁLCULO DE MÉTRICAS
    // ===============================
    private fun computeMetrics(): Map<String, Double> {
        val avgMs = times.average() / 1_000_000
        val throughput = if (avgMs > 0) 1000.0 / avgMs else 0.0
        val accTop1 = 100.0 * correctTop1 / totalImages

        // F1 promedio (macro)
        var f1Sum = 0.0
        for (i in 0 until numClasses) {
            val precision = if (truePos[i] + falsePos[i] > 0)
                truePos[i].toDouble() / (truePos[i] + falsePos[i])
            else 0.0

            val recall = if (truePos[i] + falseNeg[i] > 0)
                truePos[i].toDouble() / (truePos[i] + falseNeg[i])
            else 0.0

            val f1 = if (precision + recall > 0)
                2 * (precision * recall) / (precision + recall)
            else 0.0

            f1Sum += f1
        }
        val f1Score = (f1Sum / numClasses) * 100.0

        return mapOf(
            "accuracy" to accTop1,
            "f1" to f1Score,
            "latency" to avgMs,
            "throughput" to throughput
        )
    }

    // ===============================
    // FORMATEO DE RESULTADOS
    // ===============================
    private fun formatReport(metrics: Map<String, Double>): String {
        return """
            Evaluación completa
            ───────────────────────────────
            Modelo: $modelFile
            Imágenes evaluadas: $totalImages

            Precisión del clasificador:
            • Accuracy Top-1: ${"%.2f".format(metrics["accuracy"])} %
            • F1-Score promedio: ${"%.2f".format(metrics["f1"])} %

            Latencia de inferencia:
            • Tiempo promedio: ${"%.2f".format(metrics["latency"])} ms/img
            • Throughput: ${"%.2f".format(metrics["throughput"])} img/s
        """.trimIndent()
    }

    // ===============================
    // FUNCIÓN PRINCIPAL
    // ===============================
    fun runEvaluation(): String {
        loadInterpreter()
        evaluateModel()
        val metrics = computeMetrics()
        return formatReport(metrics)
    }
}
