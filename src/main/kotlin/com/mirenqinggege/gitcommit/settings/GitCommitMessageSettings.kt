package com.mirenqinggege.gitcommit.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

enum class ApiType { COMPLETIONS, RESPONSES }

@Service(Service.Level.APP)
@State(name = "GitCommitMessageGeneratorSettings", storages = [Storage("gitCommitMessageGenerator.xml")])
class GitCommitMessageSettings : PersistentStateComponent<GitCommitMessageSettings.State> {
    data class State(
        var baseUrl: String = "https://api.openai.com/v1",
        var model: String = "gpt-4o-mini",
        var apiType: ApiType = ApiType.RESPONSES,
        var enableThinking: Boolean = false
    )

    private var state = State()
    private val credentialAttributes = CredentialAttributes(
        "git-commit-message-generator", "apiKey"
    )

    override fun getState() = state
    override fun loadState(state: State) { this.state = state }

    var baseUrl: String get() = state.baseUrl; set(value) { state.baseUrl = value }
    var model: String get() = state.model; set(value) { state.model = value }
    var apiType: ApiType get() = state.apiType; set(value) { state.apiType = value }
    var enableThinking: Boolean get() = state.enableThinking; set(value) { state.enableThinking = value }

    /** API key is persisted in the encrypted credential store, not the state XML. */
    var apiKey: String
        get() = PasswordSafe.instance.getPassword(credentialAttributes).orEmpty()
        set(value) {
            if (value.isBlank()) {
                PasswordSafe.instance.set(credentialAttributes, null)
            } else {
                PasswordSafe.instance.set(credentialAttributes, Credentials(null, value))
            }
        }
}
