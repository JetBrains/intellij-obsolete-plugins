package com.intellij.gwt.inspections.eventListeners;

import com.intellij.codeInsight.generation.GenerateMembersUtil;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.inspections.BaseGwtLocalQuickFix;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ListenerUsageInfo {
  private static final Logger LOG = Logger.getInstance(ListenerUsageInfo.class);
  private final PsiTypeElement myDeclarationTypeElement;
  private final DeprecatedListener myListener;
  private boolean myCannotBeFixed;
  private final Set<PsiJavaCodeReferenceElement> myReferences = new HashSet<>();
  private final Set<PsiJavaCodeReferenceElement> myProcessedReferences = new HashSet<>();
  private final List<PsiMethodCallExpression> myAddListenerExpressions = new ArrayList<>();
  private PsiNewExpression myAnonymousCreationExpression;
  private PsiJavaCodeReferenceElement myListenerInterfaceInExtendsOrImplementsList;
  private PsiClass myListenerImplementation;
  private EventHandlerInfo myHandler;
  private PsiMethod myEventMethod;
  private boolean myInExtendsList;

  ListenerUsageInfo(@NotNull DeprecatedListener listener, @Nullable PsiTypeElement declarationTypeElement) {
    myListener = listener;
    myDeclarationTypeElement = declarationTypeElement;
  }

  public @NotNull DeprecatedListener getListener() {
    return myListener;
  }


  public void setCannotBeFixed() {
    myCannotBeFixed = true;
  }

  public void setListenerAddedOrRemoved(boolean added, PsiMethodCallExpression methodCallExpression) {
    if (added) {
      myAddListenerExpressions.add(methodCallExpression);
    }
    else {
      setCannotBeFixed();
    }
  }

  public void registerProcessedReference(PsiExpression listenerReference) {
    if (listenerReference instanceof PsiJavaCodeReferenceElement) {
      myProcessedReferences.add((PsiJavaCodeReferenceElement)listenerReference);
    }
  }

  public void registerReference(@NotNull PsiJavaCodeReferenceElement reference) {
    myReferences.add(reference);
  }

  public void setCreatedAsAnonymous(@NotNull PsiNewExpression newExpression, @NotNull PsiAnonymousClass anonymousClass) {
    setListenerImplementation(anonymousClass);
    myAnonymousCreationExpression = newExpression;
  }

  public void setCreatedAsInner(PsiJavaCodeReferenceElement listenerInterfaceReference, PsiClass listenerImplementation, boolean isInExtendsList) {
    setListenerImplementation(listenerImplementation);
    myInExtendsList = isInExtendsList;
    myListenerInterfaceInExtendsOrImplementsList = listenerInterfaceReference;
  }

  private void setListenerImplementation(@NotNull PsiClass listenerImplementation) {
    if (myListenerImplementation != null) {
      setCannotBeFixed();
    }
    myListenerImplementation = listenerImplementation;
    for (String methodName : myListener.getMethodNames()) {
      final PsiMethod[] methods = myListenerImplementation.findMethodsByName(methodName, false);
      if (methods.length > 1) {
        setCannotBeFixed();
        break;
      }

      if (methods.length == 1) {
        if (myEventMethod != null) {
          setCannotBeFixed();
          break;
        }
        myEventMethod = methods[0];
        myHandler = myListener.getHandler(methodName);
      }
    }
  }

  public boolean canBeFixed(DeprecatedListenerUsages usages) {
    if (myCannotBeFixed) {
      return false;
    }
    myReferences.removeAll(myProcessedReferences);
    if (!myReferences.isEmpty()) {
      return false;
    }

    if (myListenerImplementation == null || myEventMethod == null
        || usages.getMethodsWhichParametersAreUsed().contains(myEventMethod) || myHandler == null) {
      return false;
    }

    return true;
  }

  public ProblemDescriptor createProblemDescriptor(InspectionManager manager, boolean isOnTheFly) {
    String message =
      GwtBundle.message("inspection.message.0.is.deprecated.in.gwt.1.6", StringUtil.getShortName(myListener.getListenerClass()));
    PsiElement place;
    if (myDeclarationTypeElement != null && myListener.isListenerOrAdapterType(myDeclarationTypeElement.getType())) {
      place = myDeclarationTypeElement;
    }
    else if (myAnonymousCreationExpression != null) {
      place = myAnonymousCreationExpression.getClassOrAnonymousClassReference();
    }
    else if (myListenerInterfaceInExtendsOrImplementsList != null) {
      place = myListenerInterfaceInExtendsOrImplementsList;
    }
    else {
      place = myListenerImplementation.getNameIdentifier();
    }
    if (place == null) place = myListenerImplementation;
    LocalQuickFix fix = new ReplaceListenerWithHandlerFix();
    return manager.createProblemDescriptor(place, message, fix, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly);
  }

  private final class ReplaceListenerWithHandlerFix extends BaseGwtLocalQuickFix {
    private ReplaceListenerWithHandlerFix() {
      super(GwtBundle.message("quickfix.name.replace.0.with.1",
                              StringUtil.getShortName(myListener.getListenerClass()), StringUtil.getShortName(myHandler.getHandlerClass())));
    }

    @Override
    public @Nls @NotNull String getFamilyName() {
      return GwtBundle.message("quickfix.family.name.replace.listener.with.handler");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
      final PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
      JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);

      final GlobalSearchScope scope = myListenerImplementation.getResolveScope();
      if (myDeclarationTypeElement != null) {
        final PsiClassType type = factory.createTypeByFQClassName(myHandler.getHandlerClass(), scope);
        codeStyleManager.shortenClassReferences(myDeclarationTypeElement.replace(factory.createTypeElement(type)));
      }

      final PsiJavaCodeReferenceElement handlerClassReference = factory.createFQClassNameReferenceElement(myHandler.getHandlerClass(), scope);
      if (myAnonymousCreationExpression != null) {
        final PsiJavaCodeReferenceElement anonymousClassReference = myAnonymousCreationExpression.getClassOrAnonymousClassReference();
        LOG.assertTrue(anonymousClassReference != null);
        codeStyleManager.shortenClassReferences(anonymousClassReference.replace(handlerClassReference));
      }
      else if (myListenerInterfaceInExtendsOrImplementsList != null) {
        final PsiElement referenceElement;
        if (myInExtendsList) {
          myListenerInterfaceInExtendsOrImplementsList.delete();
          referenceElement = myListenerImplementation.getImplementsList().add(handlerClassReference);
        }
        else {
          referenceElement = myListenerInterfaceInExtendsOrImplementsList.replace(handlerClassReference);
        }
        codeStyleManager.shortenClassReferences(referenceElement);
      }

      LOG.assertTrue(myEventMethod != null);
      final PsiClass handlerClass = psiFacade.findClass(myHandler.getHandlerClass(), scope);
      LOG.assertTrue(handlerClass != null);
      final String handlerMethodName = myHandler.getHandlerMethod();
      PsiMethod handlerMethod = handlerClass.findMethodsByName(handlerMethodName, false)[0];
      final PsiMethod newMethod = GenerateMembersUtil.substituteGenericMethod(handlerMethod, PsiSubstitutor.EMPTY);
      PsiElement newParametersList = myEventMethod.getParameterList().replace(newMethod.getParameterList());
      codeStyleManager.shortenClassReferences(newParametersList);
      if (!handlerMethodName.equals(myEventMethod.getName())) {
        final PsiIdentifier nameIdentifier = myEventMethod.getNameIdentifier();
        LOG.assertTrue(nameIdentifier != null);
        nameIdentifier.replace(factory.createIdentifier(handlerMethodName));
      }
      removeOverrideAnnotationIfNeeded(myEventMethod);

      for (PsiMethodCallExpression addListenerExpression : myAddListenerExpressions) {
        final PsiElement methodNameElement = addListenerExpression.getMethodExpression().getReferenceNameElement();
        if (methodNameElement != null) {
          methodNameElement.replace(factory.createIdentifier(myHandler.getAddHandlerMethod()));
        }
      }
    }

    private void removeOverrideAnnotationIfNeeded(PsiMethod eventMethod) {
      final PsiAnnotation annotation = eventMethod.getModifierList().findAnnotation("java.lang.Override");
      if (annotation != null && PsiUtil.getLanguageLevel(eventMethod) == LanguageLevel.JDK_1_5) {
        annotation.delete();
      }
    }
  }
}
