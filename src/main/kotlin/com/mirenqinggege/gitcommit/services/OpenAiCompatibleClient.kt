package com.mirenqinggege.gitcommit.services

import com.mirenqinggege.gitcommit.CommitPromptBuilder
import com.mirenqinggege.gitcommit.GitCommitMessageBundle
import com.mirenqinggege.gitcommit.settings.ApiType
import com.mirenqinggege.gitcommit.settings.GitCommitMessageSettings
import com.mirenqinggege.gitcommit.settings.ReasoningLevel
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.Reasoning
import com.openai.models.ReasoningEffort
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.models.Model
import com.openai.models.responses.ResponseCreateParams
import com.openai.models.responses.ResponseStreamEvent
import java.net.URI

data class OpenAiClientSettings(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val apiType: ApiType,
    val reasoningLevel: ReasoningLevel = ReasoningLevel.LOW
)

class OpenAiCompatibleClient(private val settings: OpenAiClientSettings) {
    constructor(settings: GitCommitMessageSettings) : this(
        OpenAiClientSettings(settings.baseUrl, settings.apiKey, settings.model, settings.apiType, settings.reasoningLevel)
    )

    fun testConnection() {
        val prompt = "Reply with one token."
        val client = createClient()
        when (settings.apiType) {
            ApiType.COMPLETIONS -> client.chat().completions().create(
                ChatCompletionCreateParams.builder()
                    .model(settings.model)
                    .addUserMessage(prompt)
                    .maxCompletionTokens(1)
                    .applyReasoningLevel(settings.reasoningLevel)
                    .build()
            )

            ApiType.RESPONSES -> client.responses().create(
                ResponseCreateParams.builder()
                    .model(settings.model)
                    .input(prompt)
                    .maxOutputTokens(1)
                    .applyReasoningLevel(settings.reasoningLevel)
                    .build()
            )
        }
    }

    fun listModels(): List<String> = createClient().models().list().data().map(Model::id)

    fun generateStreaming(
        diff: String,
        isCancelled: () -> Boolean = { false },
        onDelta: (String) -> Unit
    ): String {
        require(settings.baseUrl.isNotBlank()) { GitCommitMessageBundle.message("error.base.url.missing") }
        require(settings.apiKey.isNotBlank()) { GitCommitMessageBundle.message("error.api.key.missing") }
        require(diff.isNotBlank()) { GitCommitMessageBundle.message("error.diff.empty") }

        val prompt = CommitPromptBuilder.build(diff)
        val baseUrl = settings.baseUrl.trimEnd('/')
        val endpoint = baseUrl + when (settings.apiType) {
            ApiType.COMPLETIONS -> "/chat/completions"
            ApiType.RESPONSES -> "/responses"
        }
        validateEndpoint(endpoint)
        val client = createClient()

        val generated = StringBuilder()
        fun appendDelta(delta: String) {
            if (delta.isEmpty() || isCancelled()) return
            generated.append(delta)
            onDelta(delta)
        }

        when (settings.apiType) {
            ApiType.COMPLETIONS -> client.chat().completions().createStreaming(
                ChatCompletionCreateParams.builder()
                    .model(settings.model)
                    .addUserMessage(prompt)
                    .applyReasoningLevel(settings.reasoningLevel)
                    .build()
            ).use { stream ->
                val iterator = stream.stream().iterator()
                while (iterator.hasNext()) {
                    if (isCancelled()) break
                    val chunk = iterator.next()
                    chunk.choices().firstOrNull()?.delta()?.content()?.ifPresent(::appendDelta)
                }
            }

            ApiType.RESPONSES -> client.responses().createStreaming(
                ResponseCreateParams.builder()
                    .model(settings.model)
                    .input(prompt)
                    .applyReasoningLevel(settings.reasoningLevel)
                    .build()
            ).use { stream ->
                val iterator = stream.stream().iterator()
                while (iterator.hasNext()) {
                    if (isCancelled()) break
                    val event = iterator.next()
                    event.outputTextDelta().ifPresent { appendDelta(it.delta()) }
                }
            }
        }
        if (isCancelled()) {
            return generated.toString().trim()
        }
        return generated.toString().trim().takeIf { it.isNotBlank() }
            ?: error(GitCommitMessageBundle.message("error.response.empty"))
    }

    private fun createClient() = OpenAIOkHttpClient.builder()
        .apiKey(settings.apiKey)
        .baseUrl(settings.baseUrl.trimEnd('/'))
        .build()

    /**
     * Validates the configured endpoint before any request is made. The request carries an
     * Authorization header with the user's API key and full diff content, so it must never be
     * sent to anything but an authenticated https endpoint. This rejects http, endpoints
     * without a well-formed host, and URLs carrying embedded credentials/userinfo.
     */
    private fun validateEndpoint(endpoint: String): URI {
        val uri = URI.create(endpoint)
        require(uri.scheme.equals("https", ignoreCase = true)) { "Base URL must use https" }
        require(!uri.host.isNullOrBlank() && uri.host.contains(".")) { "Base URL has no valid host" }
        require(uri.userInfo == null) { "Base URL must not contain embedded credentials" }
        return uri
    }
}

internal fun ChatCompletionCreateParams.Builder.applyReasoningLevel(level: ReasoningLevel): ChatCompletionCreateParams.Builder =
    apply { reasoningEffort(level.toOpenAiReasoningEffort()) }

internal fun ResponseCreateParams.Builder.applyReasoningLevel(level: ReasoningLevel): ResponseCreateParams.Builder =
    apply { reasoning(Reasoning.builder().effort(level.toOpenAiReasoningEffort()).build()) }

private fun ReasoningLevel.toOpenAiReasoningEffort(): ReasoningEffort = when (this) {
    ReasoningLevel.MINIMAL -> ReasoningEffort.MINIMAL
    ReasoningLevel.LOW -> ReasoningEffort.LOW
    ReasoningLevel.MEDIUM -> ReasoningEffort.MEDIUM
    ReasoningLevel.HIGH -> ReasoningEffort.HIGH
}
