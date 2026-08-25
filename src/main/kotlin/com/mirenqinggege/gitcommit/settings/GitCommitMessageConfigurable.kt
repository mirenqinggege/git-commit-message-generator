package com.mirenqinggege.gitcommit.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.panel
import com.mirenqinggege.gitcommit.GitCommitMessageBundle
import com.mirenqinggege.gitcommit.services.OpenAiClientSettings
import com.mirenqinggege.gitcommit.services.OpenAiCompatibleClient
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.progress.ProgressIndicator
import javax.swing.JComponent

class GitCommitMessageConfigurable : Configurable {
    private var baseUrlField: JBTextField? = null
    private var apiKeyField: JBPasswordField? = null
    private var modelField: ComboBox<String>? = null
    private var apiTypeField: ComboBox<ApiType>? = null
    private var statusLabel: JBLabel? = null

    override fun getDisplayName() = GitCommitMessageBundle.message("settings.display.name")

    override fun createComponent(): JComponent = panel {
        row(GitCommitMessageBundle.message("settings.base.url")) {
            baseUrlField = textField().comment(GitCommitMessageBundle.message("settings.base.url.comment")).component
        }
        row(GitCommitMessageBundle.message("settings.api.key")) { apiKeyField = passwordField().component }
        row(GitCommitMessageBundle.message("settings.model")) {
            modelField = comboBox(listOf(settings().model)).component.apply { isEditable = true }
        }
        row(GitCommitMessageBundle.message("settings.api.type")) { apiTypeField = comboBox(ApiType.entries).component }
        row { comment(GitCommitMessageBundle.message("settings.description")) }
        row {
            button(GitCommitMessageBundle.message("settings.test.connection")) { loadModels() }
            button(GitCommitMessageBundle.message("settings.test.model")) { testModel() }
        }
        row { statusLabel = cell(JBLabel()).component }
        reset()
    }

    override fun isModified(): Boolean {
        val settings = settings()
        return baseUrlField?.text != settings.baseUrl ||
            String(apiKeyField?.password ?: charArrayOf()) != settings.apiKey ||
            modelField?.selectedItem?.toString() != settings.model || apiTypeField?.selectedItem != settings.apiType
    }

    override fun apply() {
        val settings = settings()
        settings.baseUrl = baseUrlField?.text.orEmpty().trim()
        settings.apiKey = String(apiKeyField?.password ?: charArrayOf())
        settings.model = modelField?.selectedItem?.toString().orEmpty().trim()
        settings.apiType = apiTypeField?.selectedItem as? ApiType ?: ApiType.RESPONSES
    }

    override fun reset() {
        val settings = settings()
        baseUrlField?.text = settings.baseUrl
        apiKeyField?.text = settings.apiKey
        modelField?.selectedItem = settings.model
        apiTypeField?.selectedItem = settings.apiType
    }

    private fun settings() = ApplicationManager.getApplication().getService(GitCommitMessageSettings::class.java)

    private fun currentClientSettings() = OpenAiClientSettings(
        baseUrlField?.text.orEmpty().trim(),
        String(apiKeyField?.password ?: charArrayOf()),
        modelField?.selectedItem?.toString().orEmpty().trim(),
        apiTypeField?.selectedItem as? ApiType ?: ApiType.RESPONSES
    )

    private fun loadModels() {
        setStatus(GitCommitMessageBundle.message("settings.testing.connection"))
        val clientSettings = currentClientSettings()
        object : Task.Backgroundable(null, GitCommitMessageBundle.message("settings.testing.connection"), true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = GitCommitMessageBundle.message("settings.loading.models")
                    val models = OpenAiCompatibleClient(clientSettings).listModels()
                    ApplicationManager.getApplication().invokeLater {
                        updateModels(models, clientSettings.model)
                        setStatus(GitCommitMessageBundle.message("notification.models.loaded", models.size))
                        notify(GitCommitMessageBundle.message("notification.models.loaded", models.size), NotificationType.INFORMATION)
                    }
                } catch (error: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        setStatus(GitCommitMessageBundle.message("notification.connection.failed", error.message.orEmpty()))
                        notify(GitCommitMessageBundle.message("notification.connection.failed", error.message.orEmpty()), NotificationType.ERROR)
                    }
                }
            }
        }.queue()
    }

    private fun testModel() {
        setStatus(GitCommitMessageBundle.message("settings.testing.model"))
        val clientSettings = currentClientSettings()
        object : Task.Backgroundable(null, GitCommitMessageBundle.message("settings.testing.model"), true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    OpenAiCompatibleClient(clientSettings).testConnection()
                    ApplicationManager.getApplication().invokeLater {
                        setStatus(GitCommitMessageBundle.message("notification.model.success"))
                        notify(GitCommitMessageBundle.message("notification.model.success"), NotificationType.INFORMATION)
                    }
                } catch (error: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        setStatus(GitCommitMessageBundle.message("notification.model.failed", error.message.orEmpty()))
                        notify(GitCommitMessageBundle.message("notification.model.failed", error.message.orEmpty()), NotificationType.ERROR)
                    }
                }
            }
        }.queue()
    }

    private fun updateModels(models: List<String>, currentModel: String) {
        val combo = modelField ?: return
        combo.removeAllItems()
        (models + currentModel).filter { it.isNotBlank() }.distinct().forEach(combo::addItem)
        combo.selectedItem = currentModel
    }

    private fun notify(message: String, type: NotificationType) {
        NotificationGroupManager.getInstance().getNotificationGroup("Git Commit Message Generator")
            .createNotification(message, type).notify(null)
    }

    private fun setStatus(message: String) {
        statusLabel?.text = message
    }
}
