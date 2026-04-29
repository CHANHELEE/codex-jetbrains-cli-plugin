package com.codex.plugin.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "CodexSettings", storages = [Storage("CodexAssistant.xml")])
class CodexSettings : PersistentStateComponent<CodexSettings.State> {

    data class State(
        var codexPath: String = "codex",
        var extraArgs: String = "",
        var autoApplyThreshold: Int = 0,
        var confirmCodeChanges: Boolean = true
    )

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) { myState = state }

    var codexPath: String
        get() = myState.codexPath
        set(value) { myState.codexPath = value }

    var extraArgs: String
        get() = myState.extraArgs
        set(value) { myState.extraArgs = value }

    var autoApplyThreshold: Int
        get() = myState.autoApplyThreshold
        set(value) { myState.autoApplyThreshold = value }

    var confirmCodeChanges: Boolean
        get() = myState.confirmCodeChanges
        set(value) { myState.confirmCodeChanges = value }

    companion object {
        fun getInstance(): CodexSettings =
            ApplicationManager.getApplication().getService(CodexSettings::class.java)
    }
}
