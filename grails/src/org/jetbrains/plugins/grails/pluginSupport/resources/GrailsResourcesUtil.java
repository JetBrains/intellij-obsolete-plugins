// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.resources;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.grails.util.ModuleCachedValue;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.GrTopStatement;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrRenamableLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.HashMap;
import java.util.Map;

public final class GrailsResourcesUtil {

  public static final String MODULES_LIST_VARIABLE_IN_CONFIG = "grails.resources.modules";

  public static final String MODULE_BUILDER_CLASS = "org.grails.plugin.resource.module.ModuleBuilder";

  public static final String MODULE_METHOD_KIND = "grails:GrailsResourcesUtil:module";

  private static final ModuleCachedValue<Map<String, PsiMethod>> CACHED_VALUE =
    new ModuleCachedValue<>(PsiModificationTracker.MODIFICATION_COUNT) {
      @Override
      protected Map<String, PsiMethod> calculate(Module element) {
        return calculateResources(element);
      }
    };
  public static final String MODULES_LIST_VARIABLE = "modules";

  private GrailsResourcesUtil() {
  }

  private static void collectResourcesModules(Map<String, PsiMethod> result, GrClosableBlock closure) {
    for (PsiElement e = closure.getFirstChild(); e != null; e = e.getNextSibling()) {
      if (!(e instanceof GrMethodCall methodCall)) continue;

      GrClosableBlock closureArgument = GrailsUtils.getClosureArgument(methodCall);
      if (closureArgument == null) continue;

      GrExpression invokedExpression = methodCall.getInvokedExpression();
      if (!(invokedExpression instanceof GrReferenceExpression ref)) return;

      if (ref.isQualified()) return;

      String name = ref.getReferenceName();
      if (name == null) continue;

      GrLightMethodBuilder method = new GrRenamableLightMethodBuilder(closure.getManager(), name);
      method.addParameter("arg", GroovyCommonClassNames.GROOVY_LANG_CLOSURE);
      method.setNavigationElement(methodCall);
      method.setMethodKind(MODULE_METHOD_KIND);

      result.put(name, method);
    }
  }

  public static boolean isModuleDefinition(GrMethodCall methodCall) {
    PsiElement parent = methodCall.getParent();
    if (!(parent instanceof GrClosableBlock)) return false;

    return isModuleListDefinitionClosure((GrClosableBlock)parent);
  }

  public static boolean isModuleListDefinitionClosure(GrClosableBlock closure) {
    PsiElement parent = closure.getParent();
    if (!(parent instanceof GrAssignmentExpression assExp)) return false;

    if (!(assExp.getParent() instanceof GroovyFile)) return false;

    GrExpression lValue = assExp.getLValue();

    if (!(lValue instanceof GrReferenceExpression) || assExp.isOperatorAssignment()) return false;

    PsiFile aFile;

    String refText = lValue.getText();
    if (refText.equals(MODULES_LIST_VARIABLE)) {
      aFile = assExp.getContainingFile().getOriginalFile();
      VirtualFile virtualFile = aFile.getVirtualFile();
      if (!GrailsArtifact.RESOURCES.isInstance(virtualFile, aFile.getProject())) return false;
    }
    else if (refText.equals(MODULES_LIST_VARIABLE_IN_CONFIG)) {
      aFile = assExp.getContainingFile().getOriginalFile();
      if (!GrailsUtils.isConfigGroovyFile(aFile)) return false;
    }

    return true;
  }

  private static void collectResourcesModules(Map<String, PsiMethod> result,
                                              @NotNull VirtualFile virtualFile,
                                              @NotNull PsiManager manager,
                                              @NotNull String propertyName) {
    PsiFile psiFile = manager.findFile(virtualFile);
    if (!(psiFile instanceof GroovyFile)) return;

    Map<String, PsiMethod> modules = extractResourcesModules((GroovyFile)psiFile, propertyName);

    result.putAll(modules);
  }

