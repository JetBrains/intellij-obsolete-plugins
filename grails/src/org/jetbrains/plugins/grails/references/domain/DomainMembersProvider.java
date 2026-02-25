// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.references.MemberProvider;
import org.jetbrains.plugins.grails.references.domain.namedQuery.NamedQueryDescriptor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.util.GrStaticChecker;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.Map;

import static org.jetbrains.plugins.grails.references.domain.DomainClassUtils.Condition;
import static org.jetbrains.plugins.grails.references.domain.DomainClassUtils.DOMAIN_COUNT;
import static org.jetbrains.plugins.grails.references.domain.DomainClassUtils.DOMAIN_FIND;
import static org.jetbrains.plugins.grails.references.domain.DomainClassUtils.DOMAIN_FINDER_EXPRESSIONS_WITH_ONE_PARAMETER;
import static org.jetbrains.plugins.grails.references.domain.DomainClassUtils.DOMAIN_FIND_ALL;
import static org.jetbrains.plugins.grails.references.domain.DomainClassUtils.DOMAIN_FIND_OR_CREATE;
import static org.jetbrains.plugins.grails.references.domain.DomainClassUtils.DOMAIN_FIND_OR_SAVE;
import static org.jetbrains.plugins.grails.references.domain.DomainClassUtils.DOMAIN_LIST_ORDER;
import static org.jetbrains.plugins.grails.references.domain.DomainClassUtils.FinderMethod;

/**
 * @author maxim.medvedev
 */
public class DomainMembersProvider extends MemberProvider {

  public static final Object FINDER_METHOD_MARKER = new Object();
  public static final String[] MODIFIERS_PUBLIC_STATIC = {PsiModifier.PUBLIC, PsiModifier.STATIC};

  @Override
  public void processMembers(PsiScopeProcessor processor, PsiClass psiClass, PsiElement place) {
    if (place instanceof GrReferenceExpression refExpr) {
      multiResolve(refExpr, psiClass, processor);
    }
    else {
      processSimpleMembers(psiClass, processor);
    }
  }

  private static boolean processSimpleMembers(PsiClass domainClass, PsiScopeProcessor processor) {
    String name = ResolveUtil.getNameHint(processor);

    if (ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) {
      DomainDescriptor descriptor = DomainDescriptor.getDescriptor(domainClass);

      if (!descriptor.processStaticMethods(processor, name, ResolveState.initial())) return false;
      if (!descriptor.processDynamicMethods(processor, name, ResolveState.initial())) return false;
    }
    return true;
  }

  private static boolean executeNamedQueryDescriptor(PsiScopeProcessor processor,
                                                     NamedQueryDescriptor descr,
                                                     ElementClassHint classHint) {
    if (ResolveUtil.shouldProcessProperties(classHint)) {
      if (!processor.execute(descr.getVariable(), ResolveState.initial())) return false;
    }

    if (ResolveUtil.shouldProcessMethods(classHint)) {
      for (PsiMethod method : descr.getMethods()) {
        if (!processor.execute(method, ResolveState.initial())) return false;
      }
    }

    return true;
  }

  private static boolean multiResolve(GrReferenceExpression myRefExpr, PsiClass domainClass, PsiScopeProcessor processor) {
    ElementClassHint classHint = processor.getHint(ElementClassHint.KEY);

    String name = ResolveUtil.getNameHint(processor);

    final boolean isInStaticContext = GrStaticChecker.isInStaticContext(myRefExpr, domainClass);

    DomainDescriptor descriptor = DomainDescriptor.getDescriptor(domainClass);

    if (GormUtils.isNamedQueryDeclaration(myRefExpr)) {
      return true;
    }

    if (!processNamedQueries(processor, descriptor, name, classHint)) {
      return false;
    }

    if (ResolveUtil.shouldProcessMethods(classHint)) {
      if (!descriptor.processStaticMethods(processor, name, ResolveState.initial())) return false;

      if (!isInStaticContext) {
        if (!descriptor.processDynamicMethods(processor, name, ResolveState.initial())) return false;
      }
      if (!testForStaticFinderMethod(myRefExpr, descriptor, processor)) return false;
    }
    return true;
  }

  public static boolean processNamedQueries(PsiScopeProcessor processor,
                                            DomainDescriptor descriptor,
                                            String nameHint,
                                            ElementClassHint classHint) {
    if (nameHint == null) {
      for (NamedQueryDescriptor descr : descriptor.getNamedQueries().values()) {
        if (!executeNamedQueryDescriptor(processor, descr, classHint)) return false;
      }
    }
    else {
      NamedQueryDescriptor descr = descriptor.getNamedQueries().get(nameHint);
      if (descr != null) {
        if (!executeNamedQueryDescriptor(processor, descr, classHint)) return false;
      }
    }

    return true;
  }

