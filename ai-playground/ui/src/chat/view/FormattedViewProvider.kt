package com.intellij.aiplayground.ui.chat.view

import com.intellij.markdown.utils.CodeFenceSyntaxHighlighterGeneratingProvider
import com.intellij.markdown.utils.lang.CodeBlockHtmlSyntaxHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.components.JBHtmlPane
import com.intellij.ui.components.JBHtmlPaneConfiguration
import com.intellij.ui.components.JBHtmlPaneStyleConfiguration
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBThinOverlappingScrollBar
import com.intellij.util.ui.JBUI
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.CompositeASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.GeneratingProvider
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser
import java.awt.Adjustable
import java.awt.Component
import java.awt.Dimension
import java.net.URI

interface FormattedViewProvider {
  fun createView(project: Project): FormattedView
}

interface FormattedView {

  val component: Component

  fun updateText(text: String)

}

class NullFormattedViewProvider : FormattedViewProvider {

  override fun createView(project: Project): FormattedView {
    val component = createJBHtmlPane(JBHtmlPaneConfiguration.builder().build())
    return object : FormattedView {
      override val component: Component = component
      override fun updateText(text: String) {
        component.text = text
        component.invalidate()
      }
    }
  }
}

class HtmlFormattedViewProvider : FormattedViewProvider {

  override fun createView(project: Project): FormattedView {
    val flavour = object : GFMFlavourDescriptor() {
      override fun createHtmlGeneratingProviders(linkMap: LinkMap, baseURI: URI?): Map<IElementType, GeneratingProvider> {
        val map = super.createHtmlGeneratingProviders(linkMap, baseURI)
        val syntaxHighlighter = CodeFenceSyntaxHighlighterGeneratingProvider(CodeBlockHtmlSyntaxHighlighter(project))
        return map + hashMapOf(
          MarkdownElementTypes.CODE_FENCE to object : GeneratingProvider {
            override fun processNode(visitor: HtmlGenerator.HtmlGeneratingVisitor, text: String, node: ASTNode) {
              syntaxHighlighter.processNode(visitor, text, ensureAtLeastOneNodeAfterLang(node))
            }

            // CodeFenceSyntaxHighlighterGeneratingProvider expects at least one node after lang node,
            // which might not be present during streaming.
            private fun ensureAtLeastOneNodeAfterLang(node: ASTNode): ASTNode {
              val last = node.children.last()
              return if (last.type != MarkdownTokenTypes.FENCE_LANG)
                node
              else
                CompositeASTNode(node.type, node.children + LeafASTNode(MarkdownElementType("EOL"), last.endOffset, last.endOffset))
            }
          }
        )
      }
    }
    val parser = MarkdownParser(flavour)
    val component = createJBHtmlPane(JBHtmlPaneConfiguration.builder().build())
    return object : FormattedView {
      override val component: Component = JBScrollPane(component).apply {
        border = JBUI.Borders.empty()
        isOpaque = false
        viewport.isOpaque = false
        verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_NEVER
        setHorizontalScrollBar(JBThinOverlappingScrollBar(Adjustable.HORIZONTAL))
        isOverlappingScrollBar = true
      }
      override fun updateText(text: String) {
        component.text = convertMarkdownToHtml(parser, flavour, text)
        component.invalidate()
      }
    }
  }
}

private fun createJBHtmlPane(jbHtmlPaneConfiguration: JBHtmlPaneConfiguration): JBHtmlPane = object : JBHtmlPane(
  JBHtmlPaneStyleConfiguration.builder().build(),
  jbHtmlPaneConfiguration
) {
  // Height should never be equal 0
  override fun getPreferredSize(): Dimension? {
    return super.getPreferredSize()?.let {
      if (it.height == 0) {
        it.height = 1
      }
      it
    }
  }

}.apply {
  isOpaque = false
  border = null
  font = JBUI.Fonts.label()
}

private val embeddedHtmlType = IElementType("ROOT")

fun convertMarkdownToHtml(parser: MarkdownParser, flavour: MarkdownFlavourDescriptor, @NlsSafe markdownText: String): String {
  val parsedTree = parser.parse(embeddedHtmlType, markdownText)
  return HtmlGenerator(markdownText, parsedTree, flavour).generateHtml()
}