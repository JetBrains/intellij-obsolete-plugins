// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config.yaml

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.helidon.config.HelidonMetaConfigKeyManager
import com.intellij.microservices.jvm.config.ConfigKeyPathReference
import com.intellij.microservices.jvm.config.MetaConfigKey
import com.intellij.microservices.jvm.config.yaml.ConfigYamlUtils
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Key
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ObjectUtils
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLBundle
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.completion.YamlKeyCompletionInsertHandler
import org.jetbrains.yaml.psi.*
import kotlin.math.min

internal class HelidonYamlKeyCompletionProvider : CompletionProvider<CompletionParameters>() {
  companion object {
    private val CONFIG_KEY = Key.create<String>("ymlConfigKey")
    private val ROOT_KEY = Key.create<String>("ymlRootKey")

    private val INSERT_HANDLER = object : YamlKeyCompletionInsertHandler<LookupElementDecorator<LookupElementBuilder>>() {
      override fun createNewEntry(document: YAMLDocument, item: LookupElementDecorator<LookupElementBuilder>,
                                  parent: YAMLKeyValue?): YAMLKeyValue {
        val rootQualifiedName = item.getCopyableUserData(ROOT_KEY)
        val defaultAccessor = HelidonConfigYamlAccessor(document)

        val root = if (rootQualifiedName != null) defaultAccessor.findExistingKey(rootQualifiedName) else null
        val accessor = if (root != null) HelidonConfigYamlAccessor(root) else HelidonConfigYamlAccessor(document)

        val qualifiedKey = item.getCopyableUserData(CONFIG_KEY)
        // Yaml psi may change after removing insertion string, so key could be found after this.
        return accessor.findExistingKey(qualifiedKey) ?: accessor.create(qualifiedKey) ?: error(qualifiedKey)
      }

      override fun handleInsert(context: InsertionContext, item: LookupElementDecorator<LookupElementBuilder>) {
        super.handleInsert(context, item)
        AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
      }
    }

    private val INDEXED_INSERT_HANDLER = object : YamlKeyCompletionInsertHandler<LookupElementDecorator<LookupElementBuilder>>() {

      override fun createNewEntry(document: YAMLDocument,
                                  item: LookupElementDecorator<LookupElementBuilder>,
                                  parent: YAMLKeyValue?): YAMLKeyValue {
        val qualifiedKey = item.getCopyableUserData(CONFIG_KEY)
        val generator = YAMLElementGenerator.getInstance(document.project)
        val dummyFile = generator.createDummyYamlWithText(YAMLElementGenerator.createChainedKey(listOf(qualifiedKey), 0))
        val yamlMapping = dummyFile.documents[0].topLevelValue as YAMLMapping
        return yamlMapping.keyValues.first()
      }

      override fun handleInsert(context: InsertionContext, item: LookupElementDecorator<LookupElementBuilder>) {
        // keyValue is created by handler, there is no need in inserting completion char
        context.setAddCompletionChar(false)

        val currentElement = context.file.findElementAt(context.startOffset)
                             ?: error("no element at " + context.startOffset)

        val parentYamlKeyValue = PsiTreeUtil.getParentOfType(currentElement, YAMLKeyValue::class.java)
                                 ?: error("no YAMLKeyValue found")

        val parentSequenceItem = PsiTreeUtil.getParentOfType(currentElement, YAMLSequenceItem::class.java)
                                 ?: error("no YAMLSequenceItem found")

        val parentMapping = PsiTreeUtil.getParentOfType(currentElement, YAMLMapping::class.java, true, YAMLSequenceItem::class.java)

        val holdingDocument = PsiTreeUtil.getParentOfType(currentElement, YAMLDocument::class.java) ?: error("no YAMLDocument found")
        val createdKeyValue = createNewEntry(holdingDocument, item, if (parentYamlKeyValue.isValid) parentYamlKeyValue else null)

        val yamlElementGenerator = YAMLElementGenerator.getInstance(createdKeyValue.project)
        val psiModificationRunnable = Runnable {
          if (parentMapping != null) {
            val lastKeyValue = parentMapping.keyValues.last()
            val eol = parentMapping.addAfter(yamlElementGenerator.createEol(), lastKeyValue)
            val indent = parentMapping.addAfter(yamlElementGenerator.createIndent(YAMLUtil.getIndentToThisElement(parentMapping) + 2), eol)
            parentMapping.addAfter(createdKeyValue, indent)
          }
          else {
            val addEol = parentSequenceItem.textRange.endOffset != context.document.charsSequence.length - 1
            parentSequenceItem.add(createdKeyValue.parentMapping!!)
            if (addEol) {
              parentSequenceItem.add(yamlElementGenerator.createEol())
              parentSequenceItem.add(yamlElementGenerator.createIndent(YAMLUtil.getIndentToThisElement(parentSequenceItem) + 2))
            }
          }
        }

        deleteLookupText(context, parentMapping != null &&
                                  parentMapping.children.toList().indexOf(currentElement.parent) == 0)

        WriteCommandAction.runWriteCommandAction(context.project,
                                                 YAMLBundle.message("YamlKeyCompletionInsertHandler.insert.value"),
                                                 null,
                                                 psiModificationRunnable)

        PsiDocumentManager.getInstance(context.project).doPostponedOperationsAndUnblockDocument(context.document)

        if (parentMapping != null) {
          parentMapping.keyValues.last().textRange?.endOffset?.let {
            context.editor.caretModel.moveToOffset(it)
          }
        }
        else {
          context.editor.caretModel.moveToOffset(getLineEndOffset(context))
        }

        if (context.document.charsSequence[context.editor.caretModel.offset - 1] != ' ') {
          context.document.insertString(context.editor.caretModel.offset, " ")
          context.editor.caretModel.moveCaretRelatively(1, 0, false, false, true)
        }

        AutoPopupController.getInstance(context.project).scheduleAutoPopup(context.editor)
      }

      private fun deleteLookupText(context: InsertionContext, untilTail: Boolean) {
        val document = context.document
        val sequence = document.charsSequence

        var leftBound = context.startOffset - 1
        while (leftBound >= 0) {
          val c = sequence[leftBound]
          if (c != ' ' && c != '\t') {
            if (c == '\n') {
              leftBound--
            }
            else {
              leftBound = context.startOffset - 1
            }
            break
          }
          leftBound--
        }

        val rightBound = if (untilTail)
          min(context.tailOffset + 1, sequence.length)
        else
          getLineEndOffset(context)

        document.deleteString(leftBound + 1, rightBound)
        context.commitDocument()
      }

      private fun getLineEndOffset(context: InsertionContext): Int {
        val document = context.document
        val sequence = document.charsSequence
        var lineEnd = context.startOffset
        while (lineEnd < sequence.length &&
               sequence[lineEnd] != '\n') {
          lineEnd++
        }
        return lineEnd
      }
    }

    private val indexedKeyPositionPattern: ElementPattern<PsiElement> = PlatformPatterns.or(
      PlatformPatterns.psiElement().withSuperParent(2, YAMLSequenceItem::class.java),
      PlatformPatterns.psiElement().withSuperParent(3, YAMLSequenceItem::class.java)
    )
  }

  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
    val yamlFile = parameters.originalFile as YAMLFile
    val module = ModuleUtilCore.findModuleForPsiElement(yamlFile) ?: return

