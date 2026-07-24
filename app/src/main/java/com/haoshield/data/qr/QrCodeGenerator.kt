package com.haoshield.data.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import javax.inject.Inject
import javax.inject.Singleton

/** Renders a shield payload into a print-friendly QR bitmap. */
@Singleton
class QrCodeGenerator @Inject constructor() {

    fun generate(payload: String, sizePx: Int = DEFAULT_SIZE_PX): Bitmap {
        val hints = mapOf(
            // High-ish error correction so a laminated or lightly decorated print still scans.
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.Q,
            EncodeHintType.MARGIN to 2,
        )
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (matrix[x, y]) INK else Color.WHITE)
            }
        }
        return bitmap
    }

    private companion object {
        const val DEFAULT_SIZE_PX = 1024

        // Matches the brand ink color for print contrast.
        val INK = Color.rgb(0x3E, 0x4A, 0x3D)
    }
}
