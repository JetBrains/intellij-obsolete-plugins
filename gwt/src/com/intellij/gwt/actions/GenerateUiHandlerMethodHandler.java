package com.intellij.gwt.actions;

import com.intellij.CommonBundle;
import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.codeInsight.generation.GenerateMembersHandlerBase;
import com.intellij.codeInsight.generation.GenerationInfo;
import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.codeInsight.generation.PsiGenerationInfo;
import com.intellij.codeInsight.generation.TemplateGenerationInfo;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.inspections.GwtUiHandlerErrorsInspection;
import com.intellij.gwt.uiBinder.UiBinderUtil;
import com.intellij.ide.util.ClassFilter;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.intellij.gwt.uiBinder.UiBinderUtil.HAS_HANDLERS_CLASS;
import static com.intellij.gwt.uiBinder.UiBinderUtil.WIDGET_BASE_CLASS;
import static com.intellij.openapi.util.text.StringUtil.wrapWithDoubleQuote;
import static com.intellij.psi.JavaPsiFacade.getElementFactory;

public class GenerateUiHandlerMethodHandler extends GenerateMembersHandlerBase {
  private JBCheckBox mySeparateMethodsCheckBox;
  protected boolean myGenerateSeparateMethods;

  public GenerateUiHandlerMethodHandler() {
    super(GwtBundle.message("dialog.title.choose.fields.to.generate.handler.for"));
  }

  @Override
  protected ClassMember[] getAllOriginalMembers(PsiClass aClass) {
    List<ClassMember> members = new ArrayList<>();
    PsiClassType baseWidgetInterface = getElementFactory(aClass.getProject()).createTypeByFQClassName(WIDGET_BASE_CLASS, aClass.getResolveScope());
    PsiClassType hasHandlersInterface = getElementFactory(aClass.getProject()).createTypeByFQClassName(HAS_HANDLERS_CLASS, aClass.getResolveScope());
    for (PsiField field : aClass.getFields()) {
      PsiType type = field.getType();
      if (UiBinderUtil.isUiField(field) && (TypeConversionUtil.isAssignable(baseWidgetInterface, type)
                                            || TypeConversionUtil.isAssignable(hasHandlersInterface, type))) {
        members.add(new UiFieldClassMember(field));
      }
    }
    return members.toArray(ClassMember.EMPTY_ARRAY);
  }

  @Override
  protected ClassMember[] chooseOriginalMembers(PsiClass aClass, Project project) {
    final ClassMember[] members = super.chooseOriginalMembers(aClass, project);
    if (members == null) return null;
    myGenerateSeparateMethods = mySeparateMethodsCheckBox != null && mySeparateMethodsCheckBox.isSelected();
    mySeparateMethodsCheckBox = null;

    List<PsiType> fieldTypes = new ArrayList<>();
    for (ClassMember member : members) {
      fieldTypes.add(((UiFieldClassMember)member).getElement().getType());
    }

    PsiClass eventClass = chooseEventClass(project, aClass.getResolveScope(), fieldTypes);
    if (eventClass == null) {
      return null;
    }

    for (ClassMember member : members) {
      ((UiFieldClassMember)member).setEventClass(eventClass);
    }
    return members;
  }

  public static PsiClass chooseEventClass(Project project, GlobalSearchScope resolveScope, List<PsiType> fieldTypes) {
    PsiClass gwtEventClass = JavaPsiFacade.getInstance(project).findClass(UiBinderUtil.GWT_EVENT_CLASS, resolveScope);
    if (gwtEventClass == null) {
      Messages.showErrorDialog(project, GwtBundle.message("dialog.message.0.class.not.found", UiBinderUtil.GWT_EVENT_CLASS), CommonBundle.getErrorTitle());
      return null;
    }

    PsiTypeParameter[] typeParameters = gwtEventClass.getTypeParameters();
    if (typeParameters.length != 1) {
      Messages.showErrorDialog(project, GwtBundle
        .message("dialog.message.incorrect.number.of.type.parameters.in.0.class", UiBinderUtil.GWT_EVENT_CLASS), CommonBundle.getErrorTitle());
      return null;
    }

    TreeClassChooserFactory factory = TreeClassChooserFactory.getInstance(project);
    TreeClassChooser chooser = factory.createNoInnerClassesScopeChooser(GwtBundle.message("dialog.title.select.event.type"), resolveScope,
                                                                        new EventTypeFilter(gwtEventClass, typeParameters[0], fieldTypes), null);
    chooser.showDialog();
    return chooser.getSelected();
  }

  @Override
  protected @NotNull List<? extends GenerationInfo> generateMemberPrototypes(PsiClass aClass, ClassMember[] members)
      throws IncorrectOperationException {
    Project project = aClass.getProject();
    PsiElementFactory factory = getElementFactory(project);
    CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);

    PsiClass eventClass = ((UiFieldClassMember)members[0]).getEventClass();
    @NonNls String eventName = StringUtil.trimEnd(eventClass.getName(), "Event");
    if (myGenerateSeparateMethods) {
      List<GenerationInfo> infos = new ArrayList<>(members.length);
      for (ClassMember member : members) {
        UiFieldClassMember classMember = (UiFieldClassMember)member;
        String memberName = classMember.getElement().getName();
        String methodName = memberName + eventName;
        String annotationParam = wrapWithDoubleQuote(memberName);
        infos.add(generateMemberPrototype(factory, codeStyleManager, aClass, eventClass,
                                          methodName, annotationParam, false));
      }
      return infos;
    }
    else {
      @NonNls String methodName = "handle" + eventName;
      StringBuilder annotationParameters = new StringBuilder();
      if (members.length == 1) {
        String memberName = ((UiFieldClassMember)members[0]).getElement().getName();
        annotationParameters.append('"').append(memberName).append('"');
      }
      else {
        annotationParameters.append('{');
        for (int i = 0; i < members.length; i++) {
          if (i > 0) annotationParameters.append(',');
          String memberName = ((UiFieldClassMember)members[i]).getElement().getName();
          annotationParameters.append('"').append(memberName).append('"');

        }
        annotationParameters.append('}');
      }
      return Collections.singletonList(generateMemberPrototype(factory, codeStyleManager, aClass, eventClass,
                                                               methodName, annotationParameters.toString(), true));
    }
  }

  public static GenerationInfo generateMemberPrototype(PsiElementFactory factory, CodeStyleManager codeStyleManager,
                                                 PsiClass aClass, PsiClass eventClass, String methodName,
                                                 String annotationParam, boolean renameOnFinish) {
    PsiMethod method = factory.createMethod(methodName, PsiTypes.voidType());
    final PsiModifierList modifierList = method.getModifierList();
    modifierList.setModifierProperty(PsiModifier.PRIVATE, false);

    final PsiAnnotation annotation = factory.createAnnotationFromText("@" + UiBinderUtil.UI_HANDLER_ANNOTATION + "(" + annotationParam + ")", aClass);
    final PsiElement first = modifierList.getFirstChild();
    if (first != null) {
      modifierList.addBefore(annotation, first);
    }
    else {
      modifierList.add(annotation);
    }

    method.getParameterList().add(factory.createParameter("event", factory.createType(eventClass)));
    PsiMethod formattedMethod = (PsiMethod)codeStyleManager.reformat(method);
    return renameOnFinish ? new TemplateGenerationInfo(formattedMethod, new ConstantNode(methodName)) {
      @Override
      protected PsiElement getTemplateElement(PsiMethod method) {
        return method.getNameIdentifier();
      }
    } : new PsiGenerationInfo<>(formattedMethod);
  }

  @Override
  protected GenerationInfo[] generateMemberPrototypes(PsiClass aClass, ClassMember originalMember) throws IncorrectOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected JComponent @Nullable [] getOptionControls() {
    if (mySeparateMethodsCheckBox == null) {
      mySeparateMethodsCheckBox = new JBCheckBox(GwtBundle.message("checkbox.text.separate.methods"), false);
    }
    return new JComponent[] {mySeparateMethodsCheckBox};
  }

  private static class EventTypeFilter implements ClassFilter {
    private final PsiClass myGwtEventClass;
    private final PsiTypeParameter myGwtEventTypeParameter;
    private final List<PsiType> myFieldTypes;
    private final PsiClassType myHandlerRegistrationType;

    EventTypeFilter(PsiClass gwtEventClass, PsiTypeParameter typeParameter, List<PsiType> fieldTypes) {
      myGwtEventClass = gwtEventClass;
      myGwtEventTypeParameter = typeParameter;
      myFieldTypes = fieldTypes;
      myHandlerRegistrationType = getElementFactory(myGwtEventClass.getProject()).createTypeByFQClassName(UiBinderUtil.HANDLER_REGISTRATION_INTERFACE, myGwtEventClass.getResolveScope());
    }

    @Override
    public boolean isAccepted(PsiClass aClass) {
      if (!aClass.isInheritor(myGwtEventClass, true)) {
        return false;
      }

      final PsiSubstitutor substitutor = TypeConversionUtil.getSuperClassSubstitutor(myGwtEventClass, aClass, PsiSubstitutor.EMPTY);
      final PsiType handlerType = substitutor.substitute(myGwtEventTypeParameter);
      if (!(handlerType instanceof PsiClassType)) {
        return false;
      }
      final PsiClass handlerClass = ((PsiClassType)handlerType).resolve();
      if (handlerClass == null || handlerClass instanceof PsiTypeParameter || handlerClass.getMethods().length != 1) {
        return false;
      }

      for (PsiType fieldType : myFieldTypes) {
        if (fieldType instanceof PsiClassType) {
          final List<String> methods = GwtUiHandlerErrorsInspection.findAddHandlerMethods((PsiClassType)fieldType, handlerType, myHandlerRegistrationType);
          if (methods.size() != 1) {
            return false;
          }
        }
      }
      return true;
    }
  }

  protected static final class UiFieldClassMember extends PsiFieldMember {
    private PsiClass myEventClass;

    private UiFieldClassMember(PsiField field) {
      super(field);
    }

    public PsiClass getEventClass() {
      return myEventClass;
    }

    public void setEventClass(PsiClass eventClass) {
      myEventClass = eventClass;
    }
  }
}
