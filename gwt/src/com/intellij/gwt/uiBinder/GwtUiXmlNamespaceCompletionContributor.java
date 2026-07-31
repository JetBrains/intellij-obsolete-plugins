package com.intellij.gwt.uiBinder;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.gwt.uiBinder.references.GwtUiReferenceContributor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.codeInsight.completion.CompletionType.BASIC;
import static com.intellij.codeInsight.completion.PrioritizedLookupElement.withPriority;
import static com.intellij.codeInsight.lookup.Lookup.REPLACE_SELECT_CHAR;
import static com.intellij.codeInsight.lookup.LookupElementBuilder.create;
import static com.intellij.gwt.uiBinder.UiBinderUtil.URN_IMPORT_PREFIX;
import static com.intellij.gwt.uiBinder.UiBinderUtil.isUiXmlFile;
import static com.intellij.openapi.module.ModuleUtilCore.findModuleForPsiElement;
import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.StandardPatterns.or;
import static com.intellij.patterns.StandardPatterns.string;
import static com.intellij.patterns.XmlPatterns.xmlAttribute;
import static com.intellij.patterns.XmlPatterns.xmlAttributeValue;
import static com.intellij.psi.PsiReferenceRegistrar.DEFAULT_PRIORITY;
import static com.intellij.psi.PsiReferenceRegistrar.HIGHER_PRIORITY;
import static com.intellij.psi.PsiReferenceRegistrar.LOWER_PRIORITY;
import static com.intellij.psi.search.GlobalSearchScope.allScope;

public final class GwtUiXmlNamespaceCompletionContributor extends CompletionContributor {

  public static final ElementPattern<? extends PsiElement> XMLNS_VALUE_PATTERN = psiElement().inside(
    xmlAttributeValue(xmlAttribute().withName(or(string().longerThan(6).startsWith("xmlns:"), string().matches("xmlns"))))
  ).inFile(GwtUiReferenceContributor.Holder.UI_XML_FILE_PATTERN);

  public GwtUiXmlNamespaceCompletionContributor() {
    extend(BASIC, XMLNS_VALUE_PATTERN, new CompletionProvider<>() {

      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    @NotNull ProcessingContext context,
                                    @NotNull CompletionResultSet result) {
        String prefix = result.getPrefixMatcher().getPrefix();
        if (!prefix.startsWith(URN_IMPORT_PREFIX)) {
          result.addElement(withPriority(create(URN_IMPORT_PREFIX).withInsertHandler((insertionContext, item) -> {
            if (insertionContext.getCompletionChar() == REPLACE_SELECT_CHAR) {
              AutoPopupController.getInstance(insertionContext.getProject()).scheduleAutoPopup(insertionContext.getEditor());
            }
          }), HIGHER_PRIORITY));
        }
        else {
          PsiElement position = parameters.getPosition();
          Project project = position.getProject();
          JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);

          Module module = findModuleForPsiElement(position);
          GlobalSearchScope scope = module == null ? allScope(project) : module.getModuleWithDependenciesAndLibrariesScope(true);

          String fullPackageName = prefix.substring(URN_IMPORT_PREFIX.length());
          PsiPackage fullPsiPackage = psiFacade.findPackage(fullPackageName);
          if (fullPsiPackage != null) {
            for (final PsiPackage subPackage : fullPsiPackage.getSubPackages(scope)) {
              addResult(result, fullPackageName, subPackage.getQualifiedName(), LOWER_PRIORITY);
            }
          }

          int lastDotIndex = fullPackageName.lastIndexOf('.');
          String packageName = lastDotIndex == -1 ? "" : fullPackageName.substring(0, lastDotIndex);
          PsiPackage psiPackage = psiFacade.findPackage(packageName);
          if (psiPackage != null) {
            for (final PsiPackage subPackage : psiPackage.getSubPackages(scope)) {
              addResult(result, packageName, subPackage.getQualifiedName(), DEFAULT_PRIORITY);
            }
          }
        }
      }

      private void addResult(@NotNull CompletionResultSet result,
                             @NotNull String packageName,
                             @NotNull String subPackageName,
                             double priority) {
        subPackageName = subPackageName.trim();
        if (subPackageName.startsWith(packageName)) {
          int startIndex = packageName.isEmpty() ? 0 : packageName.length() + 1;
          if (subPackageName.indexOf('.', startIndex) < 0) {
            result.addElement(withPriority(create(URN_IMPORT_PREFIX + subPackageName), priority));
          }
        }
      }
    });
  }

  @Override
  public void beforeCompletion(@NotNull CompletionInitializationContext context) {
    super.beforeCompletion(context);
    PsiFile file = context.getFile();
    if (file instanceof XmlFile && isUiXmlFile((XmlFile)file)) {
      int contextStartOffset = context.getStartOffset();
      PsiElement element = file.findElementAt(contextStartOffset);
      if (element != null && XMLNS_VALUE_PATTERN.accepts(element)) {
        int elementTextOffset = element.getTextRange().getStartOffset();
        int offsetInElement = contextStartOffset - elementTextOffset;
        if (offsetInElement >= URN_IMPORT_PREFIX.length()) {
          int dotIndex = element.getText().indexOf('.', offsetInElement);
          if (dotIndex != -1) {
            context.setReplacementOffset(elementTextOffset + dotIndex);
          }
        }
      }
    }
  }

  public static boolean invokeNamespaceAutoPopup(char typeChar) {
    return typeChar == '.' || typeChar == ':';
  }

  @Override
  public boolean invokeAutoPopup(@NotNull PsiElement position, char typeChar) {
    return invokeNamespaceAutoPopup(typeChar);
  }
}
