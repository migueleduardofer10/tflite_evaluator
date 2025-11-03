package com.example.tfliteevaluator

import android.content.Context
import android.graphics.BitmapFactory
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil

class Evaluator(private val context: Context) {
    private val modelPath = "mobilenetv2_base_dynamic.tflite"
    private val modelLabelsPath = "model_output_labels.txt"
    private val gtLabelsPath = "cifar10_validation_labels.txt"
    private val datasetPath = "cifar10_images"
    private val inputSize = 32
    private val numClasses = 10

    fun runEvaluation(): String {
        val model = FileUtil.loadMappedFile(context, modelPath)
        val interpreter = Interpreter(model, Interpreter.Options().apply {
            setUseXNNPACK(true)
            setNumThreads(4)
        })

        val modelLabels = FileUtil.loadLabels(context, modelLabelsPath)
        val gtLabels = FileUtil.loadLabels(context, gtLabelsPath)

        val classDirs = context.assets.list(datasetPath) ?: return "No dataset"
        var correctTop1 = 0
        val times = mutableListOf<Long>()
        var totalImages = 0

        for (className in classDirs) {
            val imageFiles = context.assets.list("$datasetPath/$className") ?: continue
            for (fileName in imageFiles) {
                val inputStream = context.assets.open("$datasetPath/$className/$fileName")
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val input = ImageUtils.preprocess(bitmap, inputSize)

                val output = Array(1) { FloatArray(numClasses) }

                val start = System.nanoTime()
                interpreter.run(input, output)
                val end = System.nanoTime()

                val prediction = output[0]
                val top1 = prediction.indices.maxByOrNull { prediction[it] } ?: -1

                if (modelLabels[top1] == className) correctTop1++
                times.add(end - start)
                totalImages++
            }
        }

        val avgMs = times.average() / 1_000_000
        val acc = 100.0 * correctTop1 / totalImages

        return """
        Evaluación completa
        Imágenes: $totalImages
        Promedio latencia: ${"%.2f".format(avgMs)} ms
        Precisión Top-1: ${"%.2f".format(acc)} %
    """.trimIndent()
    }

}
