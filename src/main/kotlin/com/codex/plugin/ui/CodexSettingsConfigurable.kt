package com.codex.plugin.ui

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class CodexSettingsConfigurable : Configurable {

    private var codexPathField: JBTextField? = null
    private var extraArgsField: JBTextField? = null
    private var confirmCodeChangesCheckBox: JBCheckBox? = null

    override fun getDisplayName() = "Codex Assistant"

    override fun createComponent(): JComponent {
        val settings = CodexSettings.getInstance()

        val pathField = JBTextField(settings.codexPath)
        val argsField = JBTextField(settings.extraArgs)
        val confirmBox = JBCheckBox("Confirm before applying code changes", settings.confirmCodeChanges)

        codexPathField = pathField
        extraArgsField = argsField
        confirmCodeChangesCheckBox = confirmBox

        return panel {
            row("Executable path:") {
                cell(pathField)
                    .align(AlignX.FILL)
                    .comment("Enter an absolute path if codex cannot be found in PATH (e.g. /usr/local/bin/codex)")
            }
            row("Extra CLI flags:") {
                cell(argsField)
                    .align(AlignX.FILL)
                    .comment("Appended to every codex invocation (e.g. --model gpt-4o)")
            }
            separator()
            row {
                cell(confirmBox)
                    .comment("When enabled, --ask-for-approval untrusted is automatically appended to each run")
            }
        }
    }

    override fun isModified(): Boolean {
        val s = CodexSettings.getInstance()
        return codexPathField?.text != s.codexPath ||
                extraArgsField?.text != s.extraArgs ||
                confirmCodeChangesCheckBox?.isSelected != s.confirmCodeChanges
    }

    override fun apply() {
        val s = CodexSettings.getInstance()
        s.codexPath = codexPathField?.text?.trim()?.ifBlank { "codex" } ?: "codex"
        s.extraArgs = extraArgsField?.text?.trim() ?: ""
        s.confirmCodeChanges = confirmCodeChangesCheckBox?.isSelected ?: true
    }

    override fun reset() {
        val s = CodexSettings.getInstance()
        codexPathField?.text = s.codexPath
        extraArgsField?.text = s.extraArgs
        confirmCodeChangesCheckBox?.isSelected = s.confirmCodeChanges
    }

    override fun disposeUIResources() {
        codexPathField = null
        extraArgsField = null
        confirmCodeChangesCheckBox = null
    }
}