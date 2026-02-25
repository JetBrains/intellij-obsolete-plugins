// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.domain.namedQuery;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import org.jetbrains.plugins.grails.references.domain.DomainDescriptor;
import org.jetbrains.plugins.grails.references.domain.GormUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierFlags;
import org.jetbrains.plugins.groovy.lang.psi.api.signatures.GrSignature;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrClosureParameter;
import org.jetbrains.plugins.groovy.lang.psi.impl.signatures.GrClosureSignatureUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightField;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;

public class NamedQueryDescriptor {

  public static final Object NAMED_QUERY_METHOD_MARKER = "Gorm:DomainDescriptor:NamedQueryMethod";

  private final DomainDescriptor myDomainDescriptor;
  private final GrClosableBlock myClosure;
  private final GrMethodCall myMethodCall;
  private final String myName;

  private volatile PsiVariable myVariable;
  private volatile PsiMethod[] myMethods;

  public NamedQueryDescriptor(DomainDescriptor domainDescriptor, String name, GrMethodCall methodCall, GrClosableBlock closure) {
    myClosure = closure;
    myName = name;
    myMethodCall = methodCall;
    myDomainDescriptor = domainDescriptor;
  }

  private void ensureInit() {
    if (myVariable == null) {

      PsiClass domainClass = myDomainDescriptor.getDomainClass();
      PsiManager manager = domainClass.getManager();

      JavaPsiFacade facade = JavaPsiFacade.getInstance(manager.getProject());

      PsiElementFactory factory = facade.getElementFactory();

      PsiType namedCriteriaProxyType = factory.createTypeByFQClassName(GormUtils.NAMED_CRITERIA_PROXY_CLASS_NAME,
                                                                       myClosure.getResolveScope());

      GrLightVariable variable = new GrLightField(domainClass, myName, namedCriteriaProxyType, myMethodCall.getInvokedExpression());
      variable.getModifierList().setModifiers(GrModifierFlags.PUBLIC_MASK + GrModifierFlags.STATIC_MASK);

      variable.setCreatorKey(this);

      final GrSignature signature = GrClosureSignatureUtil.createSignature(myClosure);
      GrClosureParameter[] closureParams = signature.getParameters();

      GrLightMethodBuilder methodWithClosure = new GrLightMethodBuilder(manager, myName);
      methodWithClosure.setMethodKind(NAMED_QUERY_METHOD_MARKER);
      methodWithClosure.setData(this);
      methodWithClosure.setReturnType(null);
      methodWithClosure.setModifiers(GrModifierFlags.PUBLIC_MASK + GrModifierFlags.STATIC_MASK);
      methodWithClosure.setNavigationElement(myMethodCall.getInvokedExpression());
      methodWithClosure.setContainingClass(domainClass);

      GrLightMethodBuilder method = null;

      if (myClosure.hasParametersSection()) {
        for (int i = 0; i < closureParams.length; i++) {
          GrClosureParameter closureParam = closureParams[i];
          PsiType paramType = closureParam.getType();
          if (paramType == null) paramType = PsiType.getJavaLangObject(manager, myClosure.getResolveScope());
          methodWithClosure.addParameter("param" + i, paramType, closureParam.isOptional());
        }

        method = methodWithClosure.copy();
        method.setReturnType(namedCriteriaProxyType);
      }

      methodWithClosure.addParameter("additionalCriteriaClosure", GroovyCommonClassNames.GROOVY_LANG_CLOSURE);

      myMethods = method == null ? new PsiMethod[]{methodWithClosure} : new PsiMethod[]{methodWithClosure, method};
      myVariable = variable;
    }
  }

  public PsiVariable getVariable() {
    ensureInit();
    return myVariable;
  }

  public PsiMethod[] getMethods() {
    ensureInit();
    return myMethods;
  }

  public GrClosableBlock getClosure() {
    return myClosure;
  }

  public DomainDescriptor getDomainDescriptor() {
    return myDomainDescriptor;
  }

  public PsiClass getDomainClass() {
    return myDomainDescriptor.getDomainClass();
  }

  public String getName() {
    return myName;
  }
}
