package com.mirenqinggege.gitcommit

import org.junit.Assert.assertEquals
import org.junit.Test

class CommitMessageGenerationTest {

    @Test
    fun `builds a prompt that includes the diff and conventional commit rules`() {
        val prompt = CommitPromptBuilder.build("diff --git a/a.kt b/a.kt\n+return true")

        assertEquals(true, prompt.contains("diff --git a/a.kt b/a.kt"))
        assertEquals(true, prompt.contains("Conventional Commits"))
        assertEquals(true, prompt.contains("仅返回提交消息"))
    }

    @Test
    fun `extracts text from completions response`() {
        val response = """{"choices":[{"text":" feat: add commit message generator "}]}"""

        assertEquals("feat: add commit message generator", OpenAiResponseParser.parse(response))
    }

    @Test
    fun `extracts text from responses output response`() {
        val response = """{"output":[{"type":"message","content":[{"type":"output_text","text":"fix: handle empty diff"}]}]}"""

        assertEquals("fix: handle empty diff", OpenAiResponseParser.parse(response))
    }
}
