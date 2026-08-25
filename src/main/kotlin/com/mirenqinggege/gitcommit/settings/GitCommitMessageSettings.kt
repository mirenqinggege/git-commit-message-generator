package com.mirenqinggege.gitcommit.settings

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
        var apiKey: String = "",
        var model: String = "gpt-4o-mini",
        var apiType: ApiType = ApiType.RESPONSES
    )

    private var state = State()
    override fun getState() = state
    override fun loadState(state: State) { this.state = state }

    var baseUrl: String get() = state.baseUrl; set(value) { state.baseUrl = value }
    var apiKey: String get() = state.apiKey; set(value) { state.apiKey = value }
    var model: String get() = state.model; set(value) { state.model = value }
    var apiType: ApiType get() = state.apiType; set(value) { state.apiType = value }
}
