package com.mirenqinggege.gitcommit.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.vcs.commit.CommitWorkflowUi
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.mirenqinggege.gitcommit.GitDiffCommandBuilder
import com.mirenqinggege.gitcommit.services.OpenAiCompatibleClient
import com.mirenqinggege.gitcommit.settings.GitCommitMessageSettings
import com.mirenqinggege.gitcommit.GitCommitMessageBundle

class GenerateCommitMessageAction : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val message = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) as? CommitMessage
        e.presentation.isVisible = message != null
        e.presentation.isEnabled = message != null && e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val commitMessage = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) as? CommitMessage ?: return
        val selectedChanges = includedChanges(e)
        if (selectedChanges.isEmpty()) {
            NotificationGroupManager.getInstance().getNotificationGroup("Git Commit Message Generator")
                .createNotification(
                    GitCommitMessageBundle.message("notification.no.selected.changes"),
                    NotificationType.WARNING
                )
                .notify(project)
            return
        }
        val projectBasePath = project.basePath
        if (projectBasePath == null) {
            NotificationGroupManager.getInstance().getNotificationGroup("Git Commit Message Generator")
                .createNotification(
                    GitCommitMessageBundle.message("notification.project.path.missing"),
                    NotificationType.ERROR
                )
                .notify(project)
            return
        }
        val selectedPaths = selectedChanges.mapNotNull { it.relativePath(projectBasePath) }.distinct()
        if (selectedPaths.isEmpty()) {
            NotificationGroupManager.getInstance().getNotificationGroup("Git Commit Message Generator")
                .createNotification(
                    GitCommitMessageBundle.message("notification.no.selected.changes"),
                    NotificationType.WARNING
                )
                .notify(project)
            return
        }
        val settings = ApplicationManager.getApplication().getService(GitCommitMessageSettings::class.java)
        object : Task.Backgroundable(project, GitCommitMessageBundle.message("progress.generating"), true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    ApplicationManager.getApplication().invokeLater { commitMessage.setCommitMessage("") }
                    indicator.text = GitCommitMessageBundle.message("progress.reading.diff")
                    val command = GeneralCommandLine(*GitDiffCommandBuilder.arguments(selectedPaths).toTypedArray())
                        .withWorkDirectory(projectBasePath)
                    val process = CapturingProcessHandler(command).runProcess()
                    if (process.exitCode != 0) error(process.stderr.ifBlank { "Git diff failed (${process.exitCode})" })
                    val diff = process.stdout
                    indicator.text = GitCommitMessageBundle.message("progress.asking.ai")
                    val result = StringBuilder()
                    OpenAiCompatibleClient(settings).generateStreaming(diff) { delta ->
                        if (indicator.isCanceled) return@generateStreaming
                        result.append(delta)
                        val currentMessage = result.toString()
                        ApplicationManager.getApplication().invokeLater {
                            if (!indicator.isCanceled) commitMessage.setCommitMessage(currentMessage)
                        }
                    }
                } catch (error: Exception) {
                    NotificationGroupManager.getInstance().getNotificationGroup("Git Commit Message Generator")
                        .createNotification(GitCommitMessageBundle.message("notification.generate.failed", error.message.orEmpty()), NotificationType.ERROR)
                        .notify(project)
                }
            }
        }.queue()
    }
}

private fun includedChanges(e: AnActionEvent): List<Change> =
    (e.getData(VcsDataKeys.COMMIT_WORKFLOW_UI) as? CommitWorkflowUi)?.getIncludedChanges()
        ?: e.getData(VcsDataKeys.CHANGES)?.toList().orEmpty()

private fun Change.relativePath(projectBasePath: String): String? {
    val path = virtualFile?.path ?: beforeRevision?.file?.path ?: afterRevision?.file?.path ?: return null
    val basePath = projectBasePath.trimEnd('/') + "/"
    return path.removePrefix(basePath).takeIf { it != path && it.isNotBlank() }
}
