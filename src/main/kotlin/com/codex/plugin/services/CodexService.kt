package com.codex.plugin.services

import com.codex.plugin.ui.CodexSettings
import com.intellij.terminal.ui.TerminalWidget
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.util.concurrent.ConcurrentHashMap
import javax.swing.SwingUtilities

object CodexService {

    private val runningTerminals = ConcurrentHashMap<Project, TerminalWidget>()

    fun isRunning(project: Project): Boolean = runningTerminals.containsKey(project)

    fun runInTerminal(project: Project) {
        if (runningTerminals.containsKey(project)) return

        val settings = CodexSettings.getInstance()
        val workDir = project.basePath ?: System.getProperty("user.home")!!

        val cmd = buildString {
            append(shellQuote(settings.codexPath))
            if (settings.confirmCodeChanges) {
                append(" --ask-for-approval untrusted")
            }
            if (settings.extraArgs.isNotBlank()) append(" ").append(settings.extraArgs.trim())
            if (settings.confirmCodeChanges) {
                append(" ").append(shellQuote(CONFIRM_CODE_CHANGES_PROMPT))
            }
        }

        val manager = TerminalToolWindowManager.getInstance(project)
        val widget = manager.createShellWidget(workDir, "Codex", true, true)
        val previousWidget = runningTerminals.putIfAbsent(project, widget)
        if (previousWidget != null) {
            Disposer.dispose(widget)
            return
        }
        widget.addTerminationCallback({
            runningTerminals.remove(project, widget)
        }, project)
        widget.sendCommandToExecute(cmd)
    }

    fun activateTerminal(project: Project) {
        val widget = runningTerminals[project] ?: return
        ApplicationManager.getApplication().invokeLater {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal") ?: return@invokeLater
            toolWindow.show {
                val targetContent = toolWindow.contentManager.contents.firstOrNull { content ->
                    SwingUtilities.isDescendingFrom(widget.component, content.component)
                }
                if (targetContent != null) {
                    toolWindow.contentManager.setSelectedContent(targetContent, true)
                }
                widget.requestFocus()
            }
        }
    }

    fun copyToTerminal(project: Project, text: String): Boolean {
        val widget = runningTerminals[project] ?: return false
        return runCatching {
            widget.ttyConnector?.write(text) ?: return false
        }.isSuccess
    }

    private fun shellQuote(value: String): String {
        if (value.isEmpty()) return "''"
        return "'${value.replace("'", "'\"'\"'")}'"
    }

    private const val CONFIRM_CODE_CHANGES_PROMPT =
        "Before creating, modifying, deleting, moving, or formatting files, ask the user for confirmation. " +
                "After the user confirms, make the approved code changes."
}
