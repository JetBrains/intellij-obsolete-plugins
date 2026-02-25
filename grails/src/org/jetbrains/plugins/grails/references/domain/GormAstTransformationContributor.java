// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.infos.CandidateInfo;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.gorm.GormVersion;
import org.jetbrains.plugins.grails.gorm.GormVersionKt;
import org.jetbrains.plugins.grails.util.GrailsArtifactTransformerUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightField;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyPropertyUtils;
import org.jetbrains.plugins.groovy.lang.resolve.CollectClassMembersUtil;
import org.jetbrains.plugins.groovy.transformations.AstTransformationSupport;
import org.jetbrains.plugins.groovy.transformations.TransformationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo.BELONGS_TO_NAME;
import static org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo.HAS_MANY_NAME;
import static org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo.HAS_ONE_NAME;
import static org.jetbrains.plugins.grails.perspectives.graph.DomainClassRelationsInfo.RELATES_TO_MANY_NAME;
import static org.jetbrains.plugins.grails.references.domain.DomainDescriptor.findAllPropertiesFromField;

public final class GormAstTransformationContributor implements AstTransformationSupport {

  // See GormValidationTransformer.EXCLUDES
  private static final Collection<String> EXCLUDED_FROM_VALIDATION_API = Arrays.asList("setErrors", "getErrors"
                                                                                       // "hasErrors", // hasErrors() exists in GormValidationTransformer.EXCLUDES, but it is injected by GormValidationTransformer.addErrorsProperty()
    , "getBeforeValidateHelper", "setBeforeValidateHelper", "getValidator", "setValidator");

  @Override
  public void applyTransformation(@NotNull TransformationContext context) {
    GrTypeDefinition aClass = context.getCodeClass();
    if (!GormUtils.isGormBean(aClass) || !shouldInject(aClass)) return;

    injectIdVersion(context);

    context.addMethod(new GrLightMethodBuilder(aClass));
    final PsiType myMapType = TypesUtil.createTypeByFQClassName(CommonClassNames.JAVA_UTIL_MAP, aClass);
    context.addMethod(new GrLightMethodBuilder(aClass).addParameter("namedArgs", myMapType));

    GormVersion version = GormVersionKt.getGormVersion(aClass);
    if (version != GormVersion.BELOW_4) return;

    List<PsiMethod> methods = new ArrayList<>();
    {
      JavaPsiFacade facade = JavaPsiFacade.getInstance(aClass.getProject());
      GlobalSearchScope resolveScope = aClass.getResolveScope();

      PsiClass instanceApi = facade.findClass(DomainDescriptor.GORM_INSTANCE_API_CLASS, resolveScope);
      if (instanceApi != null) {
        GrailsArtifactTransformerUtils.enhanceAst(instanceApi, context, methods, false, new GormAstTransformationMethodFilter() {
          @Override
          public boolean value(PsiMethod method) {
            // See GormTransformer.isCandidateInstanceMethod()
            if (!super.value(method)) return false;

            String name = method.getName();

            if (GroovyPropertyUtils.isGetterName(name)) {
              return method.getParameterList().getParametersCount() != 1;
            }
            else if (GroovyPropertyUtils.isSetterName(name)) {
              return method.getParameterList().getParametersCount() != 2;
            }

            return true;
          }
        });
      }

      PsiClass staticApi = facade.findClass("org.grails.datastore.gorm.GormStaticApi", resolveScope);
      if (staticApi != null) {
        GrailsArtifactTransformerUtils.enhanceAst(staticApi, context, methods, true, new GormAstTransformationMethodFilter() {
          @Override
          public boolean value(PsiMethod method) {
            // See GormTransformer.isStaticCandidateMethod()
            if (!super.value(method)) return false;

            String name = method.getName();
            //if ("create".equals(name)) return false; //GormTransformer.isStaticCandidateMethod() don't allow method with name 'create', but method 'create()' will adds in other place.

            if (GroovyPropertyUtils.isGetterName(name)) {
              // #CHECK# There is a bug in GormTransformer.isStaticCandidateMethod() methods: it uses GormTransformer.isGetter/isSetter methods, but they can't work with static methods.
              // #CHECH 2.0.0.M2#
              return method.getParameterList().getParametersCount() != 1;
            }
            else if (GroovyPropertyUtils.isSetterName(name)) {
              return method.getParameterList().getParametersCount() != 2;
            }

            return true;
          }
        });
      }

      PsiClass validationApi = facade.findClass("org.grails.datastore.gorm.GormValidationApi", resolveScope);
      if (validationApi != null) {
        GrailsArtifactTransformerUtils
          .enhanceAst(validationApi, context, methods, false, new GormAstTransformationMethodFilter() {
            @Override
            public boolean value(PsiMethod method) {
              if (!super.value(method)) return false;
              return !EXCLUDED_FROM_VALIDATION_API.contains(method.getName());
            }
          });
      }

      for (PsiMethod method : methods) {
        ((GrLightMethodBuilder)method).setMethodKind(DomainDescriptor.DOMAIN_DYNAMIC_METHOD);
      }
    }

    context.addMethods(methods);
  }

