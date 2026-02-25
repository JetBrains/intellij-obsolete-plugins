// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.GrailsClosureMemberContributor;
import org.jetbrains.plugins.grails.structure.GrailsCommonClassNames;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentReferenceProvider;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.HashMap;
import java.util.Map;

public class GormMappingMethodProvider implements GrailsClosureMemberContributor.MethodProvider {
  private static final Map<String, NamedArgumentDescriptor> NAMED_ARGUMENTS;

  private static final Object GORM_MAPPING_METHOD_KEY = "GormMappingMethodProvider.gormMappingMethod";

  // #CHECK#
  // @see org.codehaus.groovy.grails.orm.hibernate.cfg.HibernateMappingBuilder#handleMethodMissing
  static {
    NAMED_ARGUMENTS = new HashMap<>();
    for (String s : new String[]{"formula", "type", "lazy", "insertable", "updateable", "cascade", "sort", "order",
      "batchSize", "ignoreNotFound", "params", "fetch", "column", "sqlType", "enumType", "index", "unique", "length", "precision",
      "scale", "cache", "indexColumn", "joinTable"}) {
      NAMED_ARGUMENTS.put(s, NamedArgumentDescriptor.SIMPLE_ON_TOP);
    }
  }

  @Override
  public boolean processMembers(@NotNull GrClosableBlock closure,
                                PsiClass artifactClass,
                                PsiScopeProcessor processor,
                                GrReferenceExpression refExpr,
                                ResolveState state) {
    Project project = artifactClass.getProject();

    GrailsCommonClassNames namesFactory = GrailsCommonClassNames.getInstance(closure);

    PsiClass aClass = JavaPsiFacade.getInstance(project).findClass(namesFactory.getHibernateMappingBuilder(), refExpr.getResolveScope());
    if (aClass != null) {
      if (!aClass.processDeclarations(processor, state, null, refExpr)) return false;
    }

    Map<String, Pair<PsiType, PsiElement>> propertiesMap = DomainDescriptor.getDescriptor(artifactClass).getPersistentProperties();

    String nameHint = ResolveUtil.getNameHint(processor);

    if (nameHint == null) {
      for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : propertiesMap.entrySet()) {
        PsiMethod method = createMethod(entry.getKey(), entry.getValue().second, artifactClass);
        if (!processor.execute(method, ResolveState.initial())) return false;
      }
    }
    else {
      Pair<PsiType, PsiElement> pair = propertiesMap.get(nameHint);
      if (pair != null) {
        PsiMethod method = createMethod(nameHint, pair.second, artifactClass);
        if (!processor.execute(method, ResolveState.initial())) return false;
      }
    }

    return true;
  }

  private static PsiMethod createMethod(String name, PsiElement navigationElement, @NotNull PsiClass domainClass) {
    GrLightMethodBuilder res = new GrLightMethodBuilder(navigationElement.getManager(), name);
    res.addOptionalParameter("args", CommonClassNames.JAVA_UTIL_MAP);
    res.setNavigationElement(navigationElement);
    res.setNamedParameters(NAMED_ARGUMENTS);

    res.setMethodKind(GORM_MAPPING_METHOD_KEY);
    res.setData(domainClass);
    res.setContainingClass(domainClass);

    return res;
  }

  public static class SortAttributeReferenceProvider implements GroovyNamedArgumentReferenceProvider {
    @Override
    public PsiReference[] createRef(@NotNull PsiElement element,
                                    @NotNull GrNamedArgument namedArgument,
                                    @NotNull GroovyResolveResult resolveResult,
                                    @NotNull ProcessingContext context) {
      PsiClass domainClass = ((GrLightMethodBuilder)resolveResult.getElement()).getData();
      return new PsiReference[]{new GormPropertyReference(element, false, domainClass)};
    }
  }
}
