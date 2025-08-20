package com.intellij.play.references;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.play.utils.PlayPathUtils;
import com.intellij.play.utils.PlayUtils;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.IconManager;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PlayControllerActionPsiReferenceProvider extends PsiReferenceProvider {

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull ProcessingContext context) {
    if (!PlayUtils.isPlayInstalled(element.getProject())) return PsiReference.EMPTY_ARRAY;

    final Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null) return PsiReference.EMPTY_ARRAY;

    final String text = ElementManipulators.getValueText(element);
    if (StringUtil.isEmptyOrSpaces(text)) return PsiReference.EMPTY_ARRAY;

    String actionName = text.startsWith("@") ? text.substring(1) : text;
    final Set<PsiReference> references = getActionNameReferences(element, module, actionName, element.getText().indexOf(actionName));
    return references.toArray(PsiReference.EMPTY_ARRAY);
  }

  public static Set<PsiReference> getActionNameReferences(final PsiElement psiElement,
                                                          final Module module,
                                                          String actionName,
                                                          final Integer startOffset) {
    if (!PlayUtils.isPlayInstalled(psiElement.getProject())) return Collections.emptySet();
    Set<PsiReference> references = new LinkedHashSet<>();
    if (actionName.contains(".")) {
      final String controller = actionName.substring(0, actionName.lastIndexOf("."));
      final String action = actionName.substring(actionName.lastIndexOf(".") + 1);

      final PsiClass controllerByName = PlayPathUtils.findControllerByName(controller, module);

      references.add(new ControllerPsiReference(psiElement, createTextRange(startOffset, controller), controllerByName, module));

      if (controllerByName != null) {
        references.add(new ActionReference(psiElement, createTextRange(startOffset + controller.length() + 1, action), controllerByName, action));
      }
    }
    else {
      final PsiClass psiClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class);
      if (psiClass != null && PlayUtils.isController(psiClass)) {
        references
          .add(new LocalActionReference(psiElement, startOffset, actionName, psiClass, module));
      }
    }

    return references;
  }

  private static TextRange createTextRange(Integer startOffset, String name) {
    return new TextRange(startOffset, startOffset + name.length());
  }

  private static class ControllerPsiReference extends PsiReferenceBase<PsiElement> {
    private PsiClass myControllerByName;
    private final Module myModule;

    ControllerPsiReference(@NotNull PsiElement psiElement,
                                  @NotNull TextRange textRange,
                                  @Nullable PsiClass controller,
                                  @NotNull Module module) {
      super(psiElement, textRange);
      myControllerByName = controller;
      myModule = module;
    }

    @Override
    public PsiElement resolve() {
      return myControllerByName;
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
      return super.isReferenceTo(element);
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
      if (element instanceof PsiClass) {
        myControllerByName = (PsiClass)element;

        return myControllerByName;
      }
      return super.bindToElement(element);
    }

    @Override
    public Object @NotNull [] getVariants() {
      final List<PsiClass> controllers = getTopLevelControllers(myModule);
      return controllers.toArray();
    }
  }

  private static class ActionReference extends PsiReferenceBase<PsiElement> {
    private final PsiClass myController;
    private final String myAction;

    ActionReference(@NotNull PsiElement psiElement,
                           @NotNull TextRange textRange,
                           @NotNull PsiClass controller,
                           @NotNull String action) {
      super(psiElement, textRange);
      myController = controller;
      myAction = action;
    }

    @Override
    public PsiElement resolve() {
      for (PsiMethod psiMethod : myController.getAllMethods()) {
        if (psiMethod.getName().equals(myAction)) return psiMethod;
      }
      return null;
    }

    @Override
    public Object @NotNull [] getVariants() {
      return ContainerUtil
        .map2Array(getActionMethods(myController), LookupElementBuilder.class, s -> LookupElementBuilder.create(s).withIcon(
          IconManager.getInstance().getPlatformIcon(com.intellij.ui.PlatformIcons.Method)));
    }
  }

  private static List<PsiClass> getTopLevelControllers(@NotNull Module module) {
    final PsiPackage psiPackage = JavaPsiFacade.getInstance(module.getProject()).findPackage("controllers");
    if (psiPackage != null) {
      return Arrays.asList(psiPackage.getClasses(GlobalSearchScope.moduleWithDependenciesScope(module)));
    }

    return new ArrayList<>();
  }

  private static Set<PsiMethod> getActionMethods(@NotNull PsiClass psiClass) {
    Set<PsiMethod> names = new HashSet<>();
    for (PsiMethod psiMethod : psiClass.getAllMethods()) {
      if (psiMethod.hasModifierProperty(PsiModifier.STATIC) && psiMethod.hasModifierProperty(PsiModifier.PUBLIC)) {
        names.add(psiMethod);
      }
    }
    return names;
  }

  private static class LocalActionReference extends ActionReference {
    private final PsiClass myPsiClass;
    private final Module myModule;

    LocalActionReference(@NotNull PsiElement psiElement, int startOffset, @NotNull String actionName, @NotNull PsiClass psiClass, @NotNull Module module) {
      super(psiElement, PlayControllerActionPsiReferenceProvider.createTextRange(startOffset, actionName), psiClass, actionName);
      myPsiClass = psiClass;
      myModule = module;
    }

    @Override
    public Object @NotNull [] getVariants() {

      List<Object> variants = new ArrayList<>();

      variants.addAll(ContainerUtil.map(getActionMethods(myPsiClass), (Function<PsiMethod, Object>)psiMethod -> LookupElementBuilder.create(psiMethod).withIcon(
        IconManager.getInstance().getPlatformIcon(com.intellij.ui.PlatformIcons.Method))));
      variants.addAll(getTopLevelControllers(myModule));

      return variants.toArray();
    }
  }
}
