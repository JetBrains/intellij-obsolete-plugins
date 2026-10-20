package com.intellij.gwt.uiBinder.mapping;

import static com.intellij.gwt.uiBinder.UiBinderUtil.UI_BINDER_INTERFACE;
import static com.intellij.gwt.uiBinder.UiBinderUtil.UI_RENDERER_INTERFACE;
import static com.intellij.gwt.uiBinder.UiBinderUtil.UI_TEMPLATE_ANNOTATION;
import static com.intellij.gwt.uiBinder.UiBinderUtil.UI_XML_SUFFIX;

import com.intellij.gwt.uiBinder.UiRendererUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.psi.xml.XmlFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class UiBinderMappingServiceImpl extends UiBinderMappingService {
  private final Module myModule;
  private final CachedValue<InfrastructureClasses> infrastructureClassesCache;

  public UiBinderMappingServiceImpl(Module module) {
    myModule = module;
    infrastructureClassesCache = CachedValuesManager.getManager(myModule.getProject()).createCachedValue(
      () -> CachedValueProvider.Result.create(findInfrastructureClasses(), ProjectRootManager.getInstance(myModule.getProject())), false);
  }

  @Override
  public @NotNull List<PsiClass> getBoundClasses(@NotNull PsiFile uiXmlFile) {
    if (uiXmlFile.getVirtualFile() == null) return Collections.emptyList();

    GlobalSearchScope moduleScope = GlobalSearchScope.moduleScope(myModule);
    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(myModule.getProject());

    Set<PsiClass> result = new LinkedHashSet<>();
    findUiBinderBoundClassByName(uiXmlFile, psiFacade, moduleScope).ifPresent(result::add);
    result.addAll(findUiRendererBoundClassesByName(uiXmlFile, psiFacade, moduleScope));
    result.addAll(findBoundClassesByAnnotation(uiXmlFile, moduleScope));
    return new ArrayList<>(result);
  }

  @Override
  public @NotNull List<XmlFile> getUiXmlFiles(@NotNull PsiClass owner) {
    if (owner.getQualifiedName() == null || owner.getContainingFile() == null) {
      return Collections.emptyList();
    }
    final List<XmlFile> result = new ArrayList<>();
    Arrays.stream(owner.getInnerClasses())
      .filter(clazz -> InheritanceUtil.isInheritor(clazz, true, UI_BINDER_INTERFACE))
      .forEach(uiBinderInheritor -> getUiXmlFile(uiBinderInheritor, owner).ifPresent(result::add));
    return result;
  }

  @Override
  public @Nullable XmlFile getUiXmlFile(@NotNull PsiClass uiBinderInheritor) {
    return getUiXmlFile(uiBinderInheritor, null).orElse(null);
  }

  private Optional<XmlFile> getUiXmlFile(PsiClass uiBinderInheritor, PsiClass owner) {
    String className = uiBinderInheritor.getQualifiedName();
    if (className == null) return Optional.empty();

    if (owner != null &&
      resolveUiBinderOwner(uiBinderInheritor)
        .map(PsiClass::getQualifiedName)
        .map(fqn -> !Objects.equals(owner.getQualifiedName(), fqn))
        .orElse(true)) {
      return Optional.empty();
    }


    String uiTemplateXmlFileUrl = deduceTemplateFileUrl(uiBinderInheritor);
    if (uiTemplateXmlFileUrl == null) return Optional.empty();
    return resolveUrlToUiXmlFile(uiTemplateXmlFileUrl);
  }

  @Override
  public boolean isUiRendererComponent(@NotNull PsiClass psiClass) {
    if (!InheritanceUtil.isInheritor(psiClass, UiRendererUtil.ABSTRACT_CELL_CLASS)) {
      return false;
    }

    PsiMethod renderMethod = UiRendererUtil.findRenderMethodImplementation(psiClass);
    if (renderMethod == null) return false;

    PsiCodeBlock renderMethodBody = renderMethod.getBody();
    if (renderMethodBody == null) return false;

    PsiClass uiRendererInterface = infrastructureClassesCache.getValue().uiRendererInterface;
    if (uiRendererInterface == null) return false;

    MutableBoolean found = new MutableBoolean();
    renderMethodBody.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitMethodCallExpression(@NotNull PsiMethodCallExpression methodCallExpression) {
        if (found.isTrue()) return;
        super.visitMethodCallExpression(methodCallExpression);

        PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();
        if (!UiRendererUtil.RENDER_METHOD_NAME.equals(methodExpression.getReferenceName())) return;

        PsiMethod resolvedMethod = methodCallExpression.resolveMethod();
        if (resolvedMethod == null) return;

        PsiClass containingClass = resolvedMethod.getContainingClass();
        if (containingClass == null || !containingClass.isInterface()) return;
        if (containingClass.isInheritor(uiRendererInterface, false)) {
          found.setTrue();
        }
      }
    });
    return found.isTrue();
  }

  private InfrastructureClasses findInfrastructureClasses() {
    InfrastructureClasses result = new InfrastructureClasses();
    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(myModule.getProject());
    GlobalSearchScope searchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(myModule);

    final PsiClass uiBinderInterface = psiFacade.findClass(UI_BINDER_INTERFACE, searchScope);
    if (uiBinderInterface != null && uiBinderInterface.getTypeParameters().length == 2) {
      result.uiBinderInterface = uiBinderInterface;
      result.ownerParameter = uiBinderInterface.getTypeParameters()[1];
    }

    result.uiTemplateAnnotation = psiFacade.findClass(UI_TEMPLATE_ANNOTATION, searchScope);
    result.uiRendererInterface = psiFacade.findClass(UI_RENDERER_INTERFACE, searchScope);

    return result;
  }

  private Optional<PsiClass> findOwner(PsiFile uiXmlFile, JavaPsiFacade psiFacade,
                                       GlobalSearchScope moduleScope) {
    PsiDirectory directory = uiXmlFile.getContainingDirectory();
    if (directory == null) return Optional.empty();

    PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(directory);
    if (psiPackage == null) return Optional.empty();

    String baseName = StringUtil.trimEnd(uiXmlFile.getName(), UI_XML_SUFFIX);
    String packageName = psiPackage.getQualifiedName();
    String candidateFqn = packageName.isEmpty() ? baseName : packageName + "." + baseName;

    return Optional.ofNullable(psiFacade.findClass(candidateFqn, moduleScope));
  }

  private Optional<PsiClass> findUiBinderBoundClassByName(PsiFile uiXmlFile, JavaPsiFacade psiFacade, GlobalSearchScope moduleScope) {
    PsiClass owner = findOwner(uiXmlFile, psiFacade, moduleScope).orElse(null);
    if (owner == null) return Optional.empty();

    String targetUrl = uiXmlFile.getVirtualFile().getUrl();
    if (Arrays.stream(owner.getInnerClasses())
      .filter(inner -> inner.isInterface() && InheritanceUtil.isInheritor(inner, UI_BINDER_INTERFACE))
      .map(uiBinder -> getUiXmlFile(uiBinder, owner))
      .anyMatch(xmlFile ->
        xmlFile.map(f -> targetUrl.equals(f.getVirtualFile().getUrl())).orElse(false))) {
      return Optional.of(owner);
    }
    return Optional.empty();
  }

  private List<PsiClass> findUiRendererBoundClassesByName(PsiFile uiXmlFile, JavaPsiFacade psiFacade, GlobalSearchScope moduleScope) {
    PsiClass owner = findOwner(uiXmlFile, psiFacade, moduleScope).orElse(null);
    if (owner == null) return Collections.emptyList();

    String targetUrl = uiXmlFile.getVirtualFile().getUrl();
    return Arrays.stream(owner.getInnerClasses())
      .filter(inner -> inner.isInterface() && InheritanceUtil.isInheritor(inner, UI_RENDERER_INTERFACE))
      .filter(uiRenderer -> targetUrl.equals(deduceTemplateFileUrl(uiRenderer)))
      .flatMap(uiRenderer -> findUiRendererConsumers(uiRenderer, moduleScope).stream())
      .toList();
  }

  private List<PsiClass> findBoundClassesByAnnotation(PsiFile uiXmlFile, GlobalSearchScope moduleScope) {
    InfrastructureClasses infrastructureClasses = infrastructureClassesCache.getValue();
    if (infrastructureClasses.uiTemplateAnnotation == null) return Collections.emptyList();

    String targetUrl = uiXmlFile.getVirtualFile().getUrl();
    List<PsiClass> result = new ArrayList<>();
    for (PsiClass annotated : AnnotatedElementsSearch.searchPsiClasses(infrastructureClasses.uiTemplateAnnotation, moduleScope)) {
      if (!annotated.isInterface() || !targetUrl.equals(deduceTemplateFileUrl(annotated))) continue;

      if (InheritanceUtil.isInheritor(annotated, UI_BINDER_INTERFACE)) {
        resolveUiBinderOwner(annotated).ifPresent(result::add);
      } else if (InheritanceUtil.isInheritor(annotated, UI_RENDERER_INTERFACE)) {
        result.addAll(findUiRendererConsumers(annotated, moduleScope));
      }
    }
    return result;
  }

  private List<PsiClass> findUiRendererConsumers(PsiClass uiRendererInterface, GlobalSearchScope moduleScope) {
    List<PsiClass> result = new ArrayList<>();
    ReferencesSearch.search(uiRendererInterface, moduleScope).forEach(reference -> {
      PsiClass enclosingCellClass = findEnclosingCellClass(reference.getElement());
      if (enclosingCellClass != null) {
        result.add(enclosingCellClass);
      }
      return true;
    });
    return result;
  }

  private static PsiClass findEnclosingCellClass(PsiElement element) {
    PsiClass candidate = PsiTreeUtil.getParentOfType(element, PsiClass.class);
    while (candidate != null && !InheritanceUtil.isInheritor(candidate, UiRendererUtil.ABSTRACT_CELL_CLASS)) {
      candidate = PsiTreeUtil.getParentOfType(candidate, PsiClass.class);
    }
    return candidate;
  }

  private Optional<PsiClass> resolveUiBinderOwner(PsiClass uiBinderInheritor) {
    InfrastructureClasses infrastructureClasses = infrastructureClassesCache.getValue();
    if (infrastructureClasses.uiBinderInterface == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(TypeConversionUtil.getSuperClassSubstitutor(infrastructureClasses.uiBinderInterface, uiBinderInheritor, PsiSubstitutor.EMPTY)
      .substitute(infrastructureClasses.ownerParameter)).map(PsiTypesUtil::getPsiClass);
  }

  private Optional<XmlFile> resolveUrlToUiXmlFile(String url) {
    return Optional.ofNullable(VirtualFileManager.getInstance().findFileByUrl(url))
      .map(file -> PsiManager.getInstance(myModule.getProject()).findFile(file))
      .filter(XmlFile.class::isInstance)
      .map(XmlFile.class::cast);
  }

  private static class InfrastructureClasses {
    public PsiClass uiBinderInterface;
    public PsiClass uiTemplateAnnotation;
    public PsiClass uiRendererInterface;
    public PsiTypeParameter ownerParameter;
  }
}
