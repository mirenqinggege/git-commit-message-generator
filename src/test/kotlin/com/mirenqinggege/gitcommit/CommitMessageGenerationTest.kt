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
    fun `english prompt asks the AI for an English message`() {
        val prompt = GitCommitMessageBundle.message("prompt.template.en", "dummy")

        assertEquals(true, prompt.contains("Write the commit message in English"))
        assertEquals(true, prompt.contains("no Markdown code fences"))
    }

    @Test
    fun `chinese prompt asks the AI for a Chinese message`() {
        val prompt = GitCommitMessageBundle.message("prompt.template.zh", "dummy")

        assertEquals(true, prompt.contains("请用中文编写提交消息"))
        assertEquals(true, prompt.contains("仅返回一条提交消息，不要 Markdown 代码围栏、解释或前缀"))
    }

    @Test
    fun `builds a diff command limited to selected files`() {
        assertEquals(
            listOf("git", "diff", "HEAD", "--", "src/main/App.kt", "README.md"),
            GitDiffCommandBuilder.arguments(listOf("src/main/App.kt", "README.md"))
        )
    }
}
