package com.haoshield.domain.model

/** The kind of physical token a user registers as their Hǎo Shield. */
enum class ShieldTokenKind {
    /** A physical NFC tag, identified by its hardware UID. */
    NFC,

    /** A printed QR code, identified by the app-generated payload it carries. */
    QR,
}

/**
 * A registered "shield" the user taps or scans to start and end Shield Mode sessions.
 *
 * [id] is the raw identity for the kind: an uppercase NFC UID hex string, or the full
 * QR payload (e.g. "haoshield://shield/<uuid>").
 */
data class ShieldToken(
    val kind: ShieldTokenKind,
    val id: String,
) {
    /** Identity normalized for comparison — NFC UIDs are case-insensitive, QR payloads exact. */
    fun normalized(): ShieldToken = when (kind) {
        ShieldTokenKind.NFC -> copy(id = id.uppercase())
        ShieldTokenKind.QR -> this
    }
}
