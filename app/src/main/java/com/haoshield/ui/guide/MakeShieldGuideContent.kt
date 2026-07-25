package com.haoshield.ui.guide

internal data class GuideStepContent(
    val number: Int,
    val title: String,
    val body: String,
)

internal object MakeShieldGuideContent {
    const val screenTitle = "Make Your Own Shield"

    const val subtitle =
        "A small object you keep close — tap it to begin protected time, and tap again to end."

    const val physicalStrengthMessage =
        "Software Mode is a lighter way to begin. A physical Shield asks a little more intention: " +
            "the tap becomes a quiet ritual."

    /** Four short, numbered craft steps — deliberately spare. */
    val craftSteps = listOf(
        GuideStepContent(
            number = 1,
            title = "Choose your object",
            body = "A stone, a wooden disc, a sturdy card. Something you'll want to touch.",
        ),
        GuideStepContent(
            number = 2,
            title = "Add the tag",
            body = "Affix a blank NFC sticker (NTAG213 works well) beneath or inside it — or skip " +
                "the tag and print a Shield code instead.",
        ),
        GuideStepContent(
            number = 3,
            title = "Make it yours",
            body = "Carve, oil, wrap, or leave it plain. There is no wrong way.",
        ),
        GuideStepContent(
            number = 4,
            title = "Keep it close",
            body = "On your desk, at your bedside, with your keys — somewhere you'll notice it.",
        ),
    )

    const val registerPrompt =
        "When your Shield is ready, register it below — you'll choose between an NFC tag you tap " +
            "and a code you print and scan."

    /** Surfaced on the guide itself, so the printed route is visible without tapping through. */
    const val printedAlternative =
        "No tag to hand? A printed Shield code works just as well — you scan it instead of " +
            "tapping, and it never runs out of battery."

    const val noNfcOnDevice =
        "This phone doesn't have NFC, so a printed code is your way in."

    const val registerInstruction =
        "Hold your Shield to the back of your phone. Stay still for a moment."

    const val registerSuccess =
        "Your Hǎo Shield is registered. It is ready when you are."

    const val alreadyRegistered =
        "You already have a Shield registered on this device. You can register another below."

    const val chooseMethodPrompt =
        "Your Shield can be a physical NFC tag you tap, or a printed code you scan. Either works — " +
            "or keep both, so you always have a way in."

    const val methodNfcLabel = "Use an NFC tag"

    const val methodQrLabel = "Use a printed code"

    const val qrDisplayInstruction =
        "This is your Shield code. Print it on A4 and keep it somewhere meaningful — beside your " +
            "desk, on the fridge, wherever calls you back to yourself."

    const val qrConfirmPrompt =
        "Once it's printed, scan the printed code to confirm your Shield. This proves the print " +
            "really works, so you can rely on it later."

    const val qrAlreadyRegistered =
        "This is your registered Shield code — the same one your printed sheet carries. Scan it " +
            "with any camera to begin or end protected time. Print another copy if you need one."

    const val qrRegisterSuccess =
        "Your printed Hǎo Shield is registered. Scan it to begin and end protected time."
}
