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
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class CommitMessageGenerationTest {

    @Test
    fun `builds a prompt that includes the diff and commit rules`() {
        val prompt = CommitPromptBuilder.build("diff --git a/a.kt b/a.kt\n+return true")

        assertEquals(true, prompt.contains("diff --git a/a.kt b/a.kt"))
        assertEquals(true, prompt.contains("Conventional Commits"))
        assertEquals(true, prompt.contains("<DIFF_START>"))
        assertEquals(true, prompt.contains("<DIFF_END>"))
        assertEquals(true, prompt.contains("required for every non-trivial change"))
        assertEquals(true, prompt.contains("2-5 concise bullet points"))
    }

    @Test
    fun `builds a diff command limited to selected files`() {
        assertEquals(
            listOf("git", "diff", "HEAD", "--", "src/main/App.kt", "README.md"),
            GitDiffCommandBuilder.arguments(listOf("src/main/App.kt", "README.md"))
        )
    }

    @Test
    fun `registers the JetBrains Marketplace error report handler`() {
        val pluginXml = checkNotNull(javaClass.getResourceAsStream("/META-INF/plugin.xml"))
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pluginXml)
        val errorHandlers = document.getElementsByTagName("errorHandler")

        assertEquals(1, errorHandlers.length)
        assertEquals(
            "com.intellij.diagnostic.JetBrainsMarketplaceErrorReportSubmitter",
            (errorHandlers.item(0) as Element).getAttribute("implementation")
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

    @Test
    fun `computes relative path correctly under git root`() {
        val root = java.io.File("/workspace/sub-repo")
        val file1 = java.io.File("/workspace/sub-repo/src/main/App.kt")
        val file2 = java.io.File("/workspace/sub-repo/README.md")
        val outsideFile = java.io.File("/workspace/other-repo/src/App.kt")

        assertEquals("src/main/App.kt", GitDiffResolver.computeRelativePath(root, file1))
        assertEquals("README.md", GitDiffResolver.computeRelativePath(root, file2))
        assertEquals(null, GitDiffResolver.computeRelativePath(root, outsideFile))
    }

    @Test
    fun `finds git root by traversing ancestor directories`() {
        val tempDir = java.nio.file.Files.createTempDirectory("git-test-repo").toFile()
        try {
            val dotGit = java.io.File(tempDir, ".git")
            dotGit.mkdir()
            val subDir = java.io.File(tempDir, "sub/package")
            subDir.mkdirs()
            val targetFile = java.io.File(subDir, "Test.kt")
            targetFile.writeText("test")

            val foundRoot = GitDiffResolver.findGitRoot(targetFile)
            assertEquals(tempDir.canonicalFile, foundRoot?.canonicalFile)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `combines diff from single repo without repo header`() {
        val repo = java.io.File("/workspace/my-repo")
        val diffMap = mapOf(repo to "diff --git a/a.kt b/a.kt\n+val x = 1")
        val combined = GitDiffResolver.combineDiffs(diffMap)

        assertEquals("diff --git a/a.kt b/a.kt\n+val x = 1", combined)
    }

    @Test
    fun `combines diffs from multiple repos with repo headers`() {
        val repoA = java.io.File("/workspace/repo-a")
        val repoB = java.io.File("/workspace/repo-b")
        val diffMap = linkedMapOf(
            repoA to "diff --git a/a.kt b/a.kt\n+val a = 1",
            repoB to "diff --git a/b.kt b/b.kt\n+val b = 2"
        )
        val combined = GitDiffResolver.combineDiffs(diffMap)

        val expected = """
            # Repository: repo-a
            diff --git a/a.kt b/a.kt
            +val a = 1

            # Repository: repo-b
            diff --git a/b.kt b/b.kt
            +val b = 2
        """.trimIndent()

        assertEquals(expected, combined)
    }

    @Test
    fun `collectDiff aggregates diff from diffRunner for multiple roots`() {
        val repoA = java.io.File("/workspace/repo-a")
        val repoB = java.io.File("/workspace/repo-b")
        val changes = linkedMapOf(
            repoA to listOf("a.kt"),
            repoB to listOf("b.kt")
        )

        val combined = GitDiffResolver.collectDiff(changes) { root, paths ->
            "diff for ${root.name}: ${paths.joinToString()}"
        }

        val expected = """
            # Repository: repo-a
            diff for repo-a: a.kt

            # Repository: repo-b
            diff for repo-b: b.kt
        """.trimIndent()

        assertEquals(expected, combined)
    }

    @Test
    fun `stop action texts are configured in message bundles`() {
        assertEquals("Stop Generating Commit Message", GitCommitMessageBundle.message("action.stop.text"))
        assertEquals("Stop generating the commit message", GitCommitMessageBundle.message("action.stop.description"))
    }

    @Test
    fun `activity tracker and toolbar update are available on EDT`() {
        javax.swing.SwingUtilities.invokeAndWait {
            com.intellij.ide.ActivityTracker.getInstance().inc()
            com.intellij.openapi.actionSystem.impl.ActionToolbarImpl.updateAllToolbarsImmediately()
        }
    }

    @Test
    fun `custom icons are available and valid`() {
        val pluginIconResource = javaClass.getResource("/META-INF/pluginIcon.svg")
        org.junit.Assert.assertNotNull("META-INF/pluginIcon.svg must exist", pluginIconResource)

        val buttonIconResource = javaClass.getResource("/icons/generateCommit.svg")
        org.junit.Assert.assertNotNull("icons/generateCommit.svg must exist", buttonIconResource)

        val icon = com.mirenqinggege.gitcommit.actions.GenerateCommitMessageAction.GENERATE_ICON
        org.junit.Assert.assertNotNull("GENERATE_ICON must be loaded", icon)
    }
}

