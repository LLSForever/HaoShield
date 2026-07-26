package com.haoshield.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Normalisation is what decides whether a Shield is recognised as your own. The two kinds are
 * deliberately treated differently: an NFC tag's UID is the same tag whatever case a reader
 * reports it in, while a printed code's payload is exact — a QR that differs by one character is
 * a different Shield, not a sloppily-read one.
 */
class ShieldTokenTest {

    @Test
    fun `an NFC tag is the same tag whatever case it is read in`() {
        val lower = ShieldToken(ShieldTokenKind.NFC, "04a2b3c4d5").normalized()
        val upper = ShieldToken(ShieldTokenKind.NFC, "04A2B3C4D5").normalized()
        assertEquals(upper, lower)
        assertEquals("04A2B3C4D5", lower.id)
    }

    @Test
    fun `a printed code is taken exactly as scanned`() {
        val payload = "haoshield://shield/AbCdEf"
        val normalized = ShieldToken(ShieldTokenKind.QR, payload).normalized()
        assertEquals(payload, normalized.id)
    }

    @Test
    fun `case matters for a printed code - it is a payload, not a reading`() {
        val lower = ShieldToken(ShieldTokenKind.QR, "haoshield://shield/abc").normalized()
        val upper = ShieldToken(ShieldTokenKind.QR, "haoshield://shield/ABC").normalized()
        assertNotEquals(upper, lower)
    }

    @Test
    fun `a tag and a printed code with the same id are different Shields`() {
        val nfc = ShieldToken(ShieldTokenKind.NFC, "ABC").normalized()
        val qr = ShieldToken(ShieldTokenKind.QR, "ABC").normalized()
        assertNotEquals(nfc, qr)
    }

    @Test
    fun `normalising twice changes nothing`() {
        val once = ShieldToken(ShieldTokenKind.NFC, "04a2b3").normalized()
        assertEquals(once, once.normalized())
    }
}
