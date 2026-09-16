package com.intellij.bigdatatools.visualization.inlays

import com.intellij.bigdatatools.visualization.inlays.pages.InlayImagePage
import com.intellij.bigdatatools.visualization.inlays.pages.InlayPage
import com.intellij.bigdatatools.visualization.inlays.pages.InlaySvgImagePage
import com.intellij.openapi.diagnostic.Logger
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.Base64
import java.util.Stack
import java.util.regex.Pattern

/** Extracts, if possible, an image from single-image html (mathplotlib output for example) and create InlayPage with this image. */
object NotebookInlayHtmlImageHelper {
  private val logger = Logger.getInstance(this::class.java)

  private val embeddedImagePattern = Pattern.compile("^(data:image\\/[a-zA-Z]+;base64,)")

  private val lastLettersPattern = Pattern.compile("[a-zA-Z]+\$")

  /** If html string contains any of this tags its definitely not a single image. */
  private val noSingleImageTagsPattern = Pattern.compile("(<table|<p>|<p |<script)")

  fun getImagePageOrNull(data: String): InlayPage? {
    var page: InlayPage? = null

    try {
      val possibleImage = getSingleImageElementOrNull(data)

      possibleImage?.let {
        if (it.tagName() == "svg") {

          val widthOrHeightString = it.attr("width").ifBlank { it.attr("height") }
          val matcher = lastLettersPattern.matcher(widthOrHeightString)
          val pixelUnit = if (matcher.find()) matcher.group().lowercase() else ""

          page = InlaySvgImagePage(it.toString().replace("clippath", "clipPath"))
        }
        else if (it.tagName() == "img") {
          val src = it.attr("src")
          val matcher = embeddedImagePattern.matcher(src)
          if (matcher.find()) {
            val group = matcher.group()
            val imageType = group.substring(11, group.length - 8).lowercase()
            page = if (imageType == "svg") {
              InlaySvgImagePage(String(Base64.getMimeDecoder().decode(data)).replace("clippath", "clipPath"))
            }
            else {
              InlayImagePage(src.substring(matcher.group().length, src.length))
            }
          }
        }
      }
    }
    catch (e: Throwable) {
      logger.warn(e)
    }

    return page
  }

  private fun getSingleImageElementOrNull(data: String): Element? {
    if (noSingleImageTagsPattern.matcher(data).find()) {
      return null
    }

    val document = Jsoup.parse(data)

    val front = Stack<Element>()
    front.add(document.body())

    var image: Element? = null

    while (front.isNotEmpty()) {
      val peeked: Element = front.pop()

      if (peeked.tagName() == "img" || peeked.tagName() == "svg") {
        if (image == null) {
          image = peeked
        }
        else {
          // we found another image
          return null
        }
      }

      front.addAll(peeked.children())
    }

    return image
  }
}