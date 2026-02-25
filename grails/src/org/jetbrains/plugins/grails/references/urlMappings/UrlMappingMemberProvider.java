// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.urlMappings;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.plugins.grails.references.MemberProvider;
import org.jetbrains.plugins.grails.structure.GrailsCommonClassNames;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

public class UrlMappingMemberProvider extends MemberProvider {

  @Override
  public void processMembers(PsiScopeProcessor processor, PsiClass psiClass, PsiElement place) {
    if (!ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) return;

    String nameHint = ResolveUtil.getNameHint(processor);

    GrClosableBlock closableBlock = PsiTreeUtil.getParentOfType(place, GrClosableBlock.class);
    if (closableBlock == null) return;

    var commonClassNames = GrailsCommonClassNames.getInstance(place);

    PsiClass urlMappingClass = JavaPsiFacade.getInstance(psiClass.getProject()).findClass(commonClassNames.getUrlMappingBuilder(), psiClass.getResolveScope());
    if (urlMappingClass == null) return;

    if (UrlMappingUtil.isMappingField(closableBlock)) {
      if (UrlMappingUtil.GROUP.equals(nameHint)) {
        PsiMethod[] nameMethods = urlMappingClass.findMethodsByName(UrlMappingUtil.GROUP, true);
        for (PsiMethod method : nameMethods) {
          if (!processor.execute(method, ResolveState.initial())) return;
        }
      }

      if ("name".equals(nameHint)) { // Don't check 'nameHint == null', because method name(Map) should not be exist in completion.
        PsiMethod[] nameMethods = urlMappingClass.findMethodsByName("name", true);
        for (PsiMethod method : nameMethods) {
          if (!processor.execute(method, ResolveState.initial())) return;
        }
      }
    }
    else {
      PsiElement eMethodCall = closableBlock.getParent();
      if (eMethodCall instanceof GrArgumentList) eMethodCall = eMethodCall.getParent();

      if (eMethodCall instanceof GrMethodCall && UrlMappingUtil.isMappingDefinition((GrMethodCall)eMethodCall)) {
        if (nameHint == null || nameHint.equals("constraints")) {
          if (nameHint != null || !hasConstraintsBlock(closableBlock)) {
            GrLightMethodBuilder builder = new GrLightMethodBuilder(psiClass.getManager(), "constraints");
            builder.addParameter("closure", GroovyCommonClassNames.GROOVY_LANG_CLOSURE);
            if (!processor.execute(builder, ResolveState.initial())) return;
          }
        }

        urlMappingClass.processDeclarations(processor, ResolveState.initial(), null, place);
      }
    }
  }

  private static boolean hasConstraintsBlock(GrClosableBlock block) {
    for (PsiElement e = block.getFirstChild(); e != null; e = e.getNextSibling()) {
      if (e instanceof GrMethodCall) {
        if (PsiUtil.isReferenceWithoutQualifier(((GrMethodCall)e).getInvokedExpression(), "constraints")) {
          return true;
        }
      }
    }

    return false;
  }
}
