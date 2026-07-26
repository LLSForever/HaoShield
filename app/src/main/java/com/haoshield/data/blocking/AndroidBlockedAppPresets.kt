package com.haoshield.data.blocking

import com.haoshield.domain.model.BlockedAppGroup
import com.haoshield.domain.service.BlockedAppPresets
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The curated Android list. Kept on this side of the boundary because package names are an Android
 * fact; another platform supplies its own.
 */
@Singleton
class AndroidBlockedAppPresets @Inject constructor() : BlockedAppPresets {
    override val groups: List<BlockedAppGroup> = PresetBlockedAppGroups
}
