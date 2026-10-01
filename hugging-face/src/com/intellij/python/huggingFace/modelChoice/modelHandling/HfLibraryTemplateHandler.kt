// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.huggingFace.modelChoice.modelHandling

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.python.codeInsight.imports.AddImportHelper
import com.jetbrains.python.psi.PyFile
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class ImportDetails(val from: String, val name: String, val asName: String?, val priority: AddImportHelper.ImportPriority?) {
  override fun toString(): String {
    val asPart = if (asName != null) " as $asName" else ""
    return "from $from import $name$asPart"
  }
}

@ApiStatus.Internal
interface HfLibraryTemplateHandler {
  private data class InsertionDetails(val offset: Int, val prefix: String, val postfix: String)
  fun getCodeTemplate(itemId: String, pipelineTag: String): String
  fun getImportDetails(itemId: String, pipelineTag: String): ImportDetails

  fun insertCode(itemId: String, pipelineTag: String, project: Project) {
    val importDetails = getImportDetails(itemId, pipelineTag)

    val editor = FileEditorManager.getInstance(project)?.selectedTextEditor ?: return
    val document = editor.document
    val virtualFile = FileDocumentManager.getInstance().getFile(document) ?: return
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return

    val codeTemplate = getCodeTemplate(itemId, pipelineTag)
    val psiDocumentManager = PsiDocumentManager.getInstance(project)
    psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)

    when (psiFile) {
      is PyFile -> insertInPyFile(editor, psiFile, importDetails, codeTemplate)
      else -> insertInIpynbFile(editor, importDetails, codeTemplate)
    }
    psiDocumentManager.commitDocument(document)
  }

  private fun insertInPyFile(editor: Editor, psiFile: PsiFile, importDetails: ImportDetails, codeTemplate: String) {
    var insertOnEnd = false

    if (editor.caretModel.offset == editor.document.textLength) {
      insertAtOffset(editor, editor.document.textLength, "\n$codeTemplate")
      insertOnEnd = true
    }

    CommandProcessor.getInstance().executeCommand(editor.project, {
      ApplicationManager.getApplication().runWriteAction {
        AddImportHelper.addOrUpdateFromImportStatement(
          psiFile,
          importDetails.from,
          importDetails.name,
          importDetails.asName,
          importDetails.priority,
          null
        )
      }
    }, null, null)

    if (!insertOnEnd) {
      val insertionDetails = calculateInsertionDetails(editor)
      insertAtOffset(editor, insertionDetails.offset, "${insertionDetails.prefix}$codeTemplate${insertionDetails.postfix}")
    }
  }

  private fun insertInIpynbFile(editor: Editor, importDetails: ImportDetails, codeTemplate: String) {
    val insertionDetails = calculateInsertionDetails(editor)
    insertAtOffset(editor, insertionDetails.offset, "${insertionDetails.prefix}$importDetails\n$codeTemplate\n")
  }

  private fun insertAtOffset(editor: Editor, offset: Int, text: String) {
    val project = editor.project ?: return
    CommandProcessor.getInstance().executeCommand(project, {
      ApplicationManager.getApplication().runWriteAction {
        editor.document.insertString(offset, text)
      }
    }, null, null)
    PsiDocumentManager.getInstance(project).commitDocument(editor.document)
  }

  private fun calculateInsertionDetails(editor: Editor): InsertionDetails {
    // todo: check if there is some platform API method to insert code blocks better | it must exist somewhere
    val document = editor.document
    val lineNumber = document.getLineNumber(editor.caretModel.offset)
    val lineStartOffset = document.getLineStartOffset(lineNumber)
    val lineEndOffset = document.getLineEndOffset(lineNumber)
    val lineContents = document.getText(TextRange(lineStartOffset, lineEndOffset))
    val partialContents = document.getText(TextRange(lineStartOffset, editor.caretModel.offset))

    if (partialContents.isBlank()) {
      val restOfTheLine = document.getText(TextRange(editor.caretModel.offset, lineEndOffset))
      return if (restOfTheLine.isBlank()) {
        InsertionDetails(editor.caretModel.offset, prefix = "", postfix = "")
      } else if (lineContents.isNotBlank()) {
        InsertionDetails(lineStartOffset, prefix = "", postfix = "\n")
      } else {
        InsertionDetails(lineStartOffset, prefix = "\n", postfix = "")
      }
    }

    val prefixWhitespaces = lineContents.takeWhile { it.isWhitespace() }
    val prefix = "\n" + prefixWhitespaces
    val offset = if (lineContents.all { it.isWhitespace() }) document.getLineEndOffset(lineNumber) else lineEndOffset
    return InsertionDetails(offset, prefix, postfix = "")
  }

}

