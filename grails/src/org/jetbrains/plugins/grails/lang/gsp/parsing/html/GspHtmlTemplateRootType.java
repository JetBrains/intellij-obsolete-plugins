// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.parsing.html;

import com.intellij.ide.highlighter.HtmlFileType;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.lexer.Lexer;
import com.intellij.lexer.LexerUtil;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.source.DummyHolder;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.Factory;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.SharedImplUtil;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.CharTable;
import com.intellij.util.LocalTimeCounter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspTokenTypesEx;
import org.jetbrains.plugins.grails.lang.gsp.parsing.gsp.lexer.GspLexer;
import org.jetbrains.plugins.grails.lang.gsp.psi.html.impl.GspHtmlOuterElementImpl;

public class GspHtmlTemplateRootType extends IFileElementType {

  public GspHtmlTemplateRootType(@NonNls String debugName) {
    super(debugName, HTMLLanguage.INSTANCE);
  }

  @Override
  public ASTNode parseContents(@NotNull ASTNode chameleon) {
    CharTable table = SharedImplUtil.findCharTableByTree(chameleon);
    PsiManagerEx manager = ((TreeElement)chameleon).getManager();
    CharSequence chars = chameleon.getChars();

    // Create HTML file without any GSP occurrences
    String templateText = createTemplateText(chars);
    PsiFile templateFile = createHtmlFileFromText(templateText, manager);

    TreeElement parsed = ((PsiFileImpl) templateFile).calcTreeElement();

    Lexer lexer = new GspLexer();
    lexer.start(chars);

    insertOuters(parsed, lexer, table);

    FileElement treeElement = new DummyHolder(manager, null, table).getTreeElement();

    treeElement.rawAddChildren(parsed.getFirstChildNode());
    treeElement.subtreeChanged();
    return treeElement.getFirstChildNode();
  }

  private static String createTemplateText(CharSequence buf) {
    GspLexer lexer = new GspLexer();

    StringBuilder result = new StringBuilder(buf.length());
    lexer.start(buf);

    IElementType tokenType;
    while ((tokenType = lexer.getTokenType()) != null) {
      if (tokenType == GspTokenTypesEx.GSP_TEMPLATE_DATA || tokenType == XmlTokenType.XML_WHITE_SPACE) {
        result.append(buf, lexer.getTokenStart(), lexer.getTokenEnd());
      }
      lexer.advance();
    }

    return result.toString(); // Don't worry about creation of new char[] during 'toString()' call. JVM optimize it, new array will not be created.
  }


  private static PsiFile createHtmlFileFromText(String text, PsiManager manager) {
    LightVirtualFile virtualFile = new LightVirtualFile("foo", HtmlFileType.INSTANCE, text, LocalTimeCounter.currentTime());

    FileViewProvider viewProvider = new SingleRootFileViewProvider(manager, virtualFile, false) {
      @Override
      public @NotNull Language getBaseLanguage() {
        return HTMLLanguage.INSTANCE;
      }
    };

    return viewProvider.getPsi(HTMLLanguage.INSTANCE);
  }

  private static void insertOuters(TreeElement root, Lexer lexer, final CharTable table) {
    PsiManager manager = root.getManager();

    int treeOffset = 0;
    LeafElement leaf = TreeUtil.findFirstLeaf(root);

    IElementType tt;
    while ((tt = lexer.getTokenType()) != null) {
      if (tt != GspTokenTypesEx.GSP_TEMPLATE_DATA && tt != XmlTokenType.XML_WHITE_SPACE) {
        int tokenStart = lexer.getTokenStart();
        while (leaf != null && treeOffset < tokenStart) {
          treeOffset += leaf.getTextLength();
          if (treeOffset > tokenStart) {
            leaf = split(manager, leaf, leaf.getTextLength() - (treeOffset - tokenStart), table);
            treeOffset = tokenStart;
          }
          leaf = TreeUtil.nextLeaf(leaf);
        }

        if (leaf == null) break;

        final GspHtmlOuterElementImpl element = createOuterElement(lexer, table);
        TreeElement anchor = insert(leaf.getTreeParent(), leaf, element);
        anchor.getTreeParent().subtreeChanged();
        leaf = element;
      }

      lexer.advance();
    }

    if (tt != null) {
      do {
        assert tt != GspTokenTypesEx.GSP_TEMPLATE_DATA && tt != XmlTokenType.XML_WHITE_SPACE;
        GspHtmlOuterElementImpl outerElement = createOuterElement(lexer, table);
        ((CompositeElement) root).rawAddChildren(outerElement);

        lexer.advance();
        tt = lexer.getTokenType();
      } while (tt != null);
      ((CompositeElement) root).subtreeChanged();
    }
  }

  private static GspHtmlOuterElementImpl createOuterElement(final Lexer lexer,
                                                       final CharTable table) {
    return new GspHtmlOuterElementImpl(GspTokenTypesEx.GSP_FRAGMENT_IN_HTML, LexerUtil.internToken(lexer, table));
  }

  private static TreeElement insert(CompositeElement parent, @NotNull TreeElement anchorBefore, OuterLanguageElement toInsert) {
    if (parent.getPsi() instanceof XmlTag &&
        anchorBefore.getElementType() == XmlTokenType.XML_START_TAG_START) {
      //XmlElementFactory.createXmlTextFromText(toInsert.getManager(), "");
      parent.rawInsertBeforeMe((TreeElement) toInsert);
      return parent;
    }

    anchorBefore.rawInsertBeforeMe((TreeElement) toInsert);
    return anchorBefore;
  }

  private static LeafElement split(PsiManager manager, LeafElement leaf, int offset, CharTable table) {
    final CharSequence chars = leaf.getChars();
    final LeafElement leftPart = Factory.createSingleLeafElement(leaf.getElementType(), chars, 0, offset, table, manager);
    final LeafElement rightPart = Factory.createSingleLeafElement(leaf.getElementType(), chars, offset, chars.length(), table, manager);
    leaf.rawInsertAfterMe(leftPart);
    leftPart.rawInsertAfterMe(rightPart);
    leaf.rawRemove();
    return leftPart;
  }

}