    val element = parameters.position
    val originalElement = CompletionUtil.getOriginalElement(element)

    val parentYamlKeyValue = getParentKeyValue(element, originalElement)

    val originalDocumentAnchor = originalElement ?: element.containingFile.originalFile.findElementAt(parameters.offset)
    val yamlDocument = PsiTreeUtil.getParentOfType(ObjectUtils.chooseNotNull(originalDocumentAnchor, element), YAMLDocument::class.java)

    val parentReferences = parentYamlKeyValue?.references
    var root: YAMLPsiElement? = yamlDocument
    var parentQualifiedName = ""
    var configKeys: List<MetaConfigKey>? = null
    val binder = HelidonMetaConfigKeyManager.getInstance().getConfigKeyNameBinder(module)

    val parentKeyReference = parentReferences?.find { it is HelidonYamlKeyMetaConfigKeyReference } as HelidonYamlKeyMetaConfigKeyReference?
    if (parentKeyReference != null) {
      val keyData = parentKeyReference.keyData
      if (keyData.key != null) {
        if (keyData.key.isAccessType(*MetaConfigKey.AccessType.MAP_GROUP)
            && keyData.pathType != ConfigKeyPathReference.PathType.ARBITRARY_ENTRY_KEY) {
          // Skip completion autopopup for map key
          result.stopHere()
          return
        }
        else if (keyData.key.isAccessType(MetaConfigKey.AccessType.INDEXED)) {
          val parentSequenceItem = getParentSequenceItem(element)
          if (parentSequenceItem != null) {
            val filteredKeys = filterExistingKeys(parentSequenceItem, keyData.key.subKeys)
            result.addAllElements(getIndexedLookupElements(filteredKeys))
          }
          result.stopHere()
          return
        }
        root = parentYamlKeyValue
        configKeys = keyData.key.subKeys
      }
      else {
        parentQualifiedName = keyData.keyText
        root = keyData.root
        if (keyData.parentKey != null) {
          configKeys = keyData.parentKey.subKeys
        }
      }
    }
    if (configKeys == null) {
      configKeys = HelidonMetaConfigKeyManager.getInstance().getAllMetaConfigKeys(module)
    }

