package com.intellij.gwt.uiBinder;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.XmlCompletionContributor;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.util.Query;
import com.intellij.xml.XmlExtension;
import org.jetbrains.annotations.NotNull;

import static com.intellij.gwt.uiBinder.UiBinderUtil.URN_IMPORT_PREFIX;
import static com.intellij.gwt.uiBinder.UiBinderUtil.isUiXmlFile;
import static com.intellij.psi.util.PsiTreeUtil.getParentOfType;
import static com.intellij.xml.util.XmlTagUtil.getStartTagNameElement;

public final class GwtComponentTagCompletionContributor extends CompletionContributor {
  @Override
  public void fillCompletionVariants(final @NotNull CompletionParameters parameters, final @NotNull CompletionResultSet result) {
    super.fillCompletionVariants(parameters, result);
    if (!parameters.isExtendedCompletion() || !XmlCompletionContributor.isXmlNameCompletion(parameters)) {
      return;
    }

    final PsiFile file = parameters.getOriginalFile();
    if (!(file instanceof XmlFile) || !isUiXmlFile((XmlFile)file)) {
      return;
    }

    Query<PsiClass> search = ReadAction.computeBlocking(() -> {
      GlobalSearchScope scope = file.getResolveScope();
      PsiClass widgetBaseClass = JavaPsiFacade.getInstance(file.getProject()).findClass(UiBinderUtil.WIDGET_BASE_CLASS, scope);
      if (widgetBaseClass != null) {
        return ClassInheritorsSearch.search(widgetBaseClass, scope, true);
      }
      return null;
    });
    if (search == null) return;

    search.forEach(psiClass -> {
      final String name = psiClass.getName();
      if (name == null || !result.getPrefixMatcher().prefixMatches(name)) return true;

      final String qualifiedName = psiClass.getQualifiedName();
      if (qualifiedName == null) return true;

      final String packageName = StringUtil.getPackageName(qualifiedName);
      String namespace = URN_IMPORT_PREFIX + packageName;
      if (GwtUiXmlSchemaProvider.isDefaultSchema(namespace)) {
        //let XmlCompletionContributor do it work
        return true;
      }

      final LookupElement element = XmlCompletionContributor.createLookupElement(new XmlExtension.TagInfo(name, namespace), packageName, null);
      result.addElement(element);
      return true;
    });
  }

  @Override
  public void beforeCompletion(@NotNull CompletionInitializationContext context) {
    super.beforeCompletion(context);

    PsiFile file = context.getFile();
    if (file instanceof XmlFile && isUiXmlFile((XmlFile)file)) {
      PsiElement element = file.findElementAt(context.getStartOffset());

      XmlTag xmlTag = getParentOfType(element, XmlTag.class, true);
      if (xmlTag != null && !xmlTag.getNamespacePrefix().isEmpty()) {
        XmlToken startTagNameElement = getStartTagNameElement(xmlTag);
        if (startTagNameElement != null) {
          int endOffset = startTagNameElement.getTextRange().getEndOffset();
          if (endOffset >= context.getSelectionEndOffset()) {
            context.setReplacementOffset(endOffset);
          }
        }
      }
    }
  }
}
