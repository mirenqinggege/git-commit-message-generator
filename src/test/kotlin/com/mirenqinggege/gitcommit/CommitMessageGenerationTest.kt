package com.mirenqinggege.gitcommit

import org.junit.Assert.assertEquals
import org.junit.Test

class CommitMessageGenerationTest {

    @Test
    fun `builds a prompt that includes the diff and commit rules`() {
        val prompt = CommitPromptBuilder.build("diff --git a/a.kt b/a.kt\n+return true")

        assertEquals(true, prompt.contains("diff --git a/a.kt b/a.kt"))
        assertEquals(true, prompt.contains("Conventional Commits"))
        assertEquals(true, prompt.contains("<DIFF_START>"))
        assertEquals(true, prompt.contains("<DIFF_END>"))
    }

    @Test
    fun `builds a diff command limited to selected files`() {
        assertEquals(
            listOf("git", "diff", "HEAD", "--", "src/main/App.kt", "README.md"),
            GitDiffCommandBuilder.arguments(listOf("src/main/App.kt", "README.md"))
        )
    }

    @Test
    fun `verifies chat completion params and disable thinking configuration`() {
        val chatParams = com.openai.models.chat.completions.ChatCompletionCreateParams.builder()
            .model("gpt-4o")
            .addUserMessage("test prompt")
            .reasoningEffort(com.openai.models.ReasoningEffort.NONE)
            .putAdditionalBodyProperty("reasoning_effort", com.openai.core.JsonValue.from("none"))
            .putAdditionalBodyProperty("enable_thinking", com.openai.core.JsonValue.from(false))
            .putAdditionalBodyProperty("thinking", com.openai.core.JsonValue.from(mapOf("type" to "disabled")))
            .build()

        assertEquals("none", chatParams.reasoningEffort().get().asString())
        assertEquals(
            com.openai.core.JsonValue.from(false),
            chatParams._additionalBodyProperties()["enable_thinking"]
        )

        val responseParams = com.openai.models.responses.ResponseCreateParams.builder()
            .model("gpt-4o")
            .input("test prompt")
            .reasoning(com.openai.models.Reasoning.builder().effort(com.openai.models.ReasoningEffort.NONE).build())
            .putAdditionalBodyProperty("reasoning_effort", com.openai.core.JsonValue.from("none"))
            .putAdditionalBodyProperty("enable_thinking", com.openai.core.JsonValue.from(false))
            .putAdditionalBodyProperty("thinking", com.openai.core.JsonValue.from(mapOf("type" to "disabled")))
            .build()

        assertEquals("none", responseParams.reasoning().get().effort().get().asString())
        assertEquals(
            com.openai.core.JsonValue.from(false),
            responseParams._additionalBodyProperties()["enable_thinking"]
        )
    }
}
