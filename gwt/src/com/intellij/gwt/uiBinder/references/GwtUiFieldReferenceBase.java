package com.intellij.gwt.uiBinder.references;

import com.intellij.codeInsight.completion.JavaLookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.gwt.uiBinder.GwtUiXmlFileUtil;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.openapi.util.MultiValuesMap;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class GwtUiFieldReferenceBase<T extends PsiElement> extends PsiPolyVariantReferenceBase<T> {
  private final boolean mySearchInXml;

  public GwtUiFieldReferenceBase(T element, final boolean searchInXml) {
    super(element, true);
    mySearchInXml = searchInXml;
  }

  protected abstract String getFieldName();

  protected abstract @NotNull List<PsiClass> findUiBinderClasses();

  protected abstract @NotNull List<XmlFile> findUiXmlFiles();

  @Override
  public Object @NotNull [] getVariants() {
    final List<PsiClass> psiClasses = findUiBinderClasses();

    Map<String, LookupElementBuilder> lookupElements = new LinkedHashMap<>();
    for (PsiClass psiClass : psiClasses) {
      for (PsiField field : psiClass.getAllFields()) {
        if (UiBinderUtil.isUiField(field)) {
          lookupElements.put(field.getName(), JavaLookupElementBuilder.forField(field));
        }
      }
    }

    if (mySearchInXml) {
      for (XmlFile xmlFile : findUiXmlFiles()) {
        final MultiValuesMap<String,XmlAttributeValue> fields = GwtUiXmlFileUtil.getFieldNameToAttributeMap(xmlFile);
        if (fields != null) {
          for (String fieldName : fields.keySet()) {
            final XmlAttributeValue attributeValue = fields.getFirst(fieldName);
            final XmlTag tag = PsiTreeUtil.getParentOfType(attributeValue, XmlTag.class);
            if (!lookupElements.containsKey(fieldName) && tag != null) {
              lookupElements.put(fieldName, LookupElementBuilder.create(tag, fieldName));
            }
          }
        }
      }
    }
    return lookupElements.values().toArray(new LookupElementBuilder[0]);
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    List<PsiElement> result = new SmartList<>();
    final String value = getFieldName();
    for (PsiClass psiClass : findUiBinderClasses()) {
      ContainerUtil.addIfNotNull(result, psiClass.findFieldByName(value, true));
    }
    if (result.isEmpty() && mySearchInXml) {
      for (XmlFile xmlFile : findUiXmlFiles()) {
        ContainerUtil.addIfNotNull(result, GwtUiXmlFileUtil.findTagForField(xmlFile, value));
      }
    }
    return PsiElementResolveResult.createResults(result);
  }
}
