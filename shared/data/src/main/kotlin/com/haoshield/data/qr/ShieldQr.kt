package com.haoshield.data.qr

import java.util.UUID

/** Payload scheme for printed Hǎo Shield QR codes. */
object ShieldQr {
    const val PREFIX = "haoshield://shield/"

    /** A fresh, unguessable payload for a new printed shield. */
    fun newPayload(): String = PREFIX + UUID.randomUUID()

    /** True if a scanned value is one of our shield codes (vs. any random QR). */
    fun isShieldPayload(value: String): Boolean = value.startsWith(PREFIX)
}
