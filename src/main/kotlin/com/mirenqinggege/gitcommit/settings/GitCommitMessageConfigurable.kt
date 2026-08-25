package com.mirenqinggege.gitcommit.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class GitCommitMessageConfigurable : Configurable {
    private var baseUrlField: JBTextField? = null
    private var apiKeyField: JBPasswordField? = null
    private var modelField: JBTextField? = null
    private var apiTypeField: ComboBox<ApiType>? = null

    override fun getDisplayName() = "Git Commit Message Generator"

    override fun createComponent(): JComponent = panel {
        row("Base URL:") { baseUrlField = textField().comment("For example: https://api.openai.com/v1").component }
        row("API key:") { apiKeyField = passwordField().component }
        row("Model:") { modelField = textField().component }
        row("API type:") { apiTypeField = comboBox(ApiType.entries).component }
        row { comment("The plugin sends the current Git diff to the configured OpenAI-compatible endpoint.") }
        reset()
    }

    override fun isModified(): Boolean {
        val settings = settings()
        return baseUrlField?.text != settings.baseUrl ||
            String(apiKeyField?.password ?: charArrayOf()) != settings.apiKey ||
            modelField?.text != settings.model || apiTypeField?.selectedItem != settings.apiType
    }

    override fun apply() {
        val settings = settings()
        settings.baseUrl = baseUrlField?.text.orEmpty().trim()
        settings.apiKey = String(apiKeyField?.password ?: charArrayOf())
        settings.model = modelField?.text.orEmpty().trim()
        settings.apiType = apiTypeField?.selectedItem as? ApiType ?: ApiType.RESPONSES
    }

    override fun reset() {
        val settings = settings()
        baseUrlField?.text = settings.baseUrl
        apiKeyField?.text = settings.apiKey
        modelField?.text = settings.model
        apiTypeField?.selectedItem = settings.apiType
    }

    private fun settings() = ApplicationManager.getApplication().getService(GitCommitMessageSettings::class.java)
}
