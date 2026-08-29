package com.mirenqinggege.gitcommit

import com.mirenqinggege.gitcommit.services.applyThinking
import com.openai.core.JsonValue
import com.openai.models.ReasoningEffort
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.responses.ResponseCreateParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun `disabling thinking adds no-reasoning parameters to chat completion params`() {
        val chatParams = ChatCompletionCreateParams.builder()
            .model("gpt-4o")
            .addUserMessage("test prompt")
            .applyThinking(enableThinking = false)
            .build()

        assertEquals(ReasoningEffort.NONE, chatParams.reasoningEffort().get())
        assertEquals(JsonValue.from(false), chatParams._additionalBodyProperties()["enable_thinking"])
        assertEquals(
            JsonValue.from(mapOf("type" to "disabled")),
            chatParams._additionalBodyProperties()["thinking"]
        )
    }

    @Test
    fun `disabling thinking adds no-reasoning parameters to response params`() {
        val responseParams = ResponseCreateParams.builder()
            .model("gpt-4o")
            .input("test prompt")
            .applyThinking(enableThinking = false)
            .build()

        assertEquals(ReasoningEffort.NONE, responseParams.reasoning().get().effort().get())
        assertEquals(JsonValue.from(false), responseParams._additionalBodyProperties()["enable_thinking"])
        assertEquals(
            JsonValue.from(mapOf("type" to "disabled")),
            responseParams._additionalBodyProperties()["thinking"]
        )
    }

    @Test
    fun `enabling thinking leaves request parameters untouched`() {
        val chatParams = ChatCompletionCreateParams.builder()
            .model("gpt-4o")
            .addUserMessage("test prompt")
            .applyThinking(enableThinking = true)
            .build()

        assertTrue(chatParams.reasoningEffort().isEmpty)
        assertFalse(chatParams._additionalBodyProperties().containsKey("enable_thinking"))

        val responseParams = ResponseCreateParams.builder()
            .model("gpt-4o")
            .input("test prompt")
            .applyThinking(enableThinking = true)
            .build()

        assertTrue(responseParams.reasoning().isEmpty)
        assertFalse(responseParams._additionalBodyProperties().containsKey("enable_thinking"))
    }
}
