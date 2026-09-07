package com.mirenqinggege.gitcommit

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangesUtil
import java.io.File

object GitDiffResolver {

    /**
     * Resolves the Git root directory and the relative file path for a given change.
     */
    fun resolveChange(project: Project, change: Change): Pair<File, String>? {
        val filePath = ChangesUtil.getFilePath(change)
        val file = filePath.ioFile

        // 1. Try to obtain the registered VCS root from IntelliJ's ProjectLevelVcsManager
        val vcsRoot = ProjectLevelVcsManager.getInstance(project).getVcsRootFor(filePath)
        val gitRoot = if (vcsRoot != null && vcsRoot.isInLocalFileSystem) {
            File(vcsRoot.path)
        } else {
            // 2. Fallback: traverse up directory tree to find a directory containing .git
            findGitRoot(file)
        } ?: return null

        val relativePath = computeRelativePath(gitRoot, file) ?: return null
        return gitRoot to relativePath
    }

    /**
     * Groups changes by their respective Git root directories, mapping each root to unique relative paths.
     */
    fun groupChangesByRoot(project: Project, changes: Collection<Change>): Map<File, List<String>> {
        val result = mutableMapOf<File, MutableList<String>>()
        for (change in changes) {
            val (gitRoot, relativePath) = resolveChange(project, change) ?: continue
            val list = result.getOrPut(gitRoot) { mutableListOf() }
            if (!list.contains(relativePath)) {
                list.add(relativePath)
            }
        }
        return result
    }

    /**
     * Finds the nearest ancestor directory that contains a `.git` file or folder.
     */
    fun findGitRoot(file: File): File? {
        var current: File? = if (file.isDirectory) file else file.parentFile
        while (current != null) {
            val dotGit = File(current, ".git")
            if (dotGit.exists()) {
                return current
            }
            current = current.parentFile
        }
        return null
    }

    /**
     * Computes the relative path of [file] from [gitRoot].
     * Returns a forward-slash separated path, or null if [file] is not under [gitRoot].
     */
    fun computeRelativePath(gitRoot: File, file: File): String? {
        val rootCanonical = runCatching { gitRoot.canonicalFile.invariantSeparatorsPath }.getOrNull()
            ?: gitRoot.invariantSeparatorsPath
        val fileCanonical = runCatching { file.canonicalFile.invariantSeparatorsPath }.getOrNull()
            ?: file.invariantSeparatorsPath

        val prefix = rootCanonical.trimEnd('/') + "/"
        if (fileCanonical.startsWith(prefix)) {
            val rel = fileCanonical.removePrefix(prefix)
            if (rel.isNotBlank()) return rel
        }

        // Fallback for non-canonical paths
        val rootPath = gitRoot.path.replace('\\', '/').trimEnd('/') + "/"
        val filePath = file.path.replace('\\', '/')
        if (filePath.startsWith(rootPath)) {
            val rel = filePath.removePrefix(rootPath)
            if (rel.isNotBlank()) return rel
        }

        return null
    }

    /**
     * Runs git diff in the specified working directory for the provided paths.
     */
    fun runGitDiff(workDir: File, relativePaths: List<String>): String {
        if (relativePaths.isEmpty()) return ""
        val command = GeneralCommandLine(*GitDiffCommandBuilder.arguments(relativePaths).toTypedArray())
            .withWorkDirectory(workDir)
        val process = CapturingProcessHandler(command).runProcess()
        if (process.exitCode != 0) {
            error(process.stderr.ifBlank { "Git diff failed (${process.exitCode}) in ${workDir.name}" })
        }
        return process.stdout
    }

    /**
     * Combines diff outputs from one or multiple Git repositories.
     * When changes span multiple repositories, annotates each with the repository name.
     */
    fun combineDiffs(diffsByRoot: Map<File, String>): String {
        val validDiffs = diffsByRoot.filter { it.value.isNotBlank() }
        if (validDiffs.isEmpty()) return ""
        if (validDiffs.size == 1) {
            return validDiffs.values.first().trim()
        }

        return buildString {
            var first = true
            for ((root, diff) in validDiffs) {
                if (!first) append("\n\n")
                first = false
                appendLine("# Repository: ${root.name}")
                append(diff.trim())
            }
        }.trim()
    }

    /**
     * Collects and aggregates diffs across all Git roots.
     */
    fun collectDiff(
        changesByRoot: Map<File, List<String>>,
        diffRunner: (File, List<String>) -> String = ::runGitDiff
    ): String {
        if (changesByRoot.isEmpty()) return ""
        val diffsByRoot = mutableMapOf<File, String>()
        for ((root, paths) in changesByRoot) {
            val diff = diffRunner(root, paths)
            if (diff.isNotBlank()) {
                diffsByRoot[root] = diff
            }
        }
        return combineDiffs(diffsByRoot)
    }
}
