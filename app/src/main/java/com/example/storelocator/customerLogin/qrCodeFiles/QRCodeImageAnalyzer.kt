package com.example.storelocator.customerLogin.qrCodeFiles

import android.graphics.ImageFormat.*
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import java.nio.ByteBuffer
import java.util.*


class QRCodeImageAnalyzer(param: QRCodeFoundListener) : ImageAnalysis.Analyzer{

    private var listener: QRCodeFoundListener = param
    lateinit var date: Date

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }

    override fun analyze(image: ImageProxy) {
        try {
            date = Calendar.getInstance().time
            if (image.format == YUV_420_888 || image.format == YUV_422_888 || image.format == YUV_444_888){
                val data = image.planes[0].buffer.toByteArray()

                val source = PlanarYUVLuminanceSource(
                        data,
                        image.width,
                        image.height,
                        0,
                        0,
                        image.width,
                        image.height,
                        false
                )


//                Log.i("CLEAR","source: $source")

                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                try {
                    val result = QRCodeMultiReader().decode(binaryBitmap)
                    Log.i("CLEAR", "QRCode${result.text}")
                    listener.onQRCodeFound(result.text);
                } catch (e: NotFoundException) {
                    listener.qrCodeNotFound()
                }
            }
            image.close()
        }catch (e: FormatException){
            val date2 = Calendar.getInstance().time
            Log.i("CLEAR", "Analyze time: ${date2.time-date.time}")
            Log.i("CLEAR", "QRException: ${e.message}")
            image.close()
        }catch (e: ChecksumException){
            val date2 = Calendar.getInstance().time
            Log.i("CLEAR", "Analyze time: ${date2.time-date.time}")
            Log.i("CLEAR", "QRException: ${e.message}")
            image.close()
        }
    }
}