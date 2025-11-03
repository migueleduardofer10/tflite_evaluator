package com.example.tfliteevaluator

import android.content.Context
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
        val (images, groundTruths) = ImageUtils.loadImages(context, datasetPath, inputSize)
        val gtLabels = FileUtil.loadLabels(context, gtLabelsPath)

        var correctTop1 = 0
        val times = mutableListOf<Long>()

        for (i in images.indices) {
            val input = images[i]
            val output = Array(1) { FloatArray(numClasses) }

            val start = System.nanoTime()
            interpreter.run(input, output)
            val end = System.nanoTime()

            times.add(end - start)
            val prediction = output[0]
            val top1 = prediction.indices.maxByOrNull { prediction[it] } ?: -1

            if (modelLabels[top1] == gtLabels[i]) correctTop1++
        }

        val avgMs = times.average() / 1_000_000
        val acc = 100.0 * correctTop1 / images.size

        return """
            Evaluación completa
            Imágenes: ${images.size}
            Avg latency: ${"%.2f".format(avgMs)} ms
            Top-1 Accuracy: ${"%.2f".format(acc)} %
        """.trimIndent()
    }
}
