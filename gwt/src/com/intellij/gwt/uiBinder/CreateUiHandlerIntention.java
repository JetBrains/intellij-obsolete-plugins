package com.intellij.gwt.uiBinder;

import com.intellij.codeInsight.generation.GenerationInfo;
import com.intellij.gwt.GwtBundle;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.gwt.actions.GenerateUiHandlerMethodHandler.chooseEventClass;
import static com.intellij.gwt.actions.GenerateUiHandlerMethodHandler.generateMemberPrototype;
import static com.intellij.gwt.uiBinder.UiBinderUtil.HAS_HANDLERS_CLASS;
import static com.intellij.gwt.uiBinder.UiBinderUtil.WIDGET_BASE_CLASS;
import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;
import static com.intellij.openapi.util.text.StringUtil.trimEnd;
import static com.intellij.openapi.util.text.StringUtil.wrapWithDoubleQuote;
import static com.intellij.psi.util.TypeConversionUtil.isAssignable;
import static java.util.Collections.singletonList;

public class CreateUiHandlerIntention extends GwtUiXmlUiFieldIntentionBase {

  @Override
  protected void invokeInternal(Project project, XmlAttributeValue attributeValue, PsiClass psiClass) {
    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
    GlobalSearchScope resolveScope = psiClass.getResolveScope();

    PsiType fieldType = getUiFieldType(elementFactory, resolveScope, attributeValue);
    if (fieldType == null) return;

    PsiClass eventClass = getEventClass(project, resolveScope, fieldType);
    if (eventClass == null) return;

    String uiFieldName = attributeValue.getValue();
    @NonNls String methodName = uiFieldName + trimEnd(eventClass.getName(), "Event");
    String annotationParam = wrapWithDoubleQuote(uiFieldName);

    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
    GenerationInfo prototype = generateMemberPrototype(elementFactory, codeStyleManager, psiClass, eventClass,
                                                       methodName, annotationParam, false);
    runWriteCommandAction(project, () -> {
      PsiMethod lastMethod = null, lastUiHandlerMethod = null;
      for (PsiMethod psiMethod : psiClass.getMethods()) {
        if (UiBinderUtil.isUiHandlerMethod(psiMethod)) {
          lastUiHandlerMethod = psiMethod;
        }
        lastMethod = psiMethod;
      }
      if (lastUiHandlerMethod != null) {
        lastMethod = lastUiHandlerMethod;
      }
      prototype.insert(psiClass, lastMethod, false);
    });
  }

  protected PsiClass getEventClass(Project project, GlobalSearchScope resolveScope, PsiType fieldType) {
    return chooseEventClass(project, resolveScope, singletonList(fieldType));
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
    Pair<XmlAttributeValue, PsiClass> attribute = findAttribute(psiFile, editor);
    if (attribute == null) return false;

    XmlAttributeValue attributeValue = attribute.getFirst();

    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
    GlobalSearchScope resolveScope = psiFile.getResolveScope();
    PsiType type = getUiFieldType(elementFactory, resolveScope, attributeValue);
    if (type == null) return false;

    return isAssignable(elementFactory.createTypeByFQClassName(WIDGET_BASE_CLASS, resolveScope), type)
        || isAssignable(elementFactory.createTypeByFQClassName(HAS_HANDLERS_CLASS, resolveScope), type);
  }

  private static PsiType getUiFieldType(PsiElementFactory elementFactory, GlobalSearchScope resolveScope, XmlAttributeValue attributeValue) {
    XmlTag tag = PsiTreeUtil.getParentOfType(attributeValue, XmlTag.class);
    if (tag == null) return null;

    String className = UiBinderUtil.getComponentClassName(tag);
    return elementFactory.createTypeByFQClassName(className, resolveScope);
  }

  @Override
  protected PsiClass filterClass(List<PsiClass> classes, String fieldName) {
    return classes.isEmpty() ? null : classes.get(0);
  }

  @Override
  public @NotNull String getText() {
    return GwtBundle.message("intention.text.create.uihandler.for.tag");
  }
}
