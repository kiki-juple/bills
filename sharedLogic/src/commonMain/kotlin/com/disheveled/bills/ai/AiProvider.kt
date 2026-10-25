package com.disheveled.bills.ai

/**
 * One OpenAI-compatible chat-completions backend, and how each kind of call is tuned on it.
 *
 * The providers disagree on how thinking is switched down — NIM takes a token budget, Gemini
 * and Groq an effort level, and Gemini 3 can't turn it off at all — so every provider carries
 * its own measured values instead of [ReceiptScanner] sending one set to all of them.
 */
class AiProvider internal constructor(
    val name: String,
    internal val endpoint: String,
    internal val apiKey: String,
    internal val model: String,
    /** Reading a photo: perception, so as little thinking as the provider allows. */
    internal val extraction: Tuning,
    /** The split recap and the month note: relational, so a little thinking. */
    internal val note: Tuning,
) {
    companion object {
        /**
         * First choice, on the free tier. A test receipt came back exactly right on 4 of 4
         * runs in 2.9–23.7s (the spread is the free tier queueing, not the model), and on
         * real receipts it dropped the non-item noise by itself. The free quota ran out on the
         * fifth call inside a minute, which is what [groq] is behind it for. Recaps: 3 of 3
         * grounded, 1.7–2.5s.
         *
         * Free-tier prompts may be used by Google to improve its products.
         */
        fun gemini(apiKey: String) = AiProvider(
            name = "Gemini",
            endpoint = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
            apiKey = apiKey,
            // Not 3.8 Flash: on the free tier (2026-10-25) it answered 503 "high demand" or
            // hung past 45s on most calls, and it rejects "minimal" thinking with a 400.
            model = "gemini-3.6-flash",
            // Gemini 3 can't stop thinking; "minimal" is the floor. Sampling stays at the
            // model's defaults.
            extraction = Tuning(reasoningEffort = "minimal"),
            note = Tuning(reasoningEffort = "low"),
        )

        /**
         * Fallback, on the free tier. The same test receipt came back exactly right in
         * 0.8–1.9s and recaps took ~1s, 3 of 3 grounded — but the free plan allows 7,000
         * input tokens a minute and an image alone counts as 2,048, so only about two scans
         * fit in a minute. Not yet checked against real receipts.
         */
        fun groq(apiKey: String) = AiProvider(
            name = "Groq",
            endpoint = "https://api.groq.com/openai/v1/chat/completions",
            apiKey = apiKey,
            model = "qwen/qwen3.8-27b",
            // "none" is Qwen's instruct mode. Sampling is what Groq publishes for each mode,
            // minus presence_penalty: a penalty on repeated tokens is the last thing a column
            // of prices needs.
            extraction = Tuning(temperature = 0.7, topP = 0.8, reasoningEffort = "none"),
            note = Tuning(temperature = 1.0, topP = 0.95, reasoningEffort = "low", reasoningFormat = "hidden"),
        )

        /**
         * Nemotron 3 Nano Omni: a 30B mixture-of-experts with ~3B active parameters, so it
         * reads an image in seconds rather than minutes. Reading a receipt is perception, not
         * reasoning: with `reasoning_budget` at 0 this returned the benchmark receipt exactly
         * right on 6 of 6 runs in 4.1–4.9s, against 25s with thinking on and over 10 minutes
         * for Kimi K3. Sampling is what NVIDIA publishes for the model, and what that was
         * measured at.
         *
         * The notes get 512 tokens of thinking. Working out *why* two people owe different
         * amounts is relational: with no budget at all the model read shared dishes as one
         * person's order and explained a one-rupiah rounding remainder as someone eating less.
         * 512 fixed both across repeated runs, for about 3 extra seconds.
         */
        fun nvidia(apiKey: String) = AiProvider(
            name = "NVIDIA",
            endpoint = "https://integrate.api.nvidia.com/v1/chat/completions",
            apiKey = apiKey,
            model = "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning",
            extraction = Tuning(temperature = 0.6, topP = 0.95, reasoningBudget = 0),
            note = Tuning(temperature = 0.6, topP = 0.95, reasoningBudget = 512),
        )
    }
}

/** Per-call request knobs; null leaves the field out so a provider that rejects it never sees it. */
internal data class Tuning(
    val temperature: Double? = null,
    val topP: Double? = null,
    val reasoningBudget: Int? = null,
    val reasoningEffort: String? = null,
    val reasoningFormat: String? = null,
)
