package com.mirenqinggege.gitcommit.services

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mirenqinggege.gitcommit.CommitPromptBuilder
import com.mirenqinggege.gitcommit.OpenAiResponseParser
import com.mirenqinggege.gitcommit.settings.ApiType
import com.mirenqinggege.gitcommit.settings.GitCommitMessageSettings
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class OpenAiCompatibleClient(private val settings: GitCommitMessageSettings) {
    private val httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()

    fun generate(diff: String): String {
        require(settings.baseUrl.isNotBlank()) { "Base URL is not configured" }
        require(settings.apiKey.isNotBlank()) { "API key is not configured" }
        require(diff.isNotBlank()) { "There are no Git changes to analyze" }

        val prompt = CommitPromptBuilder.build(diff)
        val payload = JsonObject().apply {
            addProperty("model", settings.model)
            when (settings.apiType) {
                ApiType.COMPLETIONS -> addProperty("prompt", prompt)
                ApiType.RESPONSES -> addProperty("input", prompt)
            }
        }
        val endpoint = settings.baseUrl.trimEnd('/') + when (settings.apiType) {
            ApiType.COMPLETIONS -> "/completions"
            ApiType.RESPONSES -> "/responses"
        }
        val request = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(90))
            .header("Authorization", "Bearer ${settings.apiKey}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) error("AI request failed (${response.statusCode()}): ${response.body().take(300)}")
        return OpenAiResponseParser.parse(response.body())
    }
}
