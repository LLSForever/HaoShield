package com.haoshield.ui.guide

internal data class GuideSection(
    val title: String,
    val body: String,
)

internal object MakeShieldGuideContent {
    const val screenTitle = "Make Your Own Hǎo Shield"

    const val introduction =
        "A Hǎo Shield is a small physical object you keep close — a gentle reminder " +
            "to protect your attention and return to yourself."

    const val physicalStrengthMessage =
        "Software Mode is a lighter way to begin. A physical Shield offers stronger " +
            "protection: the tap becomes a quiet ritual, and ending a session asks a " +
            "little more intention."

    val sections = listOf(
        GuideSection(
            title = "Why make one?",
            body = "You already have what you need to begin. A physical Shield turns " +
                "protection into something you can hold — not a rule on your phone, but " +
                "a kind companion on your desk, in your pocket, or beside your keys.",
        ),
        GuideSection(
            title = "What you'll need",
            body = "A blank NFC tag or sticker (NTAG213 works well), something to attach " +
                "it to — a smooth stone, a wooden disc, a sturdy card — and a few quiet " +
                "minutes to make it yours.",
        ),
        GuideSection(
            title = "Make it yours",
            body = "Choose a material that feels grounded to you. Affix the NFC tag beneath " +
                "or inside your object so the surface stays simple. There is no wrong " +
                "aesthetic — only what feels honest.",
        ),
        GuideSection(
            title = "Keep it close",
            body = "Place your Shield where you will notice it: beside your bed, on your " +
                "desk, with your keys. When you are ready, tap it to the back of your " +
                "phone to begin protected time. Tap again to end.",
        ),
    )

    const val registerPrompt =
        "When your Shield is ready, register its tag below. This links your physical " +
            "object to Hǎo Shield on this device."

    const val registerInstruction =
        "Hold your Shield to the back of your phone. Stay still for a moment."

    const val registerSuccess =
        "Your Hǎo Shield is registered. It is ready when you are."

    const val alreadyRegistered =
        "You already have a Shield registered on this device. You can register a new " +
            "tag below if you have made another."

    const val chooseMethodPrompt =
        "Your Shield can be a physical NFC tag you tap, or a printed code you scan. " +
            "Either works — or keep both, so you always have a way in."

    const val methodNfcLabel = "Use an NFC tag"

    const val methodQrLabel = "Use a printed code"

    const val qrDisplayInstruction =
        "This is your Shield code. Print it and keep it somewhere meaningful — beside your " +
            "desk, on the fridge, wherever calls you back to yourself."

    const val qrConfirmPrompt =
        "Once it's printed, scan the printed code to confirm your Shield. This proves the " +
            "print really works, so you can rely on it later."

    const val qrRegisterSuccess =
        "Your printed Hǎo Shield is registered. Scan it to begin and end protected time."
}