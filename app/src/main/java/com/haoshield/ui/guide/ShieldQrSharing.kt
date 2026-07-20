package com.haoshield.ui.guide

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/** Writes a generated shield QR to cache and opens the system share sheet so it can be printed. */
object ShieldQrSharing {
    fun share(context: Context, bitmap: Bitmap) {
        val dir = File(context.cacheDir, "shield_qr").apply { mkdirs() }
        val file = File(dir, "hao_shield.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, "Print or save your Hǎo Shield"),
        )
    }
}
