package com.intellij.gwt.uiBinder.mapping;

import com.intellij.gwt.uiBinder.UiRendererUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.BidirectionalMultiMap;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.intellij.gwt.uiBinder.UiBinderUtil.UI_BINDER_INTERFACE;
import static com.intellij.gwt.uiBinder.UiBinderUtil.UI_RENDERER_INTERFACE;

public final class UiBinderMappingServiceImpl extends UiBinderMappingService {

  private final Module myModule;
  private final CachedValue<CachedEntry> myCachedEntry;

  public UiBinderMappingServiceImpl(Module module) {
    myModule = module;
    myCachedEntry =
      CachedValuesManager.getManager(myModule.getProject()).createCachedValue(
        () -> CachedValueProvider.Result.create(computeMapping(), PsiModificationTracker.MODIFICATION_COUNT), false);
  }

  private CachedEntry computeMapping() {
    final CachedEntry result = new CachedEntry();
    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(myModule.getProject());
    GlobalSearchScope searchScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(myModule);
    GlobalSearchScope moduleOnlySearchScope = GlobalSearchScope.moduleScope(myModule);
    final PsiClass uiBinderInterface = psiFacade.findClass(UI_BINDER_INTERFACE, searchScope);
    final PsiClass uiRendererInterface = psiFacade.findClass(UI_RENDERER_INTERFACE, searchScope);

    if (uiBinderInterface != null && uiBinderInterface.getTypeParameters().length == 2) {
      final PsiTypeParameter ownerParameter = uiBinderInterface.getTypeParameters()[1];
      ClassInheritorsSearch.search(uiBinderInterface, moduleOnlySearchScope, true).forEach(aClass -> {
        String className = aClass.getQualifiedName();
        if (className == null) return true;

        PsiType ownerType = TypeConversionUtil.getSuperClassSubstitutor(uiBinderInterface, aClass, PsiSubstitutor.EMPTY).substitute(ownerParameter);
        if (ownerType == null) return true;

        PsiClass ownerClass = PsiTypesUtil.getPsiClass(ownerType);
        if (ownerClass == null) return true;

        String ownerClassName = ownerClass.getQualifiedName();
        if (ownerClassName == null) return true;

        String uiTemplateXmlFileUrl = deduceTemplateFileUrl(aClass);
        if (uiTemplateXmlFileUrl == null) return true;

        result.myClassNameToUiXmlUrl.put(ownerClassName, uiTemplateXmlFileUrl);
        result.myUiBinderToUiXmlUrl.put(className, uiTemplateXmlFileUrl);
        return true;
      });
    }

    if (uiRendererInterface != null && uiRendererInterface.getTypeParameters().length == 0) {
      PsiClass abstractCellClass = psiFacade.findClass(UiRendererUtil.ABSTRACT_CELL_CLASS, searchScope);
      if (abstractCellClass != null && abstractCellClass.getTypeParameters().length == 1) {
        PsiMethod[] abstractCellRenderMethods = abstractCellClass.findMethodsByName(UiRendererUtil.RENDER_METHOD_NAME, false);
        if (abstractCellRenderMethods.length == 1) {
          ClassInheritorsSearch.search(abstractCellClass, moduleOnlySearchScope, true).forEach(new Processor<>() {
            @Override
            public boolean process(PsiClass abstractCellInheritor) {
              final String className = abstractCellInheritor.getQualifiedName();
              if (className == null) return true;

              PsiMethod renderMethod = UiRendererUtil.findRenderMethodImplementation(abstractCellInheritor);
              if (renderMethod == null) return true;

              PsiCodeBlock renderMethodBody = renderMethod.getBody();
              if (renderMethodBody == null) return true;

              renderMethodBody.accept(new JavaRecursiveElementVisitor() {
                @Override
                public void visitMethodCallExpression(@NotNull PsiMethodCallExpression methodCallExpression) {
                  super.visitMethodCallExpression(methodCallExpression);

                  PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();
                  String methodName = methodExpression.getReferenceName();
                  if (!UiRendererUtil.RENDER_METHOD_NAME.equals(methodName)) return;

                  PsiMethod renderMethod = methodCallExpression.resolveMethod();
                  if (renderMethod == null) return;

                  PsiClass uiRendererInheritor = renderMethod.getContainingClass();
                  if (uiRendererInheritor == null) return;
                  if (!uiRendererInheritor.isInterface()) return;
                  if (!uiRendererInheritor.isInheritor(uiRendererInterface, false)) return;

                  String uiTemplateXmlFileUrl = deduceTemplateFileUrl(uiRendererInheritor);
                  if (uiTemplateXmlFileUrl == null) return;

                  result.myClassNameToUiXmlUrl.put(className, uiTemplateXmlFileUrl);
                  result.myUiRendererComponents.add(className);
                }
              });
              return true;
            }
          });
        }
      }
    }
    return result;
  }

  @Override
  public @NotNull List<PsiClass> getBoundClasses(@NotNull PsiFile uiXmlFile) {
    VirtualFile virtualFile = uiXmlFile.getVirtualFile();
    if (virtualFile != null) {
      Set<String> classNames = myCachedEntry.getValue().myClassNameToUiXmlUrl.getKeys(virtualFile.getUrl());
      GlobalSearchScope scope = GlobalSearchScope.moduleScope(myModule);
      JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(myModule.getProject());
      List<PsiClass> result = new SmartList<>();
      for (String className : classNames) {
        ContainerUtil.addIfNotNull(result, psiFacade.findClass(className, scope));
      }
      return result;
    }
    return Collections.emptyList();
  }

  @Override
  public @NotNull List<XmlFile> getUiXmlFiles(@NotNull PsiClass aClass) {
    String className = aClass.getQualifiedName();
    if (className != null) {
      Set<String> urls = myCachedEntry.getValue().myClassNameToUiXmlUrl.getValues(className);
      List<XmlFile> result = new ArrayList<>();
      for (String url : urls) {
        ContainerUtil.addIfNotNull(result, resolveUrlToUiXmlFile(url));
      }
      return result;
    }
    return Collections.emptyList();
  }

  @Override
  public @Nullable XmlFile getUiXmlFile(@NotNull PsiClass uiBinderInheritor) {
    String className = uiBinderInheritor.getQualifiedName();
    if (className != null) {
      Set<String> urls = myCachedEntry.getValue().myUiBinderToUiXmlUrl.getValues(className);
      if (!urls.isEmpty()) {
        String url = ContainerUtil.getFirstItem(urls);
        return resolveUrlToUiXmlFile(url);
      }
    }
    return null;
  }

  @Override
  public boolean isUiRendererComponent(@NotNull PsiClass psiClass) {
    String className = psiClass.getQualifiedName();
    if (className != null) {
      return myCachedEntry.getValue().myUiRendererComponents.contains(className);
    }
    return false;
  }

  private @Nullable XmlFile resolveUrlToUiXmlFile(String url) {
    VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
    if (file != null) {
      PsiFile psiFile = PsiManager.getInstance(myModule.getProject()).findFile(file);
      if (psiFile instanceof XmlFile) {
        return (XmlFile) psiFile;
      }
    }
    return null;
  }

  private static class CachedEntry {
    public final BidirectionalMultiMap<String, String> myClassNameToUiXmlUrl = new BidirectionalMultiMap<>();
    public final BidirectionalMultiMap<String, String> myUiBinderToUiXmlUrl = new BidirectionalMultiMap<>();
    public final Set<String> myUiRendererComponents = new HashSet<>();
  }
}
