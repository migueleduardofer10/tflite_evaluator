package com.example.tfliteevaluator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.tensorflow.lite.DataType
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ImageUtils {


    fun preprocess(bitmap: Bitmap, inputType: DataType): ByteBuffer {
        val width = bitmap.width
        val height = bitmap.height

        return when (inputType) {
            DataType.FLOAT32 -> {
                val buffer = ByteBuffer.allocateDirect(1 * width * height * 3 * 4)
                buffer.order(ByteOrder.nativeOrder())
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val pixel = bitmap.getPixel(x, y)
                        buffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
                        buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
                        buffer.putFloat((pixel and 0xFF) / 255f)
                    }
                }
                buffer.rewind()
                buffer
            }

            DataType.INT8 -> {
                val buffer = ByteBuffer.allocateDirect(1 * width * height * 3)
                buffer.order(ByteOrder.nativeOrder())
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val pixel = bitmap.getPixel(x, y)
                        buffer.put((((pixel shr 16) and 0xFF) - 128).toByte())
                        buffer.put((((pixel shr 8) and 0xFF) - 128).toByte())
                        buffer.put(((pixel and 0xFF) - 128).toByte())
                    }
                }
                buffer.rewind()
                buffer
            }

            DataType.UINT8 -> {
                val buffer = ByteBuffer.allocateDirect(1 * width * height * 3)
                buffer.order(ByteOrder.nativeOrder())
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val pixel = bitmap.getPixel(x, y)
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
