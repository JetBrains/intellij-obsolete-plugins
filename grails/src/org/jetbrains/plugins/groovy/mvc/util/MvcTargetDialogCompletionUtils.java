// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.groovy.mvc.util;

import com.intellij.codeInsight.TailTypes;
import com.intellij.codeInsight.lookup.EqTailType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.TailTypeDecorator;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.SystemProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyNamesUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MvcTargetDialogCompletionUtils {

  private MvcTargetDialogCompletionUtils() {
  }

  public static List<LookupElement> completeClassesAndPackages(@NotNull String prefix, @NotNull GlobalSearchScope scope) {
    if (scope.getProject() == null) return Collections.emptyList();
    JavaPsiFacade facade = JavaPsiFacade.getInstance(scope.getProject());
    final List<LookupElement> res = new ArrayList<>();

    // Complete class names if prefix is a package name with dot at end.
    if (prefix.endsWith(".") && prefix.length() > 1) {
      PsiPackage p = facade.findPackage(prefix.substring(0, prefix.length() - 1));
      if (p != null) {
        for (PsiClass aClass : p.getClasses(scope)) {
          String qualifiedName = aClass.getQualifiedName();
          if (qualifiedName != null) {
            res.add(LookupElementBuilder.create(aClass, qualifiedName));
          }
        }
      }
    }

    PsiPackage defaultPackage = facade.findPackage("");
    if (defaultPackage != null) {
      collectClassesAndPackageNames(res, defaultPackage, scope);
    }

    return res;
  }

  private static void collectClassesAndPackageNames(Collection<? super LookupElement> res, @NotNull PsiPackage aPackage, GlobalSearchScope scope) {
    PsiPackage[] subPackages = aPackage.getSubPackages(scope);

    String qualifiedName = aPackage.getQualifiedName();
    if (!qualifiedName.isEmpty()) {
      if (subPackages.length == 0 || aPackage.getClasses(scope).length > 0) {
        res.add(TailTypeDecorator.withTail(LookupElementBuilder.create(qualifiedName), TailTypes.dotType()));
      }
    }

    for (PsiPackage subPackage : subPackages) {
      collectClassesAndPackageNames(res, subPackage, scope);
    }
  }

  public static Set<String> getAllTargetNamesInternal(@NotNull Module module) {
    final Set<String> result = new HashSet<>();

    GrailsFramework.addAvailableSystemScripts(result, module);

    GrailsFramework framework = GrailsFramework.getInstance(module);
    if (framework != null) {
      final VirtualFile root = framework.findAppRoot(module);
      if (root != null) {
        GrailsFramework.addAvailableScripts(result, root);
      }

      for (VirtualFile pluginRoot : framework.getAllPluginRoots(module, false)) {
        GrailsFramework.addAvailableScripts(result, pluginRoot);
      }
    }

    collectScriptsFromUserHome(result);

    return result;
  }

  private static void collectScriptsFromUserHome(Set<? super String> result) {
    String userHome = SystemProperties.getUserHome();

    File scriptFolder = new File(userHome, ".grails/scripts");

    File[] files = scriptFolder.listFiles();

    if (files == null) return;

    for (File file : files) {
      if (file.getName().startsWith("IdeaPrintProjectSettings")) continue;

      if (isScriptFile(file)) {
        String name = file.getName();
        int idx = name.lastIndexOf('.');
        if (idx != -1) {
          name = name.substring(0, idx);
        }

        result.add(GroovyNamesUtil.camelToSnake(name));
      }
    }
  }

  public static boolean isScriptFile(File file) {
    return file.isFile() && GrailsFramework.isScriptFileName(file.getName());
  }

  public static Set<String> getAllTargetNames(final @NotNull Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(
      module,
      () -> CachedValueProvider.Result.create(getAllTargetNamesInternal(module), PsiModificationTracker.MODIFICATION_COUNT)
    );
  }

  public static class MyTailTypeEQ extends EqTailType {
    public static final MyTailTypeEQ INSTANCE = new MyTailTypeEQ();

    @Override
    protected boolean isSpaceAroundAssignmentOperators(Editor editor, int tailOffset) {
      return false;
    }

    @Override
    public String toString() {
      return "MvcTargetDialogCompletionUtils.TailTypeEQ";
    }
  }
}
