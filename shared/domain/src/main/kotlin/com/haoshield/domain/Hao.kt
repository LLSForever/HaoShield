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
 * screen — draws its rhythm from [BREATH_MILLIS]; the three qualities in
 * [TRUTHFULNESS], [COMPASSION] and [FORBEARANCE] each name a decision already
 * made in the design; and a single pixel of ink is set beneath the 好 on the
 * home screen, fully present and never seen.
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
     * 真善忍 — Truthfulness, Compassion, Forbearance.
     *
     * What the innate nature the app exists to make room for resonates with.
     * The introduction names them on its first page; they are held here so the
     * codebase remembers them too, rather than leaving them a thing said once
     * at the door.
     *
     * They are not decoration. Each already governs a decision that was made
     * before it had a name:
     *
     *  - 真 the app does not deceive. No streaks, no manufactured urgency, no
     *    invented numbers; and where the blocking can be slipped past, the copy
     *    says so plainly rather than implying a wall that isn't there.
     *  - 善 the boundary is kind. It rests an app rather than forbidding it, it
     *    always offers a way through, and the journal keeps intentions rather
     *    than a tally of failures.
     *  - 忍 the friction *is* the practice. A written intention, a slow fade, a
     *    countdown that must simply be waited out — every one of them asks the
     *    person to sit a moment with an impulse instead of acting on it.
     */
    const val TRUTHFULNESS: Char = '真'

    /** 善 — see [TRUTHFULNESS]. */
    const val COMPASSION: Char = '善'

    /** 忍 — see [TRUTHFULNESS]. */
    const val FORBEARANCE: Char = '忍'

    /** 真善忍, together. */
    const val THREE_QUALITIES: String = "真善忍"

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
