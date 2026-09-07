package com.mirenqinggege.gitcommit.actions

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.vcs.commit.CommitWorkflowUi
import com.intellij.ide.ActivityTracker
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.util.IconLoader
import com.mirenqinggege.gitcommit.GitDiffResolver
import com.mirenqinggege.gitcommit.services.OpenAiCompatibleClient
import com.mirenqinggege.gitcommit.settings.GitCommitMessageSettings
import com.mirenqinggege.gitcommit.GitCommitMessageBundle
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

class GenerateCommitMessageAction : DumbAwareAction() {
    private val runningTasks = ConcurrentHashMap<Project, RunningTask>()

    companion object {
        val GENERATE_ICON = IconLoader.getIcon("/icons/generateCommit.svg", GenerateCommitMessageAction::class.java)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val message = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) as? CommitMessage
        if (project == null || message == null) {
            e.presentation.isVisible = false
            e.presentation.isEnabled = false
            return
        }
        e.presentation.isVisible = true

        val isRunning = runningTasks.containsKey(project)
        if (isRunning) {
            e.presentation.isEnabled = true
            e.presentation.text = GitCommitMessageBundle.message("action.stop.text")
            e.presentation.description = GitCommitMessageBundle.message("action.stop.description")
            e.presentation.icon = AllIcons.Actions.Suspend
        } else {
            e.presentation.text = GitCommitMessageBundle.message("action.generate.text")
            e.presentation.description = GitCommitMessageBundle.message("action.generate.description")
            e.presentation.icon = GENERATE_ICON
            val selectedChanges = includedChanges(e)
            e.presentation.isEnabled = selectedChanges.isNotEmpty()
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val commitMessage = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) as? CommitMessage ?: return

        val existingTask = runningTasks[project]
        if (existingTask != null) {
            existingTask.cancel()
            runningTasks.remove(project)
            refreshToolbars()
            return
        }

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
        val changesByRoot = GitDiffResolver.groupChangesByRoot(project, selectedChanges)
        if (changesByRoot.isEmpty()) {
            NotificationGroupManager.getInstance().getNotificationGroup("Git Commit Message Generator")
                .createNotification(
                    GitCommitMessageBundle.message("notification.git.root.missing"),
                    NotificationType.WARNING
                )
                .notify(project)
            return
        }
        val settings = ApplicationManager.getApplication().getService(GitCommitMessageSettings::class.java)
        val task = RunningTask()
        runningTasks[project] = task
        refreshToolbars()

        task.future = ApplicationManager.getApplication().executeOnPooledThread {
            try {
                ApplicationManager.getApplication().invokeLater {
                    if (!task.isCancelled) {
                        commitMessage.setCommitMessage("")
                    }
                }
                val diff = GitDiffResolver.collectDiff(changesByRoot)
                if (task.isCancelled) return@executeOnPooledThread
                if (diff.isBlank()) {
                    error(GitCommitMessageBundle.message("error.diff.empty"))
                }
                val result = StringBuilder()
                OpenAiCompatibleClient(settings).generateStreaming(diff, isCancelled = { task.isCancelled }) { delta ->
                    if (task.isCancelled) return@generateStreaming
                    result.append(delta)
                    val currentMessage = result.toString()
                    ApplicationManager.getApplication().invokeLater {
                        if (!task.isCancelled) {
                            commitMessage.setCommitMessage(currentMessage)
                        }
                    }
                }
            } catch (error: Throwable) {
                if (!task.isCancelled) {
                    NotificationGroupManager.getInstance().getNotificationGroup("Git Commit Message Generator")
                        .createNotification(
                            GitCommitMessageBundle.message("notification.generate.failed", error.message.orEmpty()),
                            NotificationType.ERROR
                        )
                        .notify(project)
                }
            } finally {
                runningTasks.remove(project, task)
                refreshToolbars()
            }
        }
    }

    private fun refreshToolbars() {
        ApplicationManager.getApplication().invokeLater {
            ActivityTracker.getInstance().inc()
            runCatching { ActionToolbarImpl.updateAllToolbarsImmediately() }
        }
    }

    private class RunningTask(
        val cancelFlag: AtomicBoolean = AtomicBoolean(false),
        @Volatile var future: Future<*>? = null
    ) {
        fun cancel() {
            cancelFlag.set(true)
            future?.cancel(true)
        }
        val isCancelled: Boolean get() = cancelFlag.get()
    }
}

private fun includedChanges(e: AnActionEvent): List<Change> =
    (e.getData(VcsDataKeys.COMMIT_WORKFLOW_UI) as? CommitWorkflowUi)?.getIncludedChanges()
        ?: e.getData(VcsDataKeys.CHANGES)?.toList().orEmpty()

