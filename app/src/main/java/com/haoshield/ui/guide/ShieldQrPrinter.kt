package com.haoshield.ui.guide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import androidx.core.content.res.ResourcesCompat
import com.haoshield.R
import java.io.FileOutputStream

/**
 * Prints a single, calm A4 page carrying the shield QR — not a full-bleed square. The QR sits at a
 * comfortable physical size inside a dashed "cut and keep" border, under the 好 glyph and a short
 * line of intention, with generous whitespace so it reads as a keepsake rather than a barcode.
 *
 * Uses the system print framework, so the user can send it to any printer or "Save as PDF".
 */
object ShieldQrPrinter {

    fun print(context: Context, qr: Bitmap) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("hao", "hao", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        printManager.print("Hǎo Shield", ShieldPrintAdapter(context, qr), attributes)
    }

    private class ShieldPrintAdapter(
        private val context: Context,
        private val qr: Bitmap,
    ) : PrintDocumentAdapter() {

        private var attributes: PrintAttributes = PrintAttributes.Builder().build()

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?,
        ) {
            attributes = newAttributes
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder("hao_shield.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()
            callback.onLayoutFinished(info, oldAttributes != newAttributes)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback,
        ) {
            val pdf = PrintedPdfDocument(context, attributes)
            val page = pdf.startPage(0)
            if (cancellationSignal?.isCanceled == true) {
                pdf.finishPage(page)
                pdf.close()
                callback.onWriteCancelled()
                return
            }

            drawPage(page)
            pdf.finishPage(page)

            runCatching {
                pdf.writeTo(FileOutputStream(destination.fileDescriptor))
            }.onFailure {
                pdf.close()
                callback.onWriteFailed(it.message)
                return
            }
            pdf.close()
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        }

        private fun drawPage(page: PdfDocument.Page) {
            val canvas = page.canvas
            val w = canvas.width.toFloat()
            val cx = w / 2f

            val serif = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            val sans = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            // The bundled single-glyph face, so the printed 好 matches the app and the icon rather
            // than resolving to whatever CJK font this device happens to ship (often a sans).
            val haoFace = ResourcesCompat.getFont(context, R.font.noto_serif_hao) ?: serif

            val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = INK
                typeface = haoFace
                textAlign = Paint.Align.CENTER
                textSize = w * 0.14f
            }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = INK
                typeface = serif
                textAlign = Paint.Align.CENTER
                textSize = w * 0.05f
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = INK_SOFT
                typeface = sans
                textAlign = Paint.Align.CENTER
                textSize = w * 0.028f
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = STONE
                style = Paint.Style.STROKE
                strokeWidth = w * 0.004f
                pathEffect = DashPathEffect(floatArrayOf(w * 0.02f, w * 0.014f), 0f)
            }

            var y = w * 0.22f
            canvas.drawText("好", cx, y, glyphPaint)
            y += w * 0.11f
            canvas.drawText("Hǎo Shield", cx, y, titlePaint)
            y += w * 0.06f
            canvas.drawText("Scan to begin protected time. Scan again to end.", cx, y, bodyPaint)

            // The QR inside a dashed cut-and-keep border, centred.
            val qrSize = w * 0.46f
            val qrLeft = cx - qrSize / 2f
            val qrTop = y + w * 0.06f
            val pad = w * 0.05f
            val border = RectF(qrLeft - pad, qrTop - pad, qrLeft + qrSize + pad, qrTop + qrSize + pad)
            canvas.drawRoundRect(border, w * 0.03f, w * 0.03f, borderPaint)

            val src = Rect(0, 0, qr.width, qr.height)
            val dst = RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)
            canvas.drawBitmap(qr, src, dst, Paint(Paint.ANTI_ALIAS_FLAG))

            val afterQr = qrTop + qrSize + pad + w * 0.06f
            canvas.drawText("Cut along the line and keep it somewhere meaningful.", cx, afterQr, bodyPaint)

            // Quiet footer. It mixes 好 with Latin and the bundled face holds only the glyph, so
            // it's drawn as two runs, measured and centred together as one line.
            val footerPaint = Paint(bodyPaint).apply {
                textSize = w * 0.024f
                color = STONE_TEXT
                textAlign = Paint.Align.LEFT
            }
            val footerGlyphPaint = Paint(footerPaint).apply { typeface = haoFace }
            val footerGlyph = "好"
            val footerText = " · Protect your attention. Return to yourself."
            val glyphWidth = footerGlyphPaint.measureText(footerGlyph)
            val footerY = canvas.height - w * 0.06f
            var footerX = cx - (glyphWidth + footerPaint.measureText(footerText)) / 2f
            canvas.drawText(footerGlyph, footerX, footerY, footerGlyphPaint)
            footerX += glyphWidth
            canvas.drawText(footerText, footerX, footerY, footerPaint)
        }
    }

    private val INK = Color.rgb(0x3E, 0x4A, 0x3D)
    private val INK_SOFT = Color.rgb(0x7A, 0x83, 0x77)
    private val STONE = Color.rgb(0xC9, 0xC2, 0xB4)
    private val STONE_TEXT = Color.rgb(0xA7, 0xAE, 0xA2)
}