  private static void injectIdVersion(@NotNull TransformationContext context) {
    GrTypeDefinition aClass = context.getCodeClass();
    Map<String, CandidateInfo> allFields = CollectClassMembersUtil.getAllFields(aClass, false);

    // Inject id & version.
    if (!allFields.containsKey("id")) {
      GrField id = new GrLightField(aClass, "id", CommonClassNames.JAVA_LANG_LONG);
      context.addField(id);
    }
    if (!allFields.containsKey("version")) {
      GrField id = new GrLightField(aClass, "version", CommonClassNames.JAVA_LANG_LONG);
      context.addField(id);
    }

    Function<PsiClass, PsiClass> superClass = (c) -> c == aClass ? context.getSuperClass() : c.getSuperClass();
    Set<String> existingFieldNames = allFields.keySet();
    Map<String, Pair<PsiType, PsiElement>> setTypeFields = new HashMap<>();
    findAllPropertiesFromField(setTypeFields, HAS_MANY_NAME, aClass, superClass);
    findAllPropertiesFromField(setTypeFields, RELATES_TO_MANY_NAME, aClass, superClass);
    for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : setTypeFields.entrySet()) {
      String fieldName = entry.getKey();

      if (!existingFieldNames.contains(fieldName)) {
        PsiType type = TypesUtil.createSetType(aClass, entry.getValue().first);
        GrField field = new GrLightField(aClass, fieldName, type, entry.getValue().second);
        context.addField(field);
      }
    }
    existingFieldNames = ContainerUtil.union(existingFieldNames, setTypeFields.keySet());

    Map<String, Pair<PsiType, PsiElement>> regularTypeFields = new HashMap<>();
    findAllPropertiesFromField(regularTypeFields, HAS_ONE_NAME, aClass, superClass);
    findAllPropertiesFromField(regularTypeFields, BELONGS_TO_NAME, aClass, superClass);
    for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : regularTypeFields.entrySet()) {
      String fieldName = entry.getKey();

      if (!existingFieldNames.contains(fieldName)) {
        GrField field = new GrLightField(aClass, fieldName, entry.getValue().first, entry.getValue().second);
        context.addField(field);
      }
    }
  }

  // See DefaultGrailsDomainClassInjector.shouldInjectClass()
  private static boolean shouldInject(PsiClass aClass) {
    if (aClass.isEnum()) return false;

    VirtualFile virtualFile = aClass.getContainingFile().getOriginalFile().getVirtualFile();
    if (virtualFile == null) return false;

    VirtualFile folder = virtualFile.getParent();
    if (folder != null) {
      if (folder.findChild(aClass.getName() + ".hbm.xml") != null) {
        return false;
      }
    }

    return true;
  }

  private static class GormAstTransformationMethodFilter extends GrailsArtifactTransformerUtils.DefaultFilter {

    public static final GormAstTransformationMethodFilter INSTANCE = new GormAstTransformationMethodFilter();

    // See AbstractGormApi.EXCLUDES
    private static final Set<String> EXCLUDED_METHODS = ContainerUtil.newHashSet(
      "setProperty",
      "getProperty",
      "getMetaClass",
      "setMetaClass",
      "invokeMethod",
      "getMethods",
      "getExtendedMethods",
      "wait",
      "equals",
      "toString",
      "hashCode",
      "getClass",
      "notify",
      "notifyAll",
      "setTransactionManager");

    @Override
    public boolean value(PsiMethod method) {
      return super.value(method) && !EXCLUDED_METHODS.contains(method.getName());
    }
  }
}
