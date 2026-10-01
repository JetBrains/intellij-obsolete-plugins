package com.intellij.python.huggingFace.modelChoice.modelHandling

import com.intellij.openapi.project.Project
import com.intellij.python.community.impl.huggingFace.HuggingFaceEntityKind
import com.intellij.python.community.impl.huggingFace.api.HuggingFaceApi
import com.intellij.python.community.impl.huggingFace.api.HuggingFaceEntityBasicApiData
import com.intellij.python.community.impl.huggingFace.documentation.HuggingFaceHtmlBuilder
import com.intellij.python.huggingFace.modelChoice.ui.HfModelHeaderBuilder
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls

@ApiStatus.Internal
class HfModelChoiceHtmlContentGenerator(private val project: Project) {

  private val headerBuilder = HfModelHeaderBuilder()

  @Nls
  suspend fun generateHtmlContent(selectedData: HuggingFaceEntityBasicApiData): String {
    val markdownContent = HuggingFaceApi.fetchOrRetrieveModelCard(
      selectedData,
      selectedData.itemId,
      HuggingFaceEntityKind.MODEL,
      prefix = "hf_model_selection_md"
    )

    val cardHeader = headerBuilder.createHeaderForModel(selectedData)
    return HuggingFaceHtmlBuilder(
      project,
      selectedData,
      markdownContent,
      HuggingFaceEntityKind.MODEL
    ).build(customHeader = cardHeader)
  }
}
