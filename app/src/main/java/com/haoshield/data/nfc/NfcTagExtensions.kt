package com.haoshield.data.nfc

import android.nfc.Tag

fun Tag.toShieldUid(): String =
    id.joinToString(separator = "") { byte -> "%02X".format(byte) }