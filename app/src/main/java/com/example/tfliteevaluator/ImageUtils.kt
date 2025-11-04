package com.example.tfliteevaluator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.tensorflow.lite.DataType
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ImageUtils {

    fun loadImages(context: Context, datasetPath: String, inputSize: Int, inputType: DataType)
            : Pair<List<ByteBuffer>, List<String>> {

        val images = mutableListOf<ByteBuffer>()
        val labels = mutableListOf<String>()

        val classDirs = context.assets.list(datasetPath) ?: return Pair(emptyList(), emptyList())

        for (className in classDirs) {
            val imageFiles = context.assets.list("$datasetPath/$className") ?: continue
            for (fileName in imageFiles) {
                val inputStream = context.assets.open("$datasetPath/$className/$fileName")
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val buffer = preprocess(bitmap, inputSize, inputType)
                images.add(buffer)
                labels.add(className)
            }
        }
        return Pair(images, labels)
    }

    fun preprocess(bitmap: Bitmap, inputSize: Int, inputType: DataType): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        return when (inputType) {

            // ==================================================
            // FLOAT32 → usado en modelos normales o dinámicos
            // ==================================================
            DataType.FLOAT32 -> {
                val buffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
                buffer.order(ByteOrder.nativeOrder())
                for (y in 0 until inputSize) {
                    for (x in 0 until inputSize) {
                        val pixel = scaled.getPixel(x, y)
                        buffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
                        buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
                        buffer.putFloat((pixel and 0xFF) / 255f)
                    }
                }
                buffer.rewind()
                buffer
            }

            // ==================================================
            // INT8 → Full Integer Quantization (–128 a 127)
            // ==================================================
            DataType.INT8 -> {
                val buffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3)
                buffer.order(ByteOrder.nativeOrder())
                for (y in 0 until inputSize) {
                    for (x in 0 until inputSize) {
                        val pixel = scaled.getPixel(x, y)
                        // convertir 0–255 a -128–127
                        buffer.put((((pixel shr 16) and 0xFF) - 128).toByte())
                        buffer.put((((pixel shr 8) and 0xFF) - 128).toByte())
                        buffer.put(((pixel and 0xFF) - 128).toByte())
                    }
                }
                buffer.rewind()
                buffer
            }

            // ==================================================
            // INT8 → Full Integer Quantization (–128 a 127)
            // ==================================================
            DataType.UINT8 -> {
                val buffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3)
                buffer.order(ByteOrder.nativeOrder())

                for (y in 0 until inputSize) {
                    for (x in 0 until inputSize) {
                        val pixel = scaled.getPixel(x, y)
                        buffer.put(((pixel shr 16) and 0xFF).toByte())
                        buffer.put(((pixel shr 8) and 0xFF).toByte())
                        buffer.put((pixel and 0xFF).toByte())
                    }
                }

                buffer.rewind()
                buffer
            }

            else -> throw IllegalArgumentException("Tipo de entrada no soportado: $inputType")
        }
    }
}