  private static boolean testForStaticFinderMethod(GrReferenceExpression refExpr,
                                                   DomainDescriptor descriptor,
                                                   PsiScopeProcessor processor) {
    String name = ResolveUtil.getNameHint(processor);

    if (name == null) {
      PsiClassType returnType = TypesUtil.createListType(descriptor.getDomainClass());

      for (String fieldName : descriptor.getPersistentProperties().keySet()) {
        GrLightMethodBuilder builder = new GrLightMethodBuilder(descriptor.getDomainClass().getManager(),
                                                                DOMAIN_LIST_ORDER + StringUtil.capitalize(fieldName));
        builder.setModifiers(MODIFIERS_PUBLIC_STATIC);
        builder.setReturnType(returnType);
        builder.addOptionalParameter("args", CommonClassNames.JAVA_UTIL_MAP);
        builder.setMethodKind(FINDER_METHOD_MARKER);

        if (!processor.execute(builder, ResolveState.initial())) return false;
      }

      return true;
    }

    if (name.startsWith(DOMAIN_LIST_ORDER)) {
      if (descriptor.getPersistentProperties().containsKey(StringUtil.decapitalize(name.substring(DOMAIN_LIST_ORDER.length())))) {
        PsiClassType returnType = TypesUtil.createListType(descriptor.getDomainClass());

        GrLightMethodBuilder builder = new GrLightMethodBuilder(descriptor.getDomainClass().getManager(), name);
        builder.setModifiers(MODIFIERS_PUBLIC_STATIC);
        builder.setReturnType(returnType);
        builder.addOptionalParameter("args", CommonClassNames.JAVA_UTIL_MAP);
        builder.setMethodKind(FINDER_METHOD_MARKER);
        builder.setContainingClass(descriptor.getDomainClass());

        if (!processor.execute(builder, ResolveState.initial())) return false;
      }

      return true;
    }

    name = refExpr.getReferenceName();
    if (name != null) {
      PsiMethod finderMethod = parseFinderMethod(name, descriptor);
      if (finderMethod != null) {
        if (!processor.execute(finderMethod, ResolveState.initial())) return false;
      }
    }

    return true;
  }

  public static @Nullable GrLightMethodBuilder parseFinderMethod(@NotNull String name, @NotNull DomainDescriptor descriptor) {
    FinderMethod finderMethod = DomainClassUtils.parseFinderMethod(name);
    if (finderMethod == null) return null;

    PsiType returnType;

    String prefix = finderMethod.getPrefix();
    if (prefix.equals(DOMAIN_COUNT)) {
      returnType = PsiTypes.intType();
    }
    else if (prefix.equals(DOMAIN_FIND) || prefix.equals(DOMAIN_FIND_OR_CREATE) || prefix.equals(DOMAIN_FIND_OR_SAVE)) {
      returnType = PsiTypesUtil.getClassType(descriptor.getDomainClass());
    }
    else {
      assert prefix.equals(DOMAIN_FIND_ALL);
      returnType = TypesUtil.createListType(descriptor.getDomainClass());
    }

    GrLightMethodBuilder builder = new GrLightMethodBuilder(descriptor.getDomainClass().getManager(), name);
    builder.setModifiers(MODIFIERS_PUBLIC_STATIC);
    builder.setReturnType(returnType);
    builder.setContainingClass(descriptor.getDomainClass());

    Map<String, Pair<PsiType, PsiElement>> properties = descriptor.getPersistentProperties();

    for (Condition condition : finderMethod.getConditions()) {
      String finderExpr = condition.getFinderExpr();
      String capitalizedName = condition.getFieldName();

      String fieldName = StringUtil.decapitalize(capitalizedName);

      Pair<PsiType, PsiElement> pair = properties.get(fieldName);
      if (pair == null) {
        // Fields with names like "pHash" may be capitalized as "PHash" and as "pHash"
        if (fieldName.length() > 1 && Character.isUpperCase(fieldName.charAt(0))) {
          fieldName = Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
          pair = properties.get(fieldName);
        }

        if (pair == null) {
          return null;
        }
      }

      final PsiType propertyType = pair.getFirst();
      if (finderExpr == null || DOMAIN_FINDER_EXPRESSIONS_WITH_ONE_PARAMETER.contains(finderExpr)) {
        builder.addParameter(fieldName, propertyType);
      }
      else if (finderExpr.equals("InList")) {
        builder.addParameter("list", CommonClassNames.JAVA_UTIL_LIST);
      }
      else if (finderExpr.equals("InRange")) {
        builder.addParameter("range", "groovy.lang.Range");
      }
      else if (finderExpr.equals("Between")) {
        builder.addParameter("lower" + capitalizedName, propertyType);
        builder.addParameter("upper" + capitalizedName, propertyType);
      }
    }

    builder.addOptionalParameter("paginateParams", CommonClassNames.JAVA_UTIL_MAP);
    builder.setMethodKind(FINDER_METHOD_MARKER);

    return builder;
  }
}
