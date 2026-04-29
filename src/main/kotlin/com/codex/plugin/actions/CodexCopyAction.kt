package com.codex.plugin.actions

import com.codex.plugin.services.CodexService
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileSystemItem
import kotlin.io.path.Path
import kotlin.io.path.relativeToOrNull

class CodexCopyAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        if (!CodexService.isRunning(project)) {
            CodexService.runInTerminal(project)
            return
        }

        val codexReference = buildCodexReference(project, e) ?: return

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
        val isRunning = project != null && CodexService.isRunning(project)
        e.presentation.isVisible = isRunning
        e.presentation.isEnabled = project != null && (
            !isRunning || buildCodexReference(project, e) != null
        )
    }

    private fun buildCodexReference(project: Project, e: AnActionEvent): String? {
        val projectViewReference = buildProjectViewReference(project, e)
        if (isProjectViewPlace(e) && projectViewReference != null) {
            return projectViewReference
        }

        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor != null) return buildEditorReference(project, editor)

        return projectViewReference
    }

    private fun isProjectViewPlace(e: AnActionEvent): Boolean {
        return e.place == ActionPlaces.PROJECT_VIEW_POPUP || e.place == "ProjectViewPopupMenu"
    }

    private fun buildProjectViewReference(project: Project, e: AnActionEvent): String? {
        val files = buildProjectViewFiles(e)
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        return files.joinToString(separator = " ") { file ->
            "@${buildPathReference(project, file)}"
        }
    }

    private fun buildProjectViewFiles(e: AnActionEvent): List<VirtualFile>? {
        val selectedFiles = selectedItems(e)
            .flatMap(::virtualFiles)
            .distinctBy { it.path }
            .takeIf { it.isNotEmpty() }
        if (selectedFiles != null) return selectedFiles

        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
            ?.filter { it.isValid }
            .orEmpty()
            .ifEmpty {
                e.getData(CommonDataKeys.VIRTUAL_FILE)
                    ?.takeIf { it.isValid }
                    ?.let { listOf(it) }
                    .orEmpty()
            }

        if (virtualFiles.isNotEmpty()) return virtualFiles.distinctBy { it.path }

        val psiFiles = e.getData(CommonDataKeys.PSI_FILE)
            ?.virtualFile
            ?.takeIf { it.isValid }
            ?.let { listOf(it) }
        if (psiFiles != null) return psiFiles

        val psiElementFiles = e.getData(LangDataKeys.PSI_ELEMENT_ARRAY)
            ?.flatMap(::virtualFiles)
            .orEmpty()
            .ifEmpty {
                e.getData(CommonDataKeys.PSI_ELEMENT)
                    ?.let(::virtualFiles)
                    .orEmpty()
            }
            .distinctBy { it.path }
        if (psiElementFiles.isNotEmpty()) return psiElementFiles

        return e.getData(CommonDataKeys.NAVIGATABLE_ARRAY)
            ?.flatMap(::virtualFiles)
            .orEmpty()
            .ifEmpty {
                e.getData(CommonDataKeys.NAVIGATABLE)
                    ?.let(::virtualFiles)
                    .orEmpty()
            }
            .distinctBy { it.path }
            .takeIf { it.isNotEmpty() }
    }

    private fun buildEditorReference(project: Project, editor: Editor): String? {
        val selectionModel = editor.selectionModel

        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return null
        val path = buildPathReference(project, file)

        if (!selectionModel.hasSelection()) return "@$path"

        val selectionStart = selectionModel.selectionStart
        val selectionEnd = selectionModel.selectionEnd
        val endOffset = (selectionEnd - 1).coerceAtLeast(selectionStart)
        val startLine = document.getLineNumber(selectionStart) + 1
        val endLine = document.getLineNumber(endOffset) + 1
        val lineRange = if (startLine == endLine) "L$startLine" else "L$startLine-$endLine"

        return "@$path#$lineRange"
    }

    private fun buildPathReference(project: Project, file: VirtualFile): String {
        return project.basePath
            ?.let { relativePath(file.path, it) }
            ?: file.path
    }

    private fun selectedItems(e: AnActionEvent): List<Any> {
        val selectedItems = e.getData(PlatformCoreDataKeys.SELECTED_ITEMS)
            ?.filterNotNull()
            .orEmpty()
        if (selectedItems.isNotEmpty()) return selectedItems

        return e.getData(PlatformCoreDataKeys.SELECTED_ITEM)
            ?.let { listOf(it) }
            .orEmpty()
    }

    private fun virtualFiles(item: Any): List<VirtualFile> {
        val directFile = virtualFile(item)
        if (directFile != null) return listOf(directFile)

        return reflectedChildren(item)
            .flatMap { child -> virtualFiles(child) }
            .distinctBy { it.path }
    }

    private fun virtualFile(item: Any): VirtualFile? {
        return when (item) {
            is ProjectViewNode<*> -> item.virtualFile?.takeIf { it.isValid }
            is AbstractTreeNode<*> -> virtualFile(item.value)
            is VirtualFile -> item.takeIf { it.isValid }
            is PsiFileSystemItem -> item.virtualFile?.takeIf { it.isValid }
            is OpenFileDescriptor -> item.file.takeIf { it.isValid }
            is com.intellij.psi.PsiElement -> item.containingFile?.virtualFile?.takeIf { it.isValid }
            else -> null
        }
    }

    private fun reflectedChildren(item: Any): List<Any> {
        val methodNames = listOf(
            "getVirtualFile",
            "getFile",
            "getFiles",
            "getDirectory",
            "getDirectories",
            "getPsiElement",
            "getPsiFile",
            "getValue",
        )

        return methodNames
            .flatMap { methodName ->
                runCatching {
                    val method = item.javaClass.methods.firstOrNull { method ->
                        method.name == methodName && method.parameterCount == 0
                    } ?: return@runCatching emptyList<Any>()

                    val value = method.invoke(item) ?: return@runCatching emptyList<Any>()
                    flattenReflectedValue(value)
                }.getOrDefault(emptyList())
            }
            .filterNot { it === item }
    }

    private fun flattenReflectedValue(value: Any): List<Any> {
        return when (value) {
            is Array<*> -> value.filterNotNull()
            is Iterable<*> -> value.filterNotNull()
            else -> listOf(value)
        }
    }

    private fun relativePath(filePath: String, basePath: String): String {
        return Path(filePath)
            .relativeToOrNull(Path(basePath))
            ?.joinToString(separator = "/") { it.toString() }
            ?: filePath
    }
}
