package com.intellij.gwt.uiBinder.references;

import com.intellij.gwt.uiBinder.declarations.UiStyleElement;
import com.intellij.gwt.uiBinder.declarations.UiXmlVariableDeclaration;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.css.CssClass;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.SmartList;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QualifiedUiXmlReference extends PsiPolyVariantReferenceBase<XmlAttributeValue> implements UiXmlElementReference {
  private final UiXmlElementReference myQualifierReference;

  public QualifiedUiXmlReference(UiXmlElementReference qualifierReference, XmlAttributeValue element, TextRange range) {
    super(element, range, true);
    myQualifierReference = qualifierReference;
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    final PsiMethod method = findMethod();
    List<PsiElement> results = new SmartList<>();
    if (method != null) {
      results.add(method);
    }

    final MultiMap<String, CssClass> classesMap = getCssClasses();
    if (classesMap != null) {
      results.addAll(classesMap.get(getValue()));
    }

    return PsiElementResolveResult.createResults(results);
  }

  public UiXmlElementReference getQualifierReference() {
    return myQualifierReference;
  }

  private @Nullable MultiMap<String, CssClass> getCssClasses() {
    final UiStyleElement element = findStyleElement();
    if (element == null) return null;

    return element.collectCssDeclarations();
  }

  public @Nullable UiStyleElement findStyleElement() {
    if (!(myQualifierReference instanceof UiXmlVariableReference)) return null;
    final UiXmlVariableDeclaration declaration = ((UiXmlVariableReference)myQualifierReference).findDeclaration();
    return declaration instanceof UiStyleElement ? (UiStyleElement)declaration : null;
  }

  @Override
  public Object @NotNull [] getVariants() {
    Set<String> names = new HashSet<>();
    final PsiClass psiClass = resolveQualifierClass();
    if (psiClass != null) {
      for (PsiMethod method : psiClass.getMethods()) {
        names.add(method.getName());
      }
    }
    final MultiMap<String, CssClass> classes = getCssClasses();
    if (classes != null) {
      for (CssClass cssClass : classes.values()) {
        names.add(cssClass.getName());
      }
    }
    return ArrayUtilRt.toStringArray(names);
  }

  private @Nullable PsiClass resolveQualifierClass() {
    return PsiTypesUtil.getPsiClass(myQualifierReference.resolveVariableType());
  }

  private @Nullable PsiMethod findMethod() {
    final PsiClass psiClass = resolveQualifierClass();
    if (psiClass == null) return null;
    final String methodName = getValue();
    final PsiMethod[] methods = psiClass.findMethodsByName(methodName, true);
    return methods.length != 0 ? methods[0] : null;
  }

  @Override
  public PsiType resolveVariableType() {
    final PsiMethod method = findMethod();
    if (method == null) return null;

    return method.getReturnType();
  }
}
