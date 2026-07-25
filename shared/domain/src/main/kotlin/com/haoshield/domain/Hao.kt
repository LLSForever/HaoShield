package com.haoshield.domain

/**
 * 好
 *
 * The character for goodness is not an abstraction. It is two figures set side
 * by side: 女, the mother, and 子, the child. Whoever first cut this character
 * into bone, some three thousand years ago, answered a question philosophers
 * still circle: what is good? Not an idea. A bond. Care, embodied — one being
 * turned toward another.
 *
 * This file is the app's mason's mark. Stone carvers signed the cathedral in
 * places no visitor would ever look — inside a tower, behind a capital — not
 * for the audience but for the work, and for whatever the work was in service
 * of. In that spirit this object asserts nothing new, computes nothing clever,
 * and optimises nothing. It holds.
 *
 * It lives here, in the one module with no platform, because this is the part
 * of the codebase that is pure principle — no screens, no hardware, no
 * permissions. The Android app is one material body of what is written here;
 * any future body inherits it the same way. The principle precedes its
 * embodiments and outlives them.
 *
 * And it is a principle *in action*, not a museum piece: every breathing
 * animation in the app — the glyph at rest, the waiting text, the darkened
 * screen — draws its rhythm from [BREATH_MILLIS], and a single pixel of ink
 * is set beneath the 好 on the home screen, fully present and never seen.
 */
object Hao {

    /** 女 — the one who cares. */
    const val MOTHER: Char = '女'

    /** 子 — the one cared for. */
    const val CHILD: Char = '子'

    /**
     * 好 — the two together. Goodness, drawn as a relationship.
     *
     * Note that it cannot be derived from its parts by any arithmetic — no sum
     * of codepoints produces it. The composition is real but not mechanical,
     * which is itself the point.
     */
    const val GOOD: Char = '好'

    /**
     * One resting human breath, in milliseconds — about fourteen to the
     * minute, the pace of a body at ease. Everything in the app that breathes,
     * breathes at this pace. The app's pulse is not a frame rate or a polling
     * interval; it is a person, resting.
     */
    const val BREATH_MILLIS: Int = 4_200

    /**
     * The pixel: presence, whole and undivided. Used as the alpha of the
     * mason's mark — the one value in the app that is exactly and only 1.
     */
    const val PRESENCE: Float = 1f
}
