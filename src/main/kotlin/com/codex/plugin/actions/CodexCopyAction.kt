package com.codex.plugin.actions

import com.codex.plugin.services.CodexService
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import kotlin.io.path.Path
import kotlin.io.path.relativeToOrNull

class CodexCopyAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        if (!CodexService.isRunning(project)) {
            CodexService.runInTerminal(project)
            return
        }

        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val codexReference = buildCodexReference(project, editor) ?: return

        if (CodexService.copyToTerminal(project, codexReference)) {
            CodexService.activateTerminal(project)
        } else {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Codex Assistant")
                .createNotification("Codex", "Failed to send to the running Codex terminal.", NotificationType.WARNING)
                .notify(project)
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val isRunning = project != null && CodexService.isRunning(project)
        e.presentation.isVisible = isRunning
        e.presentation.isEnabled = project != null && (
            !isRunning || (editor != null && buildCodexReference(project, editor) != null)
        )
    }

    private fun buildCodexReference(project: Project, editor: Editor): String? {
        val selectionModel = editor.selectionModel

        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return null
        val path = project.basePath
            ?.let { relativePath(file.path, it) }
            ?: file.path

        if (!selectionModel.hasSelection()) return "@$path"

        val selectionStart = selectionModel.selectionStart
        val selectionEnd = selectionModel.selectionEnd
        val endOffset = (selectionEnd - 1).coerceAtLeast(selectionStart)
        val startLine = document.getLineNumber(selectionStart) + 1
        val endLine = document.getLineNumber(endOffset) + 1
        val lineRange = if (startLine == endLine) "L$startLine" else "L$startLine-$endLine"

        return "@$path#$lineRange"
    }

    private fun relativePath(filePath: String, basePath: String): String {
        return Path(filePath)
            .relativeToOrNull(Path(basePath))
            ?.joinToString(separator = "/") { it.toString() }
            ?: filePath
    }
}
