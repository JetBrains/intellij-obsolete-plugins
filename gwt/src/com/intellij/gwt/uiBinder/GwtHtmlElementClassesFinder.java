package com.intellij.gwt.uiBinder;

import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GwtHtmlElementClassesFinder {
  private static final @NonNls String TAG_NAME_ANNOTATION = "com.google.gwt.dom.client.TagName";
  private static final Key<CachedValue<Map<String, String>>> TAG_TO_CLASS_MAP_KEY = Key.create("GWT_TAG_TO_CLASS_NAME_MAP");

  private GwtHtmlElementClassesFinder() {
  }

  public static @Nullable String findElementClass(@NotNull String tagName, final @NotNull Module module) {
    final CachedValuesManager cachedValuesManager = CachedValuesManager.getManager(module.getProject());
    final Map<String, String> map = cachedValuesManager.getCachedValue(module, TAG_TO_CLASS_MAP_KEY, new TagToClassMapCachedValueProvider(module), false);
    return map != null ? map.get(tagName) : null;
  }

  public static @NotNull List<String> getTagNames(@NotNull PsiClass elementClass) {
    final PsiModifierList modifiers = elementClass.getModifierList();
    if (modifiers != null) {
      final PsiAnnotation annotation = modifiers.findAnnotation(TAG_NAME_ANNOTATION);
      if (annotation != null) {
        final PsiAnnotationMemberValue memberValue = annotation.findAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME);
        if (memberValue instanceof PsiArrayInitializerMemberValue) {
          final PsiAnnotationMemberValue[] initializers = ((PsiArrayInitializerMemberValue)memberValue).getInitializers();
          List<String> result = new SmartList<>();
          for (PsiAnnotationMemberValue initializer : initializers) {
            final String tag = JamCommonUtil.getObjectValue(initializer, String.class);
            if (tag != null) {
              result.add(tag);
            }
          }
          return result;
        }
      }
    }
    return Collections.emptyList();
  }

  private static class TagToClassMapCachedValueProvider implements CachedValueProvider<Map<String, String>> {
    private final Module myModule;

    TagToClassMapCachedValueProvider(Module module) {
      myModule = module;
    }

    @Override
    public Result<Map<String, String>> compute() {
      final Map<String, String> result = new HashMap<>();
      GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(myModule);
      final PsiClass annotationClass = JavaPsiFacade.getInstance(myModule.getProject()).findClass(TAG_NAME_ANNOTATION, scope);
      if (annotationClass != null) {
        AnnotatedElementsSearch.searchPsiClasses(annotationClass, scope).forEach(psiClass -> {
          for (String tagName : getTagNames(psiClass)) {
            result.put(tagName, psiClass.getQualifiedName());
          }
          return true;
        });
      }
      return Result.create(result, ProjectRootManager.getInstance(myModule.getProject()));
    }
  }
}
