package com.haoshield.data.util

import android.content.Context
import com.haoshield.data.blocking.PresetAppNames
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLabelProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * The friendliest name available: the installed app's own label, else the curated name for a
     * preset package (PackageManager can't name an app that isn't installed), else the raw id.
     */
    fun getLabel(packageName: String): String =
        runCatching {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrNull()
            ?: PresetAppNames[packageName]
            ?: packageName
}
