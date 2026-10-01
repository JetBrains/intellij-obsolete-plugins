package com.intellij.python.huggingFace.modelChoice.ui

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.python.community.impl.huggingFace.api.HuggingFaceEntityBasicApiData
import com.intellij.python.community.impl.huggingFace.api.HuggingFaceURLProvider
import com.intellij.python.community.impl.huggingFace.service.PyHuggingFaceBundle
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class HfModelHeaderBuilder {
  fun createHeaderForModel(model: HuggingFaceEntityBasicApiData): HtmlChunk {
    val modelLink = HuggingFaceURLProvider.getModelCardLink(model.itemId).toString()
    return HtmlChunk.div().children(
        HtmlChunk.link(modelLink, PyHuggingFaceBundle.getMessage("python.hugging.face.open.on.link.text")),
        DocumentationMarkup.EXTERNAL_LINK_ICON
      )
  }
}
