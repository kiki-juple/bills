package com.disheveled.bills.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/*
 * Wire types for the OpenAI chat-completions schema, which every [AiProvider] speaks: images
 * ride in an `image_url` block as a data URI and the JSON schema goes in `response_format`.
 * Hand-rolled rather than pulled from an SDK because this module also compiles for iOS.
 */

@Serializable
internal data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens") val maxTokens: Int,
    @SerialName("response_format") val responseFormat: ResponseFormat? = null,
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null,
    /**
     * How many tokens the model may spend thinking before it answers. Nemotron reasons by
     * default; 0 turns that off. Left null for models that reject the field.
     */
    @SerialName("reasoning_budget") val reasoningBudget: Int? = null,
    /** Gemini's and Groq's thinking control, a level instead of a token count. */
    @SerialName("reasoning_effort") val reasoningEffort: String? = null,
    /** Groq only: "hidden" keeps the chain of thought out of the response entirely. */
    @SerialName("reasoning_format") val reasoningFormat: String? = null,
    val stream: Boolean = false,
)

@Serializable
internal data class ChatMessage(
    val role: String,
    val content: List<ContentPart>,
)

/**
 * One part of a multimodal message. Union-by-nullable-field: the API discriminates on
 * `type`, and polymorphic serialization would only add ceremony for two part kinds.
 */
@Serializable
internal data class ContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: ImageUrl? = null,
) {
    companion object {
        fun text(value: String) = ContentPart(type = "text", text = value)

        /** NIM takes the bytes inline as a data URI rather than a separate source object. */
        fun image(base64: String, mediaType: String) = ContentPart(
            type = "image_url",
            imageUrl = ImageUrl("data:$mediaType;base64,$base64"),
        )
    }
}

@Serializable
internal data class ImageUrl(val url: String)

@Serializable
internal data class ResponseFormat(
    val type: String = "json_schema",
    @SerialName("json_schema") val jsonSchema: JsonSchemaSpec,
)

@Serializable
internal data class JsonSchemaSpec(
    val name: String,
    val strict: Boolean = true,
    val schema: JsonObject,
)

@Serializable
internal data class ChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<Choice> = emptyList(),
) {
    val first: Choice? get() = choices.firstOrNull()
}

@Serializable
internal data class Choice(
    val index: Int = 0,
    val message: ResponseMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
internal data class ResponseMessage(
    val role: String? = null,
    /** The answer. Only this field is schema-constrained. */
    val content: String? = null,
    /**
     * Where a reasoning model puts its chain of thought. It is not schema-bound, so parsing
     * it as the receipt would fail — read [content] and nothing else. Empty once
     * [ChatRequest.reasoningBudget] is 0, but still returned by the API.
     */
    @SerialName("reasoning_content") val reasoningContent: String? = null,
)

/** NIM returns OpenAI-style `error` objects for some failures and a bare `detail` for others. */
@Serializable
internal data class ApiErrorEnvelope(
    val error: ApiError? = null,
    val detail: String? = null,
) {
    val message: String? get() = error?.message ?: detail
}

@Serializable
internal data class ApiError(val type: String? = null, val message: String? = null)
