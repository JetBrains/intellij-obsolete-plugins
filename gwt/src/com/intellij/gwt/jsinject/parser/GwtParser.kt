package com.intellij.gwt.jsinject.parser

import com.intellij.gwt.jsinject.JSGwtReferenceExpressionImpl
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.javascript.JSCompositeElementType
import com.intellij.lang.javascript.JSKeywordSets
import com.intellij.lang.javascript.JSTokenTypes
import com.intellij.lang.javascript.JavaScriptParserBundle.message
import com.intellij.lang.javascript.parsing.ExpressionParser
import com.intellij.lang.javascript.parsing.JSFunctionParser
import com.intellij.lang.javascript.parsing.JavaScriptParser
import com.intellij.psi.tree.IElementType

internal class GwtParser(
  builder: PsiBuilder,
) : JavaScriptParser(
  GwtLanguageDialect.GWT_DIALECT,
  builder,
) {
  override val expressionParser: ExpressionParser<*> =
    object : ExpressionParser<GwtParser>(this@GwtParser) {
      override fun isReferenceQualifierSeparator(
        tokenType: IElementType?,
      ): Boolean {
        return tokenType === JSTokenTypes.DOT ||
               tokenType === JSTokenTypes.COLON_COLON
      }

      override fun parseSpecialReference(): Boolean {
        if (builder.tokenType === JSTokenTypes.AT) {
          parseGwtReferenceExpression()
          return true
        }
        return false
      }

      private fun parseGwtReferenceExpression() {
        val gwtExpr = builder.mark()
        LOG.assertTrue(builder.tokenType === JSTokenTypes.AT)
        builder.advanceLexer()
        while (true) {
          if (isIdentifierToken(builder.tokenType)) {
            builder.advanceLexer()
          }
          else {
            builder.error(message("javascript.parser.message.expected.name"))
          }
          if (builder.tokenType !== JSTokenTypes.DOT) {
            break
          }
          builder.advanceLexer()
        }
        if (builder.tokenType === JSTokenTypes.COLON_COLON) {
          builder.advanceLexer()
          if (builder.tokenType === JSTokenTypes.GWT_FIELD_OR_METHOD) {
            builder.advanceLexer()
          }
        }
        gwtExpr.done(GWT_REFERENCE_EXPRESSION)
      }
    }

  override val functionParser: JSFunctionParser<*> = object : JSFunctionParser<GwtParser>(this@GwtParser) {
    override fun hasSupportDecorators(): Boolean {
      return false
    }
  }

  override fun isIdentifierToken(
    tokenType: IElementType?,
  ): Boolean {
    return JSKeywordSets.GWT_IDENTIFIER_TOKENS_SET.contains(tokenType)
  }

  companion object {
    private val GWT_REFERENCE_EXPRESSION: IElementType =
      object : JSCompositeElementType("GWT_REFERENCE_EXPRESSION") {
        override fun createCompositeNode(): ASTNode {
          return JSGwtReferenceExpressionImpl(this)
        }
      }
  }
}
