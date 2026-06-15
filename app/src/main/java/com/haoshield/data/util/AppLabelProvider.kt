package com.haoshield.data.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLabelProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getLabel(packageName: String): String =
        runCatching {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrDefault(packageName)
}