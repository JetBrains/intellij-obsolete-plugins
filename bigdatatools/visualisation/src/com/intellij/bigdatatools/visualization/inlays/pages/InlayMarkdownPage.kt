// ToDo DataBricks special output

//package com.intellij.bigdatatools.visualization.inlays.pages
//
//import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
//import com.intellij.ide.ui.UISettings
//import com.intellij.openapi.editor.Editor
//import com.intellij.openapi.util.Key
//import com.intellij.openapi.util.NlsSafe
//import com.intellij.openapi.util.SystemInfo
//import com.vladsch.flexmark.ext.definition.DefinitionExtension
//import com.vladsch.flexmark.ext.footnotes.FootnoteExtension
//import com.vladsch.flexmark.ext.gfm.strikethrough.SubscriptExtension
//import com.vladsch.flexmark.ext.gitlab.GitLabExtension
//import com.vladsch.flexmark.ext.superscript.SuperscriptExtension
//import com.vladsch.flexmark.ext.tables.TablesExtension
//import com.vladsch.flexmark.ext.typographic.TypographicExtension
//import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension
//import com.vladsch.flexmark.html.HtmlRenderer
//import com.vladsch.flexmark.parser.Parser
//import com.vladsch.flexmark.util.ast.Node
//import com.vladsch.flexmark.util.data.MutableDataSet
//import java.awt.Color
//import java.nio.charset.Charset
//import javax.swing.plaf.ComponentUI
//import javax.swing.text.html.HTMLEditorKit
//import javax.swing.text.html.StyleSheet
//
//class InlayMarkdownPage(private val editor: Editor, backgroundColor: Color) : InlayHtmlOffscreenPage(backgroundColor) {
//
//  override val title
//    get() = VisMessagesBundle.message("page.title.markdown")
//
//  override val contentType = InlayPageContentType.HTML
//
//  var text: String = ""
//    private set
//
//  private val Editor.browserConfig: BrowserConfig
//    get() = getUserData(markdownBrowserConfigKey)
//              ?.takeIf { it.first === contentComponent.ui }
//              ?.second
//            ?: createBrowserConfig()
//              .also { putUserData(markdownBrowserConfigKey, contentComponent.ui to it) }
//
//  override fun addData(data: String) {
//    val newText = markdownToHtml(data, editor.browserConfig)
//    if (newText != text) {
//      text = newText
//      super.addData(text)
//    }
//  }
//
//  private fun markdownToHtml(markdown: String, browserConfig: BrowserConfig): String {
//    val document: Node = browserConfig.parser.parse(markdown)
//    return browserConfig.renderer.render(document)
//  }
//
//  private fun createBrowserConfig(): BrowserConfig {
//    val options = MutableDataSet()
//
//    options.set(HtmlRenderer.SOFT_BREAK, "<br>\n")
//
//    options.set(Parser.EXTENSIONS, listOf(
//      TablesExtension.create(),
//      TypographicExtension.create(),
//      SuperscriptExtension.create(),
//      GitLabExtension.create(),
//      FootnoteExtension.create(),
//      DefinitionExtension.create(),
//      WikiLinkExtension.create(),
//      SubscriptExtension.create()))
//
//    val parser = Parser.builder(options).build()
//    val renderer = HtmlRenderer.builder(options).build()
//
//    val css = StyleSheet().apply {
//      addStyleSheet(loadCss())
//      addStyleSheet(getFontStyle())
//    }
//
//    return BrowserConfig(parser, renderer, css)
//  }
//
//  private fun loadCss(): StyleSheet =
//    StyleSheet().apply {
//      HTMLEditorKit::class.java.getResourceAsStream(HTMLEditorKit.DEFAULT_CSS)?.bufferedReader(Charset.forName("UTF-8")).use {
//        loadRules(it, null)
//      }
//    }
//
//  companion object {
//    private val markdownBrowserConfigKey = Key<Pair<ComponentUI, BrowserConfig>>("BrowserConfig")
//  }
//}
//
//private data class BrowserConfig(val parser: Parser, val renderer: HtmlRenderer, val css: StyleSheet)
//
///**
// * Style to configure font family and size.
// * This function implements logic we discussed with T. Tulupenko
// */
//internal fun getFontStyle(): StyleSheet {
//
//  val (mainFontSize, fontFamily) = getFontConfig()
//  val headerIncrements = mapOf(
//    // According to Intellij UI guidelines
//    "h1" to 12,
//    "h2" to 9,
//    "h3" to 5,
//    "h4" to 3,
//  )
//  val regularTags = arrayOf("body", "p", "blockquote", "li", "dd")
//  return StyleSheet().apply {
//    regularTags.forEach { addRule("""$it { font-family: "$fontFamily"; font-size: ${mainFontSize}px; }""") }
//    headerIncrements.forEach { (tag, increment) ->
//      addRule("""$tag { font-size: ${mainFontSize + increment}px; font-family: "$fontFamily"; font-weight: bold; }""")
//    }
//    addRule("""h5 {font-size: ${mainFontSize}px; font-weight: bold; font-style:italic; font-family: "$fontFamily"; }""")
//    addRule("""h6 {font-size: ${mainFontSize}px; font-weight: normal; font-style:italic; "$fontFamily"; }""")
//  }
//}
//
//internal fun getFontConfig(): Pair<Int, @NlsSafe String> {
//  val fontSizeIncrement = when { //DS-894
//    SystemInfo.isWindows -> 3
//    SystemInfo.isMac -> 1
//    else -> 0
//  }
//  val uiSettings = UISettings.instance
//  val mainFontSize = uiSettings.let { if (it.overrideLafFonts) it.fontSize else UISettings.defFontSize } + fontSizeIncrement
//  val fontFamily = uiSettings.fontFace ?: "" // "browser default" if not set
//  return Pair(mainFontSize, fontFamily)
//}