package com.intellij.gwt.inspections;

import com.intellij.codeInsight.intention.AddAnnotationModCommandAction;
import com.intellij.codeInsight.intention.IntentionManager;
import com.intellij.codeInsight.intention.QuickFixFactory;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.uiBinder.GwtUiXmlFileUtil;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.gwt.uiBinder.mapping.UiBinderMappingService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.MultiValuesMap;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class GwtUiFieldErrorsInspection extends BaseGwtInspection {
  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    final GwtFacet facet = getFacet(file);
    if (facet == null) return null;

    if (file instanceof XmlFile xmlFile) {
      final MultiValuesMap<String,XmlAttributeValue> map = GwtUiXmlFileUtil.getFieldNameToAttributeMap(xmlFile);
      if (map != null && !map.isEmpty()) {
        final List<PsiClass> psiClasses = UiBinderMappingService.getInstance(facet.getModule()).getBoundClasses(xmlFile);
        if (psiClasses.isEmpty()) {
          return new ProblemDescriptor[]{
            manager.createProblemDescriptor(xmlFile, GwtBundle
                                              .message("problem.descriptor.description.template.uibinder.class.not.found.for.0", xmlFile.getName()), isOnTheFly,
                                            LocalQuickFix.EMPTY_ARRAY, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)};
        }

        List<ProblemDescriptor> problems = new SmartList<>();
        for (Map.Entry<String, Collection<XmlAttributeValue>> entry : map.entrySet()) {
          final String fieldName = entry.getKey();
          final Collection<XmlAttributeValue> attributes = entry.getValue();
          if (attributes.size() > 1) {
            for (XmlAttributeValue attribute : attributes) {
              problems.add(manager.createProblemDescriptor(attribute, getValueRange(attribute), GwtBundle
                                                             .message("problem.descriptor.description.template.duplicate.declaration.bound.to.0.field", fieldName),
                                                           ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly));
            }
          }

          final XmlAttributeValue attribute = ContainerUtil.getFirstItem(attributes, null);
          if (attribute == null || StringUtil.isEmpty(fieldName)) continue;

          for (PsiClass psiClass : psiClasses) {
            final PsiField field = psiClass.findFieldByName(fieldName, true);
            if (field != null) {
              checkUiField(field, manager, isOnTheFly, attribute, getValueRange(attribute), attribute, problems);
            }
          }
        }
        return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
      }
    }
    else if (file instanceof PsiJavaFile) {
      List<ProblemDescriptor> problems = new SmartList<>();
      for (PsiClass psiClass : ((PsiJavaFile)file).getClasses()) {
        final List<XmlFile> xmlFiles = UiBinderMappingService.getInstance(facet.getModule()).getUiXmlFiles(psiClass);
        for (XmlFile xmlFile : xmlFiles) {
          checkUiBinderClass(psiClass, xmlFile, manager, isOnTheFly, problems);
        }
      }
      return problems.toArray(ProblemDescriptor.EMPTY_ARRAY);
    }

    return super.checkFile(file, manager, isOnTheFly);
  }

  private static TextRange getValueRange(XmlAttributeValue attribute) {
    return attribute.getValueTextRange().shiftRight(-attribute.getTextRange().getStartOffset());
  }

  private static void checkUiBinderClass(PsiClass psiClass, XmlFile xmlFile, InspectionManager manager, boolean onTheFly,
                                  List<ProblemDescriptor> problems) {
    final MultiValuesMap<String, XmlAttributeValue> attributes = GwtUiXmlFileUtil.getFieldNameToAttributeMap(xmlFile);

    for (PsiField field : psiClass.getFields()) {
      final XmlAttributeValue attribute = attributes != null ? attributes.getFirst(field.getName()) : null;
      final PsiIdentifier nameIdentifier = field.getNameIdentifier();
      if (attribute != null) {
        checkUiField(field, manager, onTheFly, nameIdentifier, new TextRange(0, nameIdentifier.getTextLength()), attribute, problems);
      }
      else if (UiBinderUtil.isUiField(field)) {
        problems.add(manager.createProblemDescriptor(nameIdentifier, GwtBundle
                                                       .message("problem.descriptor.description.template.0.field.is.not.bound.to.tag.in.1.file", field.getName(), xmlFile.getName()),
                                                     onTheFly, LocalQuickFix.EMPTY_ARRAY, ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
      }
    }
  }

  private static void checkUiField(@NotNull PsiField field, InspectionManager manager, boolean isOnTheFly, PsiElement place, TextRange rangeInPlace,
                                   XmlAttributeValue attribute,
                                   List<ProblemDescriptor> problems) {
    if (!UiBinderUtil.isUiField(field)) {
      problems.add(manager.createProblemDescriptor(place, rangeInPlace, GwtBundle
                                                     .message("problem.descriptor.description.template.0.is.not.annotated.with.uifield", field.getName()),
                                                   ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly,
                                                   LocalQuickFix.from(new AddAnnotationModCommandAction(UiBinderUtil.UI_FIELD_ANNOTATION, field))));
    }
    else if (field.hasModifierProperty(PsiModifier.PRIVATE)) {
      final LocalQuickFix fix = IntentionManager.getInstance().convertToFix(QuickFixFactory.getInstance().createModifierListFix(field, PsiModifier.PRIVATE, false, false));
      problems.add(manager.createProblemDescriptor(place, rangeInPlace, GwtBundle
                                                     .message("problem.descriptor.description.template.uifield.0.should.not.be.private", field.getName()),
                                                   ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly, fix));
    }
    else {
      final XmlTag tag = PsiTreeUtil.getParentOfType(attribute, XmlTag.class);
      if (tag != null && tag.isValid()) {
        final String className = UiBinderUtil.getComponentClassName(tag);
        final PsiClassType tagType = JavaPsiFacade.getElementFactory(field.getProject()).createTypeByFQClassName(className, field.getResolveScope());
        final PsiType fieldType = UiBinderUtil.getUnwrappedUiFieldType(field);

        PsiType expectedType;
        PsiType actualType;
        if (UiBinderUtil.isProvidedUiField(field)) {
          expectedType = tagType;
          actualType = fieldType;
        }
        else {
          expectedType = fieldType;
          actualType = tagType;
        }
        if (!TypeConversionUtil.isAssignable(expectedType, actualType)) {
          LocalQuickFix fix = new SetCorrectUiFieldTypeFix(field, tagType);
          problems.add(manager.createProblemDescriptor(place, rangeInPlace, GwtBundle
                                                         .message("problem.descriptor.description.template.expected.0.but.1.found", expectedType.getCanonicalText(),
                                                                  actualType.getCanonicalText()),
                                                       ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly, fix));
        }
      }
    }
  }

  private static final class SetCorrectUiFieldTypeFix extends BaseGwtLocalQuickFixOnPsiElement {
    @SafeFieldForPreview private final PsiType myExpectedType;

    private SetCorrectUiFieldTypeFix(PsiField field, PsiType expectedType) {
      super(GwtBundle.message("quickfix.family.name.change.field.type"), GwtBundle.message("quickfix.name.change.0.type.to.1", field.getName(), expectedType.getCanonicalText()), field);
      myExpectedType = expectedType;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      if (!(startElement instanceof PsiField psiField)) return;

      if (!CommonRefactoringUtil.checkReadOnlyStatus(project, psiField)) {
        return;
      }

      final PsiTypeElement typeElement = psiField.getTypeElement();
      LOG.assertTrue(typeElement != null, psiField.getText());
      typeElement.replace(JavaPsiFacade.getElementFactory(project).createTypeElement(myExpectedType));
      JavaCodeStyleManager.getInstance(project).shortenClassReferences(psiField);
    }
  }
}
