package com.haoshield.desktop.data

import com.haoshield.domain.model.BlockingMode
import com.haoshield.domain.model.Session
import com.haoshield.domain.model.SessionMode
import com.haoshield.domain.model.ThemePreference
import com.haoshield.domain.service.BlockedAppsStore
import com.haoshield.domain.service.SessionSnapshot
import com.haoshield.domain.service.SessionStore
import com.haoshield.domain.service.SettingsStore
import com.haoshield.domain.service.ShieldPreferences
import com.haoshield.domain.service.StrictBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The desktop implementations of the stores the shared rules read through. Each one is the same
 * shape as its Android counterpart — the interfaces were extracted for exactly this — but writes to
 * a properties file rather than DataStore or Room.
 */

// A properties file holds one string per key, so lists are joined. The ASCII unit separator
// cannot occur in a Windows executable name, so it can never collide with the content.
private val LIST_SEPARATOR = Char(31).toString()
private const val ALLOWANCE_SEPARATOR = '|'

// --- session ---

class DesktopSessionStore(private val preferences: DesktopPreferences) : SessionStore {

    override fun observePersistedSession(): Flow<SessionSnapshot?> =
        preferences.values.map { it.toSnapshot() }

    override suspend fun persistActiveSession(
        session: Session,
        temporarilyAllowedPackages: Map<String, Long>,
    ) {
        preferences.edit {
            it[Keys.IS_ACTIVE] = "true"
            it[Keys.SESSION_ID] = session.id.toString()
            it[Keys.MODE] = session.mode.name
            it[Keys.STARTED_AT] = session.startedAtEpochMillis.toString()
            it[Keys.INTENTION] = session.intention.orEmpty()
            it[Keys.ALLOWED] = temporarilyAllowedPackages.encode()
        }
    }

    override suspend fun persistIntention(intention: String) {
        preferences.edit { if (it[Keys.IS_ACTIVE] == "true") it[Keys.INTENTION] = intention }
    }

    override suspend fun persistAllowedPackages(packages: Map<String, Long>) {
        preferences.edit { if (it[Keys.IS_ACTIVE] == "true") it[Keys.ALLOWED] = packages.encode() }
    }

    override suspend fun clearSession() {
        preferences.edit { Keys.all.forEach(it::remove) }
    }

    private fun Map<String, String>.toSnapshot(): SessionSnapshot? {
        if (this[Keys.IS_ACTIVE] != "true") return null
        val id = this[Keys.SESSION_ID]?.toLongOrNull() ?: return null
        val startedAt = this[Keys.STARTED_AT]?.toLongOrNull() ?: return null
        val mode = this[Keys.MODE]?.let { name ->
            runCatching { SessionMode.valueOf(name) }.getOrNull()
        } ?: return null

        return SessionSnapshot(
            session = Session(
                id = id,
                mode = mode,
                startedAtEpochMillis = startedAt,
                isActive = true,
                intention = this[Keys.INTENTION]?.takeIf(String::isNotBlank),
            ),
            temporarilyAllowedPackages = this[Keys.ALLOWED].orEmpty().decodeAllowances(),
        )
    }

    // Allowances are "process|expiry" joined by a separator, matching Android's encoding closely
    // enough that the two stay easy to reason about together. Entries without a delimiter are
    // dropped — fail closed, so anything unreadable re-blocks rather than letting an app through.
    private fun Map<String, Long>.encode(): String =
        entries.joinToString(LIST_SEPARATOR) { (name, expiry) -> "$name$ALLOWANCE_SEPARATOR$expiry" }

    private fun String.decodeAllowances(): Map<String, Long> =
        split(LIST_SEPARATOR)
            .filter(String::isNotBlank)
            .mapNotNull { entry ->
                val cut = entry.lastIndexOf(ALLOWANCE_SEPARATOR)
                if (cut <= 0) return@mapNotNull null
                val expiry = entry.substring(cut + 1).toLongOrNull() ?: return@mapNotNull null
                entry.substring(0, cut) to expiry
            }
            .toMap()

    private object Keys {
        const val IS_ACTIVE = "session_is_active"
        const val SESSION_ID = "session_id"
        const val MODE = "session_mode"
        const val STARTED_AT = "session_started_at"
        const val INTENTION = "session_intention"
        const val ALLOWED = "allowed_packages"

