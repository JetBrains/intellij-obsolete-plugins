package com.intellij.bigdatatools.visualization.inlays.components

import com.intellij.bigdatatools.coreUi.util.getColorHexString
import com.intellij.bigdatatools.visualization.inlays.style.InlaysConfig
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.util.Scanner

/** github-markdown.css as String, dependent form current IDE theme. */
class GithubMarkdownCss {
  companion object {

    private var storedCss: String? = null

    private var isStoredDarkula = false

    private fun getResourceAsString(resource: String): String {
      val inputStream = GithubMarkdownCss::class.java.classLoader.getResourceAsStream(resource)
      val scanner = Scanner(inputStream).useDelimiter("\\A")
      return if (scanner.hasNext()) scanner.next() else ""
    }

    fun getCss(backgroundColor: Color): String {

      var css = storedCss

      if (css != null && isStoredDarkula == UIUtil.isUnderDarcula()) {
        return css
      }

      css = if (UIUtil.isUnderDarcula()) {
        isStoredDarkula = true
        getResourceAsString("css/github-darcula.css")
      }
      else {
        isStoredDarkula = false
        getResourceAsString("css/github-intellij.css")
      }

      val index = css.indexOf("font-size: 16px;")
      if (index != -1) {
        css = css.replaceRange(index + 11, index + 13, JBUIScale.scaleFontSize(14f).toString())
      }

      if (InlaysConfig.getInstance().transparentOutput) {
        var backgroundColorIndex = css.indexOf("body {\n    background-color:")
        if (backgroundColorIndex == -1) {
          backgroundColorIndex = css.indexOf("body {\r\n    background-color:")
        }
        if (backgroundColorIndex != -1) {
          css = css.replaceRange(backgroundColorIndex + 30, backgroundColorIndex + 37, backgroundColor.getColorHexString())
        }
      }

      storedCss = css

      return css
    }
  }
}