package com.mirenqinggege.gitcommit.services

import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.completions.CompletionCreateParams
import com.openai.models.responses.ResponseCreateParams
import com.openai.models.responses.ResponseStreamEvent
import com.openai.models.models.Model
import com.mirenqinggege.gitcommit.CommitPromptBuilder
import com.mirenqinggege.gitcommit.GitCommitMessageBundle
import com.mirenqinggege.gitcommit.settings.ApiType
import com.mirenqinggege.gitcommit.settings.GitCommitMessageSettings
import java.net.URI

data class OpenAiClientSettings(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val apiType: ApiType
)

class OpenAiCompatibleClient(private val settings: OpenAiClientSettings) {
    constructor(settings: GitCommitMessageSettings) : this(
        OpenAiClientSettings(settings.baseUrl, settings.apiKey, settings.model, settings.apiType)
    )

    fun testConnection() {
        val prompt = "Reply with one token."
        val client = createClient()
        when (settings.apiType) {
            ApiType.COMPLETIONS -> client.completions().create(
                CompletionCreateParams.builder()
                    .model(settings.model)
                    .prompt(prompt)
                    .maxTokens(1)
                    .build()
            )
            ApiType.RESPONSES -> client.responses().create(
                ResponseCreateParams.builder()
                    .model(settings.model)
                    .input(prompt)
                    .maxOutputTokens(1)
                    .build()
            )
        }
    }

    fun listModels(): List<String> = createClient().models().list().data().map(Model::id)

    fun generateStreaming(diff: String, onDelta: (String) -> Unit): String {
        require(settings.baseUrl.isNotBlank()) { GitCommitMessageBundle.message("error.base.url.missing") }
        require(settings.apiKey.isNotBlank()) { GitCommitMessageBundle.message("error.api.key.missing") }
        require(diff.isNotBlank()) { GitCommitMessageBundle.message("error.diff.empty") }

        val prompt = CommitPromptBuilder.build(diff)
        val baseUrl = settings.baseUrl.trimEnd('/')
        val endpoint = baseUrl + when (settings.apiType) {
            ApiType.COMPLETIONS -> "/completions"
            ApiType.RESPONSES -> "/responses"
        }
        validateEndpoint(endpoint)
        val client = createClient()

        val generated = StringBuilder()
        fun appendDelta(delta: String) {
            if (delta.isEmpty()) return
            generated.append(delta)
            onDelta(delta)
        }

        when (settings.apiType) {
            ApiType.COMPLETIONS -> client.completions().createStreaming(
                CompletionCreateParams.builder()
                    .model(settings.model)
                    .prompt(prompt)
                    .build()
            ).use { stream ->
                stream.stream().forEach { chunk ->
                    chunk.choices().firstOrNull()?.text()?.let(::appendDelta)
                }
            }

            ApiType.RESPONSES -> client.responses().createStreaming(
                ResponseCreateParams.builder()
                    .model(settings.model)
                    .input(prompt)
                    .build()
            ).use { stream ->
                stream.stream().forEach { event: ResponseStreamEvent ->
                    event.outputTextDelta().ifPresent { appendDelta(it.delta()) }
                }
            }
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
