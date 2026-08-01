package com.intellij.gwt.uiBinder;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.gwt.uiBinder.mapping.UiBinderMappingService;
import com.intellij.openapi.module.Module;
import com.intellij.util.containers.MultiMap;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.intellij.codeInsight.completion.PrioritizedLookupElement.withPriority;
import static com.intellij.codeInsight.lookup.AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE;
import static com.intellij.codeInsight.lookup.LookupElementBuilder.create;
import static com.intellij.gwt.uiBinder.GwtUiXmlFileUtil.getFieldNameToAttributeMap;
import static com.intellij.openapi.module.ModuleUtilCore.findModuleForPsiElement;
import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.psi.PsiReferenceRegistrar.HIGHER_PRIORITY;

public final class GwtUiFieldNameCompletionContributor extends CompletionContributor {


  public GwtUiFieldNameCompletionContributor() {
    PsiElementPattern.Capture<PsiIdentifier> uiFieldPattern = psiElement(PsiIdentifier.class).withParent(PsiField.class);
    extend(CompletionType.BASIC, uiFieldPattern, new CompletionProvider<>() {

      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    @NotNull ProcessingContext context,
                                    @NotNull CompletionResultSet result) {
        PsiField psiField = (PsiField)parameters.getPosition().getParent();
        if (!UiBinderUtil.isUiField(psiField)) return;

        PsiClass psiClass = psiField.getContainingClass();
        if (psiClass == null) return;

        Module module = findModuleForPsiElement(psiClass);
        if (module == null) return;

        UiBinderMappingService mappingService = UiBinderMappingService.getInstance(module);
        List<XmlFile> uiXmlFiles = mappingService.getUiXmlFiles(psiClass);
        if (uiXmlFiles.isEmpty()) return;

        for (XmlFile uiXmlFile : uiXmlFiles) {
          MultiMap<String, XmlAttributeValue> fieldNameToAttributeMap = getFieldNameToAttributeMap(uiXmlFile);
          if (fieldNameToAttributeMap == null) continue;

          for (Map.Entry<String, Collection<XmlAttributeValue>> entry : fieldNameToAttributeMap.entrySet()) {
            String fieldName = entry.getKey();
            for (XmlAttributeValue attribute : entry.getValue()) {
              XmlTag tag = PsiTreeUtil.getParentOfType(attribute, XmlTag.class);
              if (tag != null && tag.isValid()) {
                String className = UiBinderUtil.getComponentClassName(tag);
                PsiClassType tagType = JavaPsiFacade.getElementFactory(psiField.getProject())
                  .createTypeByFQClassName(className, psiField.getResolveScope());
                PsiType psiFieldType = UiBinderUtil.getUnwrappedUiFieldType(psiField);

                if (TypeConversionUtil.isAssignable(psiFieldType, tagType)) {
                  result.addElement(withPriority(
                    create(fieldName).withAutoCompletionPolicy(GIVE_CHANCE_TO_OVERWRITE), HIGHER_PRIORITY));
                }
              }
            }
          }
        }
      }
    });
  }
}
