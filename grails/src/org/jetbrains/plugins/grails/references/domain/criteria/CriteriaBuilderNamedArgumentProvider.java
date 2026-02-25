// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.criteria;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.references.domain.DomainClassUtils;
import org.jetbrains.plugins.grails.references.domain.DomainMembersProvider;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor.SIMPLE_ON_TOP;
import static org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor.TYPE_BOOL;
import static org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor.TYPE_MAP;
import static org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor.TYPE_STRING;

final class CriteriaBuilderNamedArgumentProvider extends GroovyNamedArgumentProvider {

  // #CHECK# List of arguments see in GrailsHibernateUtil.populateArgumentsForCriteria(...)
  private static final Map<String, NamedArgumentDescriptor> MAP = new HashMap<>();
  static {
      MAP.put("max", SIMPLE_ON_TOP);
      MAP.put("offset", SIMPLE_ON_TOP);
      MAP.put("order", TYPE_STRING);
      MAP.put("fetch", TYPE_MAP);
      MAP.put("sort", TYPE_STRING);
      MAP.put("cache", SIMPLE_ON_TOP);
      MAP.put("lock", SIMPLE_ON_TOP);
      MAP.put("ignoreCase", TYPE_BOOL);
  }

  @Override
  public void getNamedArguments(@NotNull GrCall call,
                                @NotNull GroovyResolveResult resolveResult,
                                @Nullable String argumentName,
                                boolean forCompletion,
                                @NotNull Map<String, NamedArgumentDescriptor> result) {
    PsiElement resolve = resolveResult.getElement();
    if (!(resolve instanceof PsiMethod method)) return;

    if (
      (CriteriaBuilderImplicitMemberContributor.isMine(method)
       && "list".equals(method.getName())
       && method.getParameterList().getParametersCount() == 2)
      || (GrLightMethodBuilder.checkKind(method, DomainMembersProvider.FINDER_METHOD_MARKER) && method.getName().startsWith(DomainClassUtils.DOMAIN_LIST_ORDER))) {

      if (argumentName == null) {
        result.putAll(MAP);
      }
      else {
        NamedArgumentDescriptor argumentDescriptor = MAP.get(argumentName);
        if (argumentDescriptor != null) {
          result.put(argumentName, argumentDescriptor);
        }
      }
    }
  }
}