    val rootQualifiedName = if (root is YAMLKeyValue) getQualifiedConfigKeyName(root) else null
    val accessor =
      when (root) {
        is YAMLDocument -> {
          HelidonConfigYamlAccessor(root)
        }
        is YAMLKeyValue -> {
          HelidonConfigYamlAccessor(root)
        }
        else -> {
          null
        }
      }

    ConfigYamlUtils.addCompletionAddIfNeeded(parameters, result)

    val currentLineKeyComponents =
      ConfigYamlUtils.getCurrentLineKeyComponents(ObjectUtils.chooseNotNull(originalElement, element), binder,
                                                  parentQualifiedName, configKeys)
    if (currentLineKeyComponents.isNotEmpty()) {
      result.addAllElements(currentLineKeyComponents)
    }

    val invocationCount = parameters.invocationCount
    val parentConfigKeyName = if (invocationCount > 1) "" else parentQualifiedName

    val keyLookupElements = ArrayList<LookupElement>()
    for (configKey in configKeys) {
      if (parentConfigKeyName.isNotEmpty() && !binder.matchesPrefix(configKey, parentConfigKeyName)) continue

      // filter existing keys incl. relaxed
      val configKeyName = configKey.name
      if (accessor?.findExistingKey(configKey.name) != null) continue

      val builder = configKey.presentation.lookupElement
      builder.putCopyableUserData(CONFIG_KEY, configKeyName)
      builder.putCopyableUserData(ROOT_KEY, rootQualifiedName)
      builder.withLookupString(configKeyName)
      val insertHandler = LookupElementDecorator.withInsertHandler(builder, INSERT_HANDLER)
      val lookupElement = configKey.presentation.tuneLookupElement(insertHandler)
      keyLookupElements.add(lookupElement)
    }
    result.addAllElements(keyLookupElements)
    result.stopHere()
  }

  private fun getParentKeyValue(element: PsiElement, originalElement: PsiElement?): YAMLKeyValue? {
    var parentYamlKeyValue = PsiTreeUtil.getParentOfType(originalElement, YAMLKeyValue::class.java)
    if (parentYamlKeyValue == null) {  // with existing text
      parentYamlKeyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java)
      if (parentYamlKeyValue != null) {
        parentYamlKeyValue = CompletionUtil.getOriginalElement(parentYamlKeyValue) ?: parentYamlKeyValue
      }
    }
    if (element.node.elementType === YAMLTokenTypes.SCALAR_KEY) {
      parentYamlKeyValue = PsiTreeUtil.getParentOfType(parentYamlKeyValue, YAMLKeyValue::class.java)
    }
    return parentYamlKeyValue
  }

  private fun getParentSequenceItem(element: PsiElement): YAMLSequenceItem? {
    val suitableElement = indexedKeyPositionPattern.accepts(element)
    if (suitableElement) {
      return PsiTreeUtil.findFirstParent(element, Condition { it is YAMLSequenceItem }) as? YAMLSequenceItem
    }
    return null
  }

  private fun filterExistingKeys(parentSequenceItem: YAMLSequenceItem?,
                                 allKeys: List<MetaConfigKey>): List<MetaConfigKey> {
    if (parentSequenceItem == null) return allKeys

    val qsConfigYamlAccessor = HelidonConfigYamlAccessor(parentSequenceItem, null)

    return allKeys.filter { qsConfigYamlAccessor.findExistingKey(it.name) == null }
  }

  private fun getIndexedLookupElements(keys: List<MetaConfigKey>): List<LookupElement> {
    val lookupElements = mutableListOf<LookupElement>()

    for (key in keys) {
      val builder = key.presentation.lookupElement
      builder.putCopyableUserData(CONFIG_KEY, key.name)
      builder.withLookupString(key.name)
      val insertHandler = LookupElementDecorator.withInsertHandler(builder, INDEXED_INSERT_HANDLER)
      val lookupElement = key.presentation.tuneLookupElement(insertHandler)
      lookupElements.add(lookupElement)
    }

    return lookupElements
  }
}