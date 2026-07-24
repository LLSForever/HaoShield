package com.haoshield.ui.intro

/** The first-run introduction — the app's philosophy, in the author's words. */
internal data class IntroPage(
    val title: String,
    val paragraphs: List<String>,
    val showGlyph: Boolean = false,
)

internal val IntroPages = listOf(
    IntroPage(
        title = "The Nature of Hǎo",
        showGlyph = true,
        paragraphs = listOf(
            "In its deepest sense, Hǎo points to our innate true nature — the original goodness " +
                "and harmony that already exists within us. It is not something we must create " +
                "from nothing, but something we can return to and cultivate.",
            "This inner nature naturally resonates with 真善忍 — Truthfulness, Compassion, and " +
                "Forbearance.",
        ),
    ),
    IntroPage(
        title = "What Pulls Us Away",
        paragraphs = listOf(
            "In daily life, constant distraction and the distorted currents of social media and " +
                "modern culture quietly draw us away from this nature.",
            "They fragment our attention, stir agitation, and make it harder to live in alignment " +
                "with what is true, good, and steady within us.",
        ),
    ),
    IntroPage(
        title = "The Need for Space",
        paragraphs = listOf(
            "To cultivate Hǎo, we need space.",
            "Not more stimulation or better self-control techniques, but actual room — free from " +
                "the constant pull — where we can begin to remember and strengthen what is already " +
                "there.",
        ),
    ),
    IntroPage(
        title = "What Hǎo Shield Offers",
        paragraphs = listOf(
            "Hǎo Shield exists to help create that space.",
            "By using a simple physical object you make yourself, you can temporarily step out of " +
                "the flow of distraction. In the protected time this creates, you give yourself " +
                "the opportunity to return to your own nature and cultivate it in whatever way is " +
                "natural to you.",
        ),
    ),
    IntroPage(
        title = "How It Works",
        paragraphs = listOf(
            "You decide which parts of the phone pull you away from what matters.",
            "You tap your physical Hǎo Shield to enter protected time.",
            "In that space, the noise is reduced, and you are free to choose what you actually " +
                "want to give your attention to — whether that is your work, your family, " +
                "handcraft, or quiet inner cultivation.",
            "This is not about forcing a particular path. It is about giving yourself the " +
                "conditions to live closer to your own true nature.",
        ),
    ),
)
