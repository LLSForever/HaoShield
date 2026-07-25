package com.haoshield.domain.model

/** What a shield scan (NFC tap or QR camera scan) should do. */
enum class ScanMode {
    /** Next scan registers the physical Hǎo Shield (NFC UID or QR payload). */
    REGISTRATION,

    /** Scan starts or ends a Shield Mode session. */
    SESSION,
}
