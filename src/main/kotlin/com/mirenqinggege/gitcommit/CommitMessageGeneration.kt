package com.mirenqinggege.gitcommit

object CommitPromptBuilder {
    private const val TEMPLATE = "prompt.template"

    /**
     * Builds the prompt for the current IDE language.
     * The prompt template comes from the message resource bundle (resolved by the IDE locale),
     * and instructs the AI to write the commit message in that same language.
     */
    fun build(diff: String): String {

        return GitCommitMessageBundle.message(TEMPLATE, diff)
    }

}

object GitDiffCommandBuilder {
    fun arguments(selectedPaths: List<String>): List<String> =
        listOf("git", "diff", "HEAD", "--") + selectedPaths
}
