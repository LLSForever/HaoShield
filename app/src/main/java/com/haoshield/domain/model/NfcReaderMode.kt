package com.haoshield.domain.model

enum class NfcReaderMode {
    /** Next tap registers the physical Hǎo Shield UID. */
    REGISTRATION,

    /** Tap starts or ends a Shield Mode session. */
    SESSION,
}