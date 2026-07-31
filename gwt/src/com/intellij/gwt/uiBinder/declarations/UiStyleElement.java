package com.intellij.gwt.uiBinder.declarations;

import com.intellij.gwt.clientBundle.css.GwtCssDeclarationsManager;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.css.CssClass;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import com.intellij.util.SmartList;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.util.containers.ContainerUtil.findInstance;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toCollection;

public class UiStyleElement extends UiXmlVariableDeclarationImpl {
  public static final @NonNls String SRC_ATTRIBUTE = "src";
  public static final @NonNls String DEFAULT_FIELD_NAME = "style";

  public UiStyleElement(@NotNull XmlTag tag) {
    super(tag, DEFAULT_FIELD_NAME);
  }

  public MultiMap<String, CssClass> collectCssDeclarations() {
    final MultiMap<String, CssClass> declarations = new MultiMap<>();
    for (StylesheetFile externalFile : getStylesheetFiles()) {
      GwtCssDeclarationsManager.collectDeclarations(externalFile, CssClass.class, declarations);
    }

    return declarations;
  }

  private @NotNull List<StylesheetFile> findExternalFiles() {
    XmlAttribute srcXmlAttribute = myTag.getAttribute(SRC_ATTRIBUTE);
    if (srcXmlAttribute != null) {

      XmlAttributeValue srcXmlAttributeValueElement = srcXmlAttribute.getValueElement();
      if (srcXmlAttributeValueElement != null) {

        PsiReference[] srcXmlAttributeValueReferences = srcXmlAttributeValueElement.getReferences();
        FileReference anyFileReference = findInstance(srcXmlAttributeValueReferences, FileReference.class);
        if (anyFileReference != null) {

          FileReference cssFileReference = anyFileReference.getFileReferenceSet().getLastReference();
          if (cssFileReference != null) {

            ResolveResult[] resolveResults = cssFileReference.multiResolve(false);
            if (resolveResults.length > 0) {
              return stream(resolveResults).map(ResolveResult::getElement).filter(StylesheetFile.class::isInstance)
                    .map(StylesheetFile.class::cast).collect(toCollection(SmartList::new));
            }
          }
        }
      }
    }
    return emptyList();
  }

  public @NotNull List<StylesheetFile> getStylesheetFiles() {
    List<StylesheetFile> files = findExternalFiles();

    for (XmlText text : myTag.getValue().getTextElements()) {
      final List<Pair<PsiElement, TextRange>> pairs = InjectedLanguageManager.getInstance(text.getProject()).getInjectedPsiFiles(text);
      if (pairs != null) {
        for (Pair<PsiElement, TextRange> pair : pairs) {
          if (pair.getFirst() instanceof StylesheetFile) {
            if (files.isEmpty()) { // might be immutable
              files = new SmartList<>((StylesheetFile)pair.getFirst());
            } else {
              files.add((StylesheetFile)pair.getFirst());
            }
          }
        }
      }
    }
    return files;
  }
}