@ApiStatus.Internal
class HfTransformersHandler : HfLibraryTemplateHandler {
  override fun getCodeTemplate(itemId: String, pipelineTag: String): String = """
    |pipe = pipeline("$pipelineTag", model="$itemId")
    """.trimMargin()

  override fun getImportDetails(itemId: String, pipelineTag: String): ImportDetails {
    return ImportDetails(
      from = "transformers",
      name = "pipeline",
      asName = null,
      priority = AddImportHelper.ImportPriority.THIRD_PARTY
    )
  }
}

@ApiStatus.Internal
class HfDiffusersHandler : HfLibraryTemplateHandler {
  override fun getCodeTemplate(itemId: String, pipelineTag: String): String  = """
    |pipeline = DiffusionPipeline.from_pretrained("$itemId")
  """.trimMargin()

  override fun getImportDetails(itemId: String, pipelineTag: String): ImportDetails {
    return ImportDetails(
      from = "diffusers",
      name = "DiffusionPipeline",
      asName = null,
      priority = AddImportHelper.ImportPriority.THIRD_PARTY
    )
  }
}

@ApiStatus.Internal
class HfSentenceTransformersHandler : HfLibraryTemplateHandler {
  override fun getCodeTemplate(itemId: String, pipelineTag: String): String = """
    |model = SentenceTransformer("$itemId")
  """.trimMargin()

  override fun getImportDetails(itemId: String, pipelineTag: String): ImportDetails {
    return ImportDetails(
      from = "sentence_transformers",
      name = "SentenceTransformer",
      asName = null,
      priority = AddImportHelper.ImportPriority.THIRD_PARTY
    )
  }
}

@ApiStatus.Internal
@Suppress("SpellCheckingInspection")
class HfStableBaseline3Handler : HfLibraryTemplateHandler {
  override fun getCodeTemplate(itemId: String, pipelineTag: String): String = """
    |checkpoint = load_from_hub(
    |  repo_id="$itemId",
    |  filename="$itemId.zip",
    |)
  """.trimMargin()

  override fun getImportDetails(itemId: String, pipelineTag: String): ImportDetails {
    return ImportDetails(
      from = "huggingface_sb3",
      name = "load_from_hub",
      asName = null,
      priority = AddImportHelper.ImportPriority.THIRD_PARTY
    )
  }
}

@ApiStatus.Internal
@Suppress("SpellCheckingInspection")
class HfFairseqHandler : HfLibraryTemplateHandler {
  override fun getCodeTemplate(itemId: String, pipelineTag: String): String = """
    |models, cfg, task = load_model_ensemble_and_task_from_hf_hub(
    |    "$itemId"
    |)
  """.trimMargin()

  override fun getImportDetails(itemId: String, pipelineTag: String): ImportDetails {
    return ImportDetails(
      from = "fairseq.checkpoint_utils",
      name = "load_model_ensemble_and_task_from_hf_hub",
      asName = null,
      priority = AddImportHelper.ImportPriority.THIRD_PARTY
    )
  }
}

@ApiStatus.Internal
@Suppress("SpellCheckingInspection")
class HfEspnetHandler : HfLibraryTemplateHandler {
  override fun getCodeTemplate(itemId: String, pipelineTag: String): String = """
    |model = Text2Speech.from_pretrained("$itemId")
    |
    |speech, *_ = model("text to generate speech from")
  """.trimMargin()

  override fun getImportDetails(itemId: String, pipelineTag: String): ImportDetails {
    return ImportDetails(
      from = "espnet2.bin.tts_inference",
      name = "Text2Speech",
      asName = null,
      priority = AddImportHelper.ImportPriority.THIRD_PARTY
    )
  }
}

@ApiStatus.Internal
@Suppress("SpellCheckingInspection")
class HfPyannoteHandler : HfLibraryTemplateHandler {
  override fun getCodeTemplate(itemId: String, pipelineTag: String): String = """
    |pipeline = Pipeline.from_pretrained("$itemId")
  """.trimMargin()

  override fun getImportDetails(itemId: String, pipelineTag: String): ImportDetails {
    return ImportDetails(
      from = "pyannote.audio",
      name = "pipeline",
      asName = null,
      priority = AddImportHelper.ImportPriority.THIRD_PARTY
    )
  }
}