        val all = listOf(IS_ACTIVE, SESSION_ID, MODE, STARTED_AT, INTENTION, ALLOWED)
    }
}

// --- settings ---

class DesktopSettingsStore(private val preferences: DesktopPreferences) : SettingsStore {

    override fun observeBlockingMode(): Flow<BlockingMode> =
        preferences.observe("blocking_mode").map { stored ->
            if (stored == BlockingMode.SHIELD.name) BlockingMode.SHIELD else BlockingMode.SOFTWARE
        }

    override suspend fun setBlockingMode(mode: BlockingMode) = put("blocking_mode", mode.name)

    override fun observeAmbientSoundEnabled(): Flow<Boolean> = flag("ambient_sound", default = true)

    override suspend fun setAmbientSoundEnabled(enabled: Boolean) = put("ambient_sound", enabled)

    override fun observeQuotesEnabled(): Flow<Boolean> = flag("quotes", default = true)

    override suspend fun setQuotesEnabled(enabled: Boolean) = put("quotes", enabled)

    override fun observeStrictBlockingEnabled(): Flow<Boolean> = flag("strict", default = false)

    override suspend fun setStrictBlockingEnabled(enabled: Boolean) = put("strict", enabled)

    override fun observeThemePreference(): Flow<ThemePreference> =
        preferences.observe("theme").map { stored ->
            runCatching { ThemePreference.valueOf(stored.orEmpty()) }
                .getOrDefault(ThemePreference.SYSTEM)
        }

    override suspend fun setThemePreference(preference: ThemePreference) =
        put("theme", preference.name)

    override fun observeHasSeenIntro(): Flow<Boolean> = flag("has_seen_intro", default = false)

    override suspend fun setHasSeenIntro(seen: Boolean) = put("has_seen_intro", seen)

    private fun flag(key: String, default: Boolean): Flow<Boolean> =
        preferences.observe(key).map { it?.toBooleanStrictOrNull() ?: default }

    private fun put(key: String, value: Any) = preferences.edit { it[key] = value.toString() }
}

// --- the Shield itself ---

class DesktopShieldPreferences(private val preferences: DesktopPreferences) : ShieldPreferences {

    override fun observeRegisteredUid(): Flow<String?> = preferences.observe(UID)

    override suspend fun getRegisteredUid(): String? = preferences.get(UID)

    override suspend fun persistRegisteredUid(uid: String) = preferences.edit { it[UID] = uid }

    override suspend fun clearRegisteredUid() = preferences.edit { it.remove(UID) }

    override fun observeRegisteredQr(): Flow<String?> = preferences.observe(QR)

    override suspend fun getRegisteredQr(): String? = preferences.get(QR)

    override suspend fun persistRegisteredQr(payload: String) = preferences.edit { it[QR] = payload }

    override suspend fun clearRegisteredQr() = preferences.edit { it.remove(QR) }

    override suspend fun getPendingQr(): String? = preferences.get(PENDING)

    override suspend fun persistPendingQr(payload: String) =
        preferences.edit { it[PENDING] = payload }

    override suspend fun clearPendingQr() = preferences.edit { it.remove(PENDING) }

    private companion object {
        const val UID = "registered_shield_uid"
        const val QR = "registered_qr_token"
        const val PENDING = "pending_qr_token"
    }
}

// --- blocklist ---

class DesktopBlockedAppsStore(private val preferences: DesktopPreferences) : BlockedAppsStore {

    override fun observeBlockedPackages(): Flow<Set<String>?> =
        preferences.values.map { values ->
            if (values["user_customised"] != "true") {
                null
            } else {
                values["blocked_packages"].orEmpty()
                    .split(LIST_SEPARATOR)
                    .filter(String::isNotBlank)
                    .toSet()
            }
        }

    override suspend fun setBlockedPackages(packages: Set<String>) {
        preferences.edit {
            it["user_customised"] = "true"
            it["blocked_packages"] = packages.joinToString(LIST_SEPARATOR)
        }
    }
}

/**
 * Strict blocking is an Android-with-root idea (suspending packages through `pm`). The desktop
 * blocker already closes windows outright, so there is nothing stricter to escalate to.
 */
object NoStrictBlocking : StrictBlocking {
    override suspend fun applyForSession() = Unit

    override suspend fun releaseAll() = Unit

    override suspend fun release(packageName: String, resuspendAfterMillis: Long?) = Unit
}
