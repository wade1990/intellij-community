// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vcs.changes.ignore.psi.util

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vcs.changes.IgnoredFileContentProvider
import com.intellij.openapi.vcs.changes.IgnoredFileDescriptor
import com.intellij.openapi.vcs.changes.ignore.lang.IgnoreFileConstants
import com.intellij.openapi.vcs.changes.ignore.lang.IgnoreLanguage
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.vcsUtil.VcsImplUtil
import com.intellij.vcsUtil.VcsUtil
import org.jetbrains.annotations.TestOnly

@TestOnly
fun updateIgnoreBlock(project: Project,
                      ignoreFile: VirtualFile,
                      ignoredGroupDescription: String,
                      vararg newEntries: IgnoredFileDescriptor) {
  changeIgnoreFile(project, ignoreFile) { provider ->
    updateIgnoreBlock(ignoredGroupDescription, ignoreFile, newEntries, provider)
  }
}

fun addNewElementsToIgnoreBlock(project: Project,
                                ignoreFile: VirtualFile,
                                ignoredGroupDescription: String,
                                vararg newEntries: IgnoredFileDescriptor) {
  changeIgnoreFile(project, ignoreFile) { provider ->
    addNewElementsToIgnoreBlock(ignoredGroupDescription, ignoreFile, newEntries, provider)
  }
}

fun addNewElements(project: Project, ignoreFile: VirtualFile, newEntries: List<IgnoredFileDescriptor>) {
  changeIgnoreFile(project, ignoreFile) { provider ->
    val document = FileDocumentManager.getInstance().getDocument(ignoreFile) ?: return@changeIgnoreFile
    if (document.textLength != 0 && document.charsSequence.last() != '\n') {
      document.insertString(document.textLength, IgnoreFileConstants.NEWLINE)
    }
    val textEndOffset = document.textLength
    val text = newEntries.joinToString(
      separator = IgnoreFileConstants.NEWLINE,
      postfix = IgnoreFileConstants.NEWLINE
    ) { it.toText(provider, ignoreFile) }
    document.insertString(textEndOffset, text)
  }
}

private fun changeIgnoreFile(project: Project,
                             ignoreFile: VirtualFile,
                             action: (IgnoredFileContentProvider) -> Unit) {
  val vcs = VcsUtil.getVcsFor(project, ignoreFile) ?: return
  val ignoredFileContentProvider = VcsImplUtil.findIgnoredFileContentProvider(vcs) ?: return
  invokeAndWaitIfNeeded {
    runUndoTransparentWriteAction {
      if (PsiManager.getInstance(project).findFile(ignoreFile)?.language !is IgnoreLanguage) return@runUndoTransparentWriteAction
      action(ignoredFileContentProvider)
      ignoreFile.save()
    }
  }
}

private fun updateIgnoreBlock(ignoredGroupDescription: String,
                              ignoreFile: VirtualFile,
                              newEntries: Array<out IgnoredFileDescriptor>,
                              provider: IgnoredFileContentProvider) {
  val document = FileDocumentManager.getInstance().getDocument(ignoreFile) ?: return
  val contentTextRange = getOrCreateIgnoreBlockContentTextRange(document, ignoredGroupDescription)

  val newEntriesText = newEntries.joinToString(
    separator = IgnoreFileConstants.NEWLINE,
    postfix = IgnoreFileConstants.NEWLINE
  ) { it.toText(provider, ignoreFile) }
  document.replaceString(contentTextRange.startOffset, contentTextRange.endOffset, newEntriesText)
}

private fun addNewElementsToIgnoreBlock(ignoredGroupDescription: String,
                                        ignoreFile: VirtualFile,
                                        newEntries: Array<out IgnoredFileDescriptor>,
                                        provider: IgnoredFileContentProvider) {
  val document = FileDocumentManager.getInstance().getDocument(ignoreFile) ?: return

  val contentRange = getOrCreateIgnoreBlockContentTextRange(document, ignoredGroupDescription)
  val existingEntries = document.charsSequence.subSequence(contentRange.startOffset, contentRange.endOffset).lines().toSet()
  val newEntriesText = newEntries
    .map { it.toText(provider, ignoreFile) }
    .filterNot { it in existingEntries }
    .joinToString(separator = IgnoreFileConstants.NEWLINE, postfix = IgnoreFileConstants.NEWLINE)
  document.insertString(contentRange.endOffset, newEntriesText)
}

private fun getOrCreateIgnoreBlockContentTextRange(
  ignoreFile: Document,
  ignoredGroupDescription: String
): TextRange {
  val text = ignoreFile.charsSequence
  val groupDescrLine = text.lineSequence().indexOfFirst { it == ignoredGroupDescription }
  return if (groupDescrLine == -1) {
    val ignoreGroupToAppend = createIgnoreGroup(text, ignoredGroupDescription)
    ignoreFile.insertString(ignoreFile.textLength, ignoreGroupToAppend)
    val lastIndex = ignoreFile.textLength
    TextRange(lastIndex, lastIndex)
  }
  else {
    val groupDescrStartOffset = ignoreFile.getLineStartOffset(groupDescrLine)
    val tail = text.subSequence(groupDescrStartOffset, ignoreFile.textLength)
    val emptyLine = tail.lineSequence()
      .drop(1)
      .indexOfFirst {
        it.isBlank() || it.startsWith(IgnoreFileConstants.HASH)
      }
    val groupEndOffset = if (emptyLine == -1) {
      ignoreFile.insertString(ignoreFile.textLength, IgnoreFileConstants.NEWLINE)
      ignoreFile.textLength
    }
    else {
      ignoreFile.getLineStartOffset(emptyLine + 1 + groupDescrLine)
    }
    val contentStartOffset = if (ignoreFile.lineCount <= groupDescrLine + 1) {
      groupEndOffset
    }
    else {
      ignoreFile.getLineStartOffset(groupDescrLine + 1)
    }
    TextRange(contentStartOffset, groupEndOffset)
  }
}

private fun createIgnoreGroup(text: CharSequence, ignoredGroupDescription: String): String {
  val newlineRequired = text.isNotEmpty() && text.last() != IgnoreFileConstants.NEWLINE[0]
  return buildString {
    if (newlineRequired) {
      append(IgnoreFileConstants.NEWLINE)
    }
    append(ignoredGroupDescription)
    append(IgnoreFileConstants.NEWLINE)
  }
}

private fun IgnoredFileDescriptor.toText(ignoredFileContentProvider: IgnoredFileContentProvider, ignoreFile: VirtualFile): String {
  val ignorePath = path
  val ignoreMask = mask
  return if (ignorePath != null) {
    val ignoreFileContainingDir = ignoreFile.parent ?: throw IllegalStateException(
      "Cannot determine ignore file path for $ignoreFile")
    ignoredFileContentProvider.buildIgnoreEntryContent(ignoreFileContainingDir, this)
  }
  else {
    ignoreMask ?: throw IllegalStateException("IgnoredFileBean: path and mask cannot be null at the same time")
  }
}

// Requires write action
private fun VirtualFile.save() {
  if (isDirectory || !isValid) {
    return
  }
  val documentManager = FileDocumentManager.getInstance()
  if (documentManager.isFileModified(this)) {
    documentManager.getDocument(this)?.let(documentManager::saveDocumentAsIs)
  }
}