  public static Map<String, PsiMethod> extractResourcesModules(@NotNull GroovyFile groovyFile) {
    String propertyName = groovyFile.getName().equals(GrailsUtils.CONFIG_GROOVY) ? MODULES_LIST_VARIABLE_IN_CONFIG : MODULES_LIST_VARIABLE;
    return extractResourcesModules(groovyFile, propertyName);
  }

  private static void collectResourcesModules(Map<String, PsiMethod> result, @NotNull GrAssignmentExpression assExp, @NotNull String propertyName) {
    GrExpression rValue = assExp.getRValue();
    if (rValue instanceof GrClosableBlock
        && assExp.getLValue().getText().equals(propertyName)
        && !assExp.isOperatorAssignment()) {

      collectResourcesModules(result, (GrClosableBlock)rValue);
    }
  }

  private static Map<String, PsiMethod> extractResourcesModules(@NotNull GroovyFile groovyFile, @NotNull String propertyName) {
    return CachedValuesManager.getCachedValue(groovyFile, () -> {
      Map<String, PsiMethod> cachedValue = new HashMap<>();

      for (GrTopStatement topStatement : groovyFile.getTopStatements()) {
        if (topStatement instanceof GrAssignmentExpression) {
          collectResourcesModules(cachedValue, (GrAssignmentExpression)topStatement, propertyName);
        }
        else if (topStatement instanceof GrMethodCall methodCall) {    // check if environments { test { modules = { ... } } }
          String methodName = PsiUtil.getUnqualifiedMethodName(methodCall);
          if (GrailsUtils.ENVIRONMENTS.equals(methodName)) {
            GrClosableBlock envClosure = GrailsUtils.getClosureArgument(methodCall);
            if (envClosure != null) {
              for (PsiElement envClosureChild = envClosure.getFirstChild(); envClosureChild != null; envClosureChild = envClosureChild.getNextSibling()) {
                if (envClosureChild instanceof GrMethodCall mc) {
                  String name = PsiUtil.getUnqualifiedMethodName(mc);
                  if (name != null && GrailsUtils.ENVIRONMENT_LIST.contains(name)) {
                    GrClosableBlock c = GrailsUtils.getClosureArgument(mc);
                    if (c != null) {
                      for (PsiElement e = c.getFirstChild(); e != null; e = e.getNextSibling()) {
                        if (e instanceof GrAssignmentExpression) {
                          collectResourcesModules(cachedValue, (GrAssignmentExpression)e, propertyName);
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      return Result.create(cachedValue, groovyFile);
    });
  }

  private static @NotNull Map<String, PsiMethod> calculateResources(@NotNull Module module) {
    Map<String, PsiMethod> res = new HashMap<>();

    PsiManager manager = PsiManager.getInstance(module.getProject());

    if (GrailsFramework.isCommonPluginsModule(module)) {
      for (VirtualFile virtualFile : ModuleRootManager.getInstance(module).getContentRoots()) {
        VirtualFile configGroovy = VfsUtil.findRelativeFile(virtualFile, GrailsUtils.GRAILS_APP_DIRECTORY, GrailsUtils.CONF_DIRECTORY,
                                                            GrailsUtils.CONFIG_GROOVY);
        if (configGroovy != null) {
          collectResourcesModules(res, configGroovy, manager, MODULES_LIST_VARIABLE_IN_CONFIG);
        }
      }
    }
    else {
      VirtualFile confDirectory = GrailsUtils.findConfDirectory(module);
      if (confDirectory != null) {
        VirtualFile configGroovy = confDirectory.findChild(GrailsUtils.CONFIG_GROOVY);
        if (configGroovy != null) {
          collectResourcesModules(res, configGroovy, manager, MODULES_LIST_VARIABLE_IN_CONFIG);
        }
      }
    }

    for (VirtualFile virtualFile : GrailsArtifact.RESOURCES.getVirtualFileMap(module).values()) {
      collectResourcesModules(res, virtualFile, manager, MODULES_LIST_VARIABLE);
    }

    return res;
  }

  public static Map<String, PsiMethod> getResources(final @NotNull Module module) {
    return CACHED_VALUE.get(module);
  }
}
