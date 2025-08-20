package com.intellij.play.utils.processors;

import com.intellij.play.utils.beans.PlayImplicitVariable;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import java.util.HashSet;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ImplicitVariablesProcessor implements PlayDeclarationsProcessor {
  static Map<String, String> myImplicitVariables = new HashMap<>();

  static {
    myImplicitVariables.put("errors", CommonClassNames.JAVA_UTIL_LIST);
    myImplicitVariables.put("error", "play.data.validation.Error");
    myImplicitVariables.put("flash", "play.mvc.Scope.Flash");
    myImplicitVariables.put("lang", "play.i18n.Lang");
    myImplicitVariables.put("messages", "play.i18n.Messages");
    myImplicitVariables.put("out", "java.io.PrintWriter");
    myImplicitVariables.put("params", "play.mvc.Scope.Params");
    myImplicitVariables.put("play", "play.Play");
    myImplicitVariables.put("request", "play.mvc.Http.Request");
    myImplicitVariables.put("session", "play.mvc.Scope.Session");
    myImplicitVariables.put("_response_encoding", "java.lang.String");
    myImplicitVariables.put("result", "java.lang.Exception");  //PlayHandler, ServletWrapper
    myImplicitVariables.put("exception", "java.lang.Exception");  //PlayHandler, ServletWrapper

  }

  @Override
  public boolean processElement(PsiScopeProcessor processor, ResolveState state, PsiElement scope) {
    for (PlayImplicitVariable variableBase : getPredefinedVariables(scope)) {
      if (!ResolveUtil.processElement(processor, variableBase, state)) return false;
    }
    return true;
  }

  public static Set<PlayImplicitVariable> getPredefinedVariables(final PsiElement scope) {
    Set<PlayImplicitVariable> implicitSet = new HashSet<>();

    final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(scope.getProject());
    for (Map.Entry<String, String> entry : myImplicitVariables.entrySet()) {
      final PsiClass psiClass = psiFacade.findClass(entry.getValue(), scope.getResolveScope());
      if (psiClass != null) {
        final String name = entry.getKey();
        implicitSet.add(PlayImplicitVariablesFactory.createLightClassImplicitVariable(psiClass, name, false));
      }
    }
    return implicitSet;
  }
}
