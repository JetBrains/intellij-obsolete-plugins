// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.controller;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.DelegatingScopeProcessor;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.GspTagLibUtil;
import org.jetbrains.plugins.grails.references.MemberProvider;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierFlags;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrMethodWrapper;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

/**
 * @author Maxim.Medvedev
 */
public class ControllerMembersProvider extends MemberProvider {

  public static final Object CONTROLLER_METHOD_KIND = "ControllerMembersProvider_controller_method";

  /**
   * #CHECK#
   *
   * @see ControllersGrailsPlugin.registerControllerMethods
   */
  private static final String CLASS_SOURCE = "/**\n" +
                                             "  * @kind " + CONTROLLER_METHOD_KIND + '\n' +
                                             "  */\n" +
                                             "class ControllerElements {" +
                                             GrailsUtils.COMMON_WEB_PROPERTIES +
                                             " private String getActionUri() {}" +
                                             " private String getControllerUri() {}" +
                                             " private String getTemplateUri(String name){}" +
                                             " private String getViewUri(String name){}" +
                                             " private void setErrors(org.springframework.validation.Errors errors) {}" +
                                             " private org.springframework.validation.Errors getErrors() {}" +
                                             " private void setModelAndView(org.springframework.web.servlet.ModelAndView modelAndView) {}" +
                                             " private org.springframework.web.servlet.ModelAndView getModelAndView() {}" +
                                             " private java.util.Map getChainModel() {}" +
                                             " private boolean hasErrors(){}" +
                                             " private void redirect(Map params){def z = params.uri + params.url + params.controller + params.action + params.id + params.fragment + params.params}" +
                                             " private void chain(Map params){def z = params.uri + params.controller + params.action + params.id + params.model + params.params}" +
                                             " private void render(Closure cl){}" +
                                             " private void render(Map params, Closure cl = null){def z = params.text + params.builder " +
                                             "+ params.view + params.template + params.var + params.bean + params.model + params.collection " +
                                             "+ params.contentType + params.encoding + params.converter + params.plugin + params.status + params.contextPath}" +
                                             " private void render(String text){}" +
                                             " private void render(org.codehaus.groovy.grails.web.converters.Converter converter){}" +
                                             " private void bindData(def target, Map params, List<String> excludes, String prefix){}" +
                                             " private void bindData(def target, Map params, List<String> excludes){}" +
                                             " private void bindData(def target, Map params, Map excludes, String prefix){}" +
                                             " private void bindData(def target, Map params, Map excludes){}" +
                                             " private void bindData(def target, Map params, String prefix){}" +
                                             " private void bindData(def target, Map params){}" +
                                             " private org.codehaus.groovy.grails.web.metaclass.InvalidResponseHandler withForm(Closure closure){}" +
                                             " private void forward(Map params){def z = params.controller + params.action + params.id + params.params}" +

                                             // from org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesGrailsPlugin#addWithFormatMethod
                                             " private Map withFormat(Closure closure){}" +
                                             "}";

  private static final String TEST_VARIABLES = """
    /**
      * @originalInfo injected in MockUtils#mockController()
      */
    class TestControllerVariablesHolder {  public Map getForwardArgs(){}  public Map getRedirectArgs(){}  public Map getRenderArgs(){}  public Map getChainArgs(){}}""";

  public static final String CONTROLLER_API_CLASS = "org.codehaus.groovy.grails.plugins.web.api.ControllersApi";

  public static final String MIME_API_CLASS = "org.codehaus.groovy.grails.plugins.web.api.ControllersMimeTypesApi";

  @Override
  public void processMembers(PsiScopeProcessor processor, PsiClass psiClass, GrReferenceExpression ref) {
    PsiScopeProcessor executeProcessor = processor;

    ElementClassHint classHint = processor.getHint(ElementClassHint.KEY);

    if (ResolveUtil.shouldProcessMethods(classHint)) {
      if (GrailsUtils.isInGrailsTests(ref)) {
        if (!DynamicMemberUtils.process(processor, psiClass, ref, TEST_VARIABLES)) return;

        executeProcessor = new TestProcessor(processor);
      }
    }

    JavaPsiFacade facade = JavaPsiFacade.getInstance(psiClass.getProject());

    GlobalSearchScope resolveScope = psiClass.getResolveScope();

    PsiClass apiClass = facade.findClass(CONTROLLER_API_CLASS, resolveScope);
    if (apiClass != null) {
      // Grails version >= 1.4
      PsiType objectType = PsiType.getJavaLangObject(psiClass.getManager(), resolveScope);

      if (!GrailsPsiUtil.enhance(executeProcessor, apiClass, objectType, CONTROLLER_METHOD_KIND)) return;

      PsiClass mimeApiClass = facade.findClass(MIME_API_CLASS, resolveScope);
      if (mimeApiClass != null) {
        if (!GrailsPsiUtil.enhance(executeProcessor, mimeApiClass, objectType, CONTROLLER_METHOD_KIND)) return;
      }

      PsiClass controllerRestApiClass = facade.findClass("org.grails.plugins.web.rest.api.ControllersRestApi", resolveScope);
      if (controllerRestApiClass != null) {
        if (!GrailsPsiUtil.enhance(executeProcessor, controllerRestApiClass, objectType, CONTROLLER_METHOD_KIND)) return;
      }
    }
    else {
      // Grails version < 1.4
      if (!DynamicMemberUtils.process(executeProcessor, psiClass, ref, CLASS_SOURCE)) return;
    }

    if (!GspTagLibUtil.processGrailsTags(processor, ref, ResolveState.initial(), ResolveUtil.getNameHint(processor), classHint)) return;
  }

  private static class TestProcessor extends DelegatingScopeProcessor {

    TestProcessor(PsiScopeProcessor delegate) {
      super(delegate);
    }

    @Override
    public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
      if (element instanceof PsiMethod method) {
        String name = method.getName();

        String overriddenType = null;

        if ("getRequest".equals(name)) {
          overriddenType = "org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest";
        }
        else if ("getResponse".equals(name)) {
          overriddenType = "org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse";
        }

        if (overriddenType != null) {
          GrMethodWrapper builder = GrMethodWrapper.wrap(method);
          builder.setReturnType(TypesUtil.createType(overriddenType, element));
          builder.setModifiers(GrModifierFlags.PUBLIC_MASK);
          return super.execute(builder, state);
        }
      }

      return super.execute(element, state);
    }
  }
}
