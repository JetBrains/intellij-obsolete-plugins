// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.formatter.processors;

import com.intellij.formatting.Indent;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.formatter.xml.XmlFormattingPolicy;
import com.intellij.psi.html.HtmlTag;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.addins.js.JavaScriptIntegrationUtil;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspTokenTypesEx;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspXmlRootTag;

import static com.intellij.psi.xml.XmlTokenType.XML_DATA_CHARACTERS;

public final class GspIndentProcessor implements GspTokenTypesEx {

  private GspIndentProcessor() {
  }

  /**
   * Calculates indent, based on code style, between parent block and child node
   *
   * @param parent parent
   * @param child  child node
   * @return indent
   */
  public static @NotNull Indent getGspChildIndent(final @NotNull ASTNode parent,
                                         final @NotNull ASTNode child,
                                         XmlFormattingPolicy policy) {

    PsiElement parentPsi = parent.getPsi();
    PsiElement childPsi = child.getPsi();

    if (parentPsi instanceof GspXmlRootTag) {
      return Indent.getNoneIndent();
    }

    if (parentPsi instanceof XmlTag) {
      if (GspTokenTypesEx.GSP_GROOVY_SEPARATORS.contains(child.getElementType())) {
        return Indent.getNoneIndent();
      }
      if (childPsi instanceof XmlTag || childPsi instanceof XmlText || XML_DATA_CHARACTERS == child.getElementType()) {
        return indentForHtmlTag(policy, (XmlTag)parent);
      }
      if (childPsi instanceof OuterLanguageElement) {
        if (JavaScriptIntegrationUtil.isJavaScriptInjection(childPsi)) {
          return Indent.getNormalIndent();
        }
        return parentPsi instanceof HtmlTag ? indentForHtmlTag(policy, (HtmlTag) parentPsi) : Indent.getNormalIndent();
      }
      if (childPsi instanceof XmlAttribute) {
        return Indent.getContinuationIndent();
      }
    }

    return Indent.getNoneIndent();
  }

  public static Indent indentForHtmlTag(XmlFormattingPolicy policy, XmlTag parent) {
    return policy.indentChildrenOf(parent) ? Indent.getNormalIndent() : Indent.getNoneIndent();
  }
}
