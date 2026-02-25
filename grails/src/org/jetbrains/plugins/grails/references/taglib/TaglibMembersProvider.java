// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.taglib;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.GspTagLibUtil;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;
import org.jetbrains.plugins.grails.references.MemberProvider;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtilKt;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

import java.util.Map;

public class TaglibMembersProvider extends MemberProvider {

  private static final String CLASS_SOURCE = "class TagLibProperties {" +
                                             GrailsUtils.COMMON_WEB_PROPERTIES +
                                             " private org.codehaus.groovy.grails.web.util.GrailsPrintWriter getOut() {}" +
                                             " private void setOut(java.io.Writer newOut) {}" +
                                             " private String getPluginContextPath() {}" +
                                             " private groovy.lang.Binding getPageScope() {}" +
                                             " private void throwTagError(String message) {}" +
                                             "}";

  @Override
  public void processMembers(PsiScopeProcessor processor, PsiClass psiClass, GrReferenceExpression ref) {
    JavaPsiFacade facade = JavaPsiFacade.getInstance(psiClass.getProject());

    GlobalSearchScope resolveScope = psiClass.getResolveScope();

    PsiClass apiClass = facade.findClass("org.codehaus.groovy.grails.plugins.web.api.TagLibraryApi", resolveScope);
    if (apiClass != null) {
      PsiType objectType = PsiType.getJavaLangObject(psiClass.getManager(), resolveScope);
      if (!GrailsPsiUtil.enhance(processor, apiClass, objectType)) return;
    }
    else {
      if (!DynamicMemberUtils.process(processor, psiClass, ref, CLASS_SOURCE)) return;
    }

    Map<String,TagLibNamespaceDescriptor> tagLibClasses = GspTagLibUtil.getTagLibClasses(ref);

    String nameHint = ResolveUtil.getNameHint(processor);

    if (!GspTagLibUtil.processGrailsTags(processor, ref, ResolveState.initial(), nameHint, processor.getHint(ElementClassHint.KEY), tagLibClasses)) return;

    if (ResolveUtilKt.shouldProcessMethods(processor)) {
      // Process tags from current taglib namespace if this namespace if not "g"
      TagLibNamespaceDescriptor myDescriptor = findDescriptorByTaglibClass(tagLibClasses, psiClass);

      if (myDescriptor != null && !GspTagLibUtil.DEFAULT_TAGLIB_PREFIX.equals(myDescriptor.getNamespacePrefix())) {
        myDescriptor.processTags(processor, ResolveState.initial(), nameHint);
      }
    }
  }

  private static TagLibNamespaceDescriptor findDescriptorByTaglibClass(Map<String,TagLibNamespaceDescriptor> tagLibClasses, PsiClass aClass) {
    PsiClass originalClass = PsiUtil.getOriginalClass(aClass);

    for (TagLibNamespaceDescriptor descriptor : tagLibClasses.values()) {
      if (descriptor.getClasses().contains(originalClass)) {
        return descriptor;
      }
    }

    return null;
  }
}
