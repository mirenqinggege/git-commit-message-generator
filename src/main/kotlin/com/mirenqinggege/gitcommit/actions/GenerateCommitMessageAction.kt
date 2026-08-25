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
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.mirenqinggege.gitcommit.services.OpenAiCompatibleClient
import com.mirenqinggege.gitcommit.settings.GitCommitMessageSettings

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
        val settings = ApplicationManager.getApplication().getService(GitCommitMessageSettings::class.java)
        object : Task.Backgroundable(project, "Generating commit message", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Reading Git diff"
                    val diff = CapturingProcessHandler(GeneralCommandLine("git", "diff", "HEAD").withWorkDirectory(project.basePath)).runProcess().stdout
                    indicator.text = "Asking AI"
                    val result = OpenAiCompatibleClient(settings).generate(diff)
                    ApplicationManager.getApplication().invokeLater { commitMessage.setCommitMessage(result) }
                } catch (error: Exception) {
                    NotificationGroupManager.getInstance().getNotificationGroup("Git Commit Message Generator")
                        .createNotification("Could not generate commit message: ${error.message}", NotificationType.ERROR)
                        .notify(project)
                }
            }
        }.queue()
    }
}
