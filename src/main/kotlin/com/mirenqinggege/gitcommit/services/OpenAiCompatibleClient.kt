package com.mirenqinggege.gitcommit.services

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.completions.CompletionCreateParams
import com.openai.models.responses.ResponseCreateParams
import com.mirenqinggege.gitcommit.CommitPromptBuilder
import com.mirenqinggege.gitcommit.GitCommitMessageBundle
import com.mirenqinggege.gitcommit.settings.ApiType
import com.mirenqinggege.gitcommit.settings.GitCommitMessageSettings
import java.net.URI

class OpenAiCompatibleClient(private val settings: GitCommitMessageSettings) {
    fun generate(diff: String): String {
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
        val client = OpenAIOkHttpClient.builder()
            .apiKey(settings.apiKey)
            .baseUrl(baseUrl)
            .build()

        return when (settings.apiType) {
            ApiType.COMPLETIONS -> client.completions().create(
                CompletionCreateParams.builder()
                    .model(settings.model)
                    .prompt(prompt)
                    .build()
            ).choices().firstOrNull()?.text()?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: error(GitCommitMessageBundle.message("error.response.empty"))

            ApiType.RESPONSES -> client.responses().create(
                ResponseCreateParams.builder()
                    .model(settings.model)
                    .input(prompt)
                    .build()
            ).output().asSequence()
                .flatMap { it.message().orElse(null)?.content().orEmpty().asSequence() }
                .mapNotNull { it.outputText().orElse(null)?.text() }
                .joinToString("")
                .trim()
                .takeIf { it.isNotBlank() }
                ?: error(GitCommitMessageBundle.message("error.response.empty"))
        }
    }

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
