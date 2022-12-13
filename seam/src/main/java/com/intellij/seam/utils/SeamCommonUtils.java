/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.intellij.seam.utils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.facet.FacetManager;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.jam.JamService;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.javaee.ejb.EjbHelper;
import com.intellij.javaee.ejb.model.SessionBean;
import com.intellij.javaee.ejb.model.common.enums.SessionType;
import com.intellij.javaee.ejb.role.EjbClassRole;
import com.intellij.jsf.constants.JsfConstants;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.JarVersionDetectionUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.seam.constants.SeamAnnotationConstants;
import com.intellij.seam.constants.SeamConstants;
import com.intellij.seam.facet.SeamFacet;
import com.intellij.seam.model.SeamInstallPrecedence;
import com.intellij.seam.model.jam.*;
import com.intellij.seam.model.jam.bijection.SeamJamOutjection;
import com.intellij.seam.model.jam.dataModel.SeamJamDataModel;
import com.intellij.seam.model.xml.SeamDomModelManager;
import com.intellij.seam.model.xml.components.*;
import com.intellij.seam.model.xml.core.BasicBundleNamesHolder;
import com.intellij.seam.utils.beans.ContextVariable;
import com.intellij.seam.utils.beans.DomFactoryContextVariable;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class SeamCommonUtils {
  private static final String EL_START = "#{";
  private static final String EL_END = "}";

  private SeamCommonUtils() { }

  public static Set<ContextVariable> getSeamContextVariablesWithDependencies(final Module module) {
    return getSeamContextVariablesWithDependencies(module, true, true);
  }

  public static Set<ContextVariable> getSeamContextVariablesWithDependencies(@Nullable final Module module, boolean jam, boolean dom) {
    if (module == null) return Collections.emptySet();
    final Set<ContextVariable> contextVariables = getSeamContextVariables(module, jam, dom, true);
    for (Module mod : JamCommonUtil.getAllModuleDependencies(module)) {
      contextVariables.addAll(getSeamContextVariables(mod, jam, dom, false));
    }

    return contextVariables;
  }

  public static Set<ContextVariable> getSeamContextVariables(@Nullable final Module module, boolean jam, boolean dom, boolean fromLibs) {
    Set<ContextVariable> vars = new HashSet<>();
    if (module != null) {
      final Set<String> globalImports = getGlobalImports(module);

      if (jam) {
        addAnnotatedContextVariables(module, vars, fromLibs);
      }
      if (dom) {
        addXmlDefinedContextVariables(module, vars);
        addXmlDefinedFactoryContextVariables(module, vars);
        addMergedContextVariables(module, vars, fromLibs);
      }

      Set<ContextVariable> notImported = new HashSet<>(vars);
      for (ContextVariable contextVariable : notImported) {
        addNotNullContextVariable(createImportedVariables(contextVariable, notImported, globalImports), vars);
      }
    }
    return vars;
  }

  private static void addMergedContextVariables(Module module, Set<? super ContextVariable> vars, boolean fromLibs) {
    final List<SeamJamComponent> mergedComponents = SeamJamModel.getModel(module).getAnnotatedSeamComponents(fromLibs);

    for (SeamJamComponent mergedComponent : mergedComponents) {
      if (!mergedComponent.isValid()) continue;

      addImplicitContextVariables(module, vars, mergedComponent);
    }
  }

  public static List<String> getSeamContextVariableNames(final Module module) {
    return ContainerUtil.map2List(getSeamContextVariablesWithDependencies(module), contextVariable -> contextVariable.getName());
  }

  public static void addAnnotatedContextVariables(final Module module, final Set<? super ContextVariable> vars, final boolean fromLibs) {
    final Set<SeamJamComponent> seamComponents = SeamJamModel.getModel(module).getSeamComponents(false, fromLibs);

    final Set<ContextVariable> annotatedVariables = new HashSet<>();

    for (SeamJamComponent seamComponent : seamComponents) {
      if (!seamComponent.isValid()) continue;
      //add components
      addNotNullContextVariable(createContextVariable(seamComponent, seamComponent.getComponentName(), seamComponent.getComponentType()),
                                annotatedVariables);

      addImplicitContextVariables(module, annotatedVariables, seamComponent);
    }

    for (SeamJamComponent seamComponent : seamComponents) {
      // add outjections (IDEADEV-26945)
      for (SeamJamOutjection outjection : seamComponent.getOutjections()) {
        final String name = outjection.getName();
        final PsiType type = outjection.getType();

        if (!StringUtil.isEmptyOrSpaces(name) && type != null && !isAlreadyDefined(name, type, annotatedVariables)) {
          addNotNullContextVariable(createContextVariable(outjection, name, type), annotatedVariables);
        }
      }
    }

    vars.addAll(annotatedVariables);
  }

  private static void addImplicitContextVariables(Module module, Set<? super ContextVariable> annotatedVariables, SeamJamComponent jamComponent) {
    // add roles
    for (SeamJamRole jamRole : jamComponent.getRoles()) {
      addNotNullContextVariable(createContextVariable(jamRole, jamRole.getName(), jamRole.getComponentType()), annotatedVariables);
    }

    // add factories
    for (SeamJamFactory jamFactory : jamComponent.getFactories()) {
      addNotNullContextVariable(createContextVariable(jamFactory, jamFactory.getFactoryName(), jamFactory.getFactoryType()),
                                annotatedVariables);
    }

    // add data models
    for (SeamJamDataModel dataModel : jamComponent.getDataModels()) {
      addDataModels(module, annotatedVariables, dataModel);
    }
  }

  private static void addDataModels(Module module, Set<? super ContextVariable> annotatedVariables, SeamJamDataModel dataModel) {
    PsiType type = dataModel.getType();

    if (type != null) {
      addNotNullContextVariable(createContextVariable(dataModel, dataModel.getName(), type), annotatedVariables);
    }
    else {
      PsiClass dataModelClazz = JavaPsiFacade.getInstance(module.getProject())
        .findClass(JsfConstants.JSF_DATA_MODEL_CLASSNAME, GlobalSearchScope.allScope(module.getProject()));
      if (dataModelClazz != null) {
        final PsiClassType classType = JavaPsiFacade.getInstance(module.getProject()).getElementFactory().createType(dataModelClazz);
        addNotNullContextVariable(createContextVariable(dataModel, dataModel.getName(), classType), annotatedVariables);
      }
    }
  }

  public static Set<String> getGlobalImports(final Module module) {
    Set<String> imports = new HashSet<>();

    @NotNull List<SeamComponents> models = SeamDomModelManager.getInstance(module.getProject()).getAllModels(module);

    for (SeamComponents model : models) {
      for (SeamImport seamImport : model.getImports()) {
        final String stringValue = seamImport.getStringValue();
        if (stringValue != null) {
          imports.add(stringValue);
        }
      }
    }

    return imports;
  }

  public static void addXmlDefinedContextVariables(final Module module, final Set<ContextVariable> vars) {
    final Project project = module.getProject();
    @NotNull List<SeamComponents> models = SeamDomModelManager.getInstance(project).getAllModels(module);

    for (SeamComponents model : models) {
      for (SeamDomComponent seamDomComponent : DomUtil.getDefinedChildrenOfType(model, SeamDomComponent.class)) {
        final String name = seamDomComponent.getComponentName();
        final PsiType type = seamDomComponent.getComponentType();

        if (!StringUtil.isEmptyOrSpaces(name) && type != null && !isAlreadyDefined(name, type, vars)) {
          addNotNullContextVariable(createContextVariable(seamDomComponent, name, type), vars);
        }
      }

      addBundleContextVariables(vars, project, model);
    }
  }

  private static boolean isAlreadyDefined(@NotNull final String name, final Set<? extends ContextVariable> vars) {
    for (ContextVariable var : vars) {
      if (var.getName().equals(name)) return true;
    }
    return false;
  }

  private static boolean isAlreadyDefined(@NotNull final String name, @NotNull final PsiType type, final Set<? extends ContextVariable> vars) {
    for (ContextVariable var : vars) {
      if (var.getName().equals(name) && var.getType().equals(type)) return true;
    }
    return false;
  }

  public static void addXmlDefinedFactoryContextVariables(final Module module,
                                                          final Set<ContextVariable> vars) {
    final Project project = module.getProject();
    @NotNull List<SeamComponents> models = SeamDomModelManager.getInstance(project).getAllModels(module);

    for (SeamComponents model : models) {
      for (SeamDomFactory factory : model.getFactories()) {
        addNotNullContextVariable(createSeamDomFactoryContextVariable(factory, vars, module), vars);
      }
    }
  }

  @Nullable
  private static ContextVariable createSeamDomFactoryContextVariable(final SeamDomFactory factory,
                                                                     final Set<ContextVariable> vars,
                                                                     final Module module) {
    String factoryName = factory.getFactoryName();
    if (StringUtil.isEmptyOrSpaces(factoryName)) return null;

    return new DomFactoryContextVariable(factory, factoryName, vars, module);
  }

  @Nullable
  public static String getFactoryAliasedVarName(SeamDomFactory factory) {
    final String value = factory.getValue().getStringValue();  // aliasing 3.2.7
    if (value != null && isElText(value)) {
      return value.substring(value.indexOf(EL_START) + EL_START.length(), value.indexOf(EL_END)).trim();
    }
    return null;
  }

  @Nullable
  public static PsiType getFactoryType(SeamDomFactory factory, final Module module) {
    return getFactoryType(factory, getSeamContextVariablesWithDependencies(module));
  }

  @Nullable
  public static PsiType getFactoryType(SeamDomFactory factory, Set<? extends ContextVariable> vars) {
    final String value = factory.getValue().getStringValue();  // aliasing 3.2.7
    if (value != null && isElText(value)) {
      String aliasedVar = value.substring(value.indexOf(EL_START) + EL_START.length(), value.indexOf(EL_END));
      for (ContextVariable var : vars) {
        if (var instanceof DomFactoryContextVariable) continue;
        if (var.getName().equals(aliasedVar)) {
          return var.getType();
        }
      }
    }

    return null;
  }

  public static PsiType getObjectClassType(final Project project) {
    final PsiType psiType = getPsiClassTypeByName(project, CommonClassNames.JAVA_LANG_OBJECT);

    return psiType == null ? PsiType.VOID : psiType;
  }

  @Nullable
  private static PsiType getPsiClassTypeByName(final Project project, final String className) {
    final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);

    final PsiClass psiClass = psiFacade.findClass(className, GlobalSearchScope.allScope(project));

    return psiClass == null ? null : psiFacade.getElementFactory().createType(psiClass);
  }

  public static boolean isElText(@NotNull final String s) {
    int startIndex = s.indexOf(EL_START);
    int endIndex = s.indexOf(EL_END);

    return startIndex != -1 && startIndex < endIndex;
  }

  private static void addBundleContextVariables(final Set<? super ContextVariable> vars, final Project project, final SeamComponents model) {
      for (BasicBundleNamesHolder bundleNamesHolder : DomUtil
        .getDefinedChildrenOfType(model, BasicBundleNamesHolder.class)) {
        addBundleContextVariable(bundleNamesHolder, bundleNamesHolder.getBundleNames(), project, vars);

        for (MultiValuedProperty multiValuedProperty : bundleNamesHolder.getBundleNameses()) {
          for (SeamValue value : multiValuedProperty.getValues()) {
            addBundleContextVariable(bundleNamesHolder, value, project, vars);
          }
        }
      }
  }

  private static void addBundleContextVariable(final CommonModelElement modelElement,
                                               final SeamValue bundle,
                                               final Project project,
                                               final Set<? super ContextVariable> vars) {
    final String bundleName = bundle.getStringValue();
    if (bundleName != null) {
      final PsiClassType type = JavaPsiFacade.getInstance(project).getElementFactory()
        .createTypeByFQClassName(CommonClassNames.JAVA_UTIL_PROPERTY_RESOURCE_BUNDLE, GlobalSearchScope.allScope(project));

      addNotNullContextVariable(createContextVariable(modelElement, bundleName, type), vars);
    }
  }

  @Nullable
  private static ContextVariable createContextVariable(final CommonModelElement modelElement, final String name, final PsiType type) {
    if (name == null || name.length() == 0 || type == null) return null;

    return new ContextVariable(modelElement, name, type);
  }

  private static void addNotNullContextVariable(@Nullable final ContextVariable contextVariable, @NotNull final Set<? super ContextVariable> vars) {


    if (contextVariable != null) vars.add(contextVariable);
  }

  @Nullable
  private static ContextVariable createImportedVariables(final ContextVariable var,
                                                         final Set<? extends ContextVariable> vars,
                                                         final Set<String> globalImports) {
    final String name = var.getName();

    for (String globalImport : globalImports) {
      String varName = getShortName(name, globalImport);
      if (!StringUtil.isEmptyOrSpaces(varName) && !isAlreadyDefined(varName, vars)) {
        return createContextVariable(var.getModelElement(), varName, var.getType());
      }
    }

    return null;
  }

  @Nullable
  private static String getShortName(final String name, final String globalImport) {
    if (name.startsWith(globalImport) && name.length() > globalImport.length() && name.charAt(globalImport.length()) == '.') {
      return name.substring(globalImport.length() + 1);
    }

    return null;
  }


  public static Set<SeamFacet> getAllSeamFacets(final Project project) {
    final Set<SeamFacet> result = new HashSet<>();
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      result.addAll(getAllSeamFacets(module));
    }
    return result;
  }

  private static Collection<? extends SeamFacet> getAllSeamFacets(final Module module) {
    return FacetManager.getInstance(module).getFacetsByType(SeamFacet.FACET_TYPE_ID);
  }

  public static boolean isSeamFacetDefined(@Nullable final Module module) {
    return module != null && isModuleContainsSeamFacet(module);
  }

  public static boolean isModuleContainsSeamFacet(final Module module) {
    return SeamFacet.getInstance(module) != null;
  }

  @NotNull
  public static List<CommonModelElement> findSeamComponents(final String name, final Module module) {
    List<CommonModelElement> components = new ArrayList<>();
    for (ContextVariable variable : getSeamContextVariablesWithDependencies(module)) {
      if (name.equals(variable.getName())) {
        components.add(variable.getModelElement());
      }
    }
    return components;
  }

  public static EjbClassRole[] getEjbRoles(SeamJamComponent seamComponent) {
    return EjbHelper.getEjbHelper().getEjbRoles(seamComponent.getPsiElement());
  }

  public static boolean isStateful(final SessionBean sessionBean) {
    return sessionBean.getSessionType().getValue() == SessionType.STATEFUL;
  }

  public static boolean isSeamClass(final PsiClass psiClass) {
    return isSeamComponent(psiClass);
  }

  @Nullable
  public static PsiType getUnwrapType(final PsiClass psiClass) {
    for (PsiMethod method : psiClass.getMethods()) {
      if (AnnotationUtil.isAnnotated(method, SeamAnnotationConstants.UNWRAP_ANNOTATION, 0)) {
        return method.getReturnType();
      }
    }

    return null;
  }

  @Nullable
  public static ContextVariable getContextVariable(final String variableName, final Module module) {
    for (ContextVariable variable : getSeamContextVariablesWithDependencies(module)) {
      if (variableName.equals(variable.getName())) return variable;
    }
    return null;
  }

  public static boolean isSeamComponent(@NotNull final PsiClass aClass) {
    return getSeamJamComponent(aClass) != null;
  }

  @Nullable
  public static SeamJamComponent getSeamJamComponent(PsiClass aClass) {
    return JamService.getJamService(aClass.getProject()).getJamElement(aClass, SeamJamComponent.META);
  }

  // if class has 'abstract' modifier and one of inheritors is seam component
  public static boolean isAbstractSeamComponent(final PsiClass aClass) {
    final PsiModifierList modifierList = aClass.getModifierList();

    if (modifierList == null) return false;

    final boolean isAbstarct = modifierList.hasModifierProperty(PsiModifier.ABSTRACT);

    if (isAbstarct) {
      for (PsiClass inheritorClass : ClassInheritorsSearch.search(aClass).findAll()) {
        if (isSeamComponent(inheritorClass)) return true;
      }
    }

    return false;
  }

  // search @Name annotated components for dom component
  @Nullable
  public static SeamJamComponent getPair(final SeamDomComponent domComponent) {
    final PsiType type = domComponent.getComponentType();
    if (type instanceof PsiClassType) {
      final PsiClass psiClass = ((PsiClassType)type).resolve();
      if (psiClass != null) {
        SeamJamComponent seamComponent = getSeamJamComponent(psiClass);
        if (seamComponent != null) {
          final String componentName = seamComponent.getComponentName();
          if (componentName.equals(domComponent.getComponentName())) {
            return seamComponent;
          }
        }
      }
    }
    return null;
  }

  public static boolean comparelInstalls(final SeamJamComponent seamComponent, final SeamJamComponent checkedComponent) {
    return getPrecedence(seamComponent) == getPrecedence(checkedComponent);
  }

  public static boolean comparelInstalls(final SeamJamComponent seamComponent, final SeamDomComponent checkedComponent) {
    return getPrecedence(seamComponent) == getPrecedence(checkedComponent);
  }

  private static int getPrecedence(final SeamJamComponent seamComponent) {
    final SeamJamInstall install = seamComponent.getInstall();

    return install == null ? SeamInstallPrecedence.APPLICATION : install.getPrecedence();
  }

  private static int getPrecedence(final SeamDomComponent seamComponent) {
    final Integer install = seamComponent.getPrecedence().getValue();

    return install == null ? SeamInstallPrecedence.APPLICATION : install.intValue();
  }

  public static FileTemplate chooseTemplate(final Module module) {
    final String version = JarVersionDetectionUtil.detectJarVersion(SeamConstants.SEAM_DETECTION_CLASS, module);


    final String templateName =
      version != null && version.startsWith("1") ? SeamConstants.FILE_TEMPLATE_NAME_SEAM_1_2 : SeamConstants.FILE_TEMPLATE_NAME_SEAM_2_0;

    return FileTemplateManager.getInstance(module.getProject()).getJ2eeTemplate(templateName);
  }
}