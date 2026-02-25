// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.constraints;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentReferenceProvider;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.PsiImplUtil;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GrailsConstraintsSharedReferenceProvider implements GroovyNamedArgumentReferenceProvider {

  @Override
  public PsiReference[] createRef(@NotNull PsiElement element,
                                  @NotNull GrNamedArgument namedArgument,
                                  @NotNull GroovyResolveResult resolveResult,
                                  @NotNull ProcessingContext context) {
    return new PsiReference[]{
      new PsiReferenceBase<>(element, false) {

        private Map<String, PsiElement> getConstraintGroups(@Nullable String hint) {
          Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
          if (module == null) return Collections.emptyMap();

          VirtualFile conf = GrailsUtils.findConfDirectory(module);
          if (conf == null) return Collections.emptyMap();

          VirtualFile configGroovy = conf.findChild(GrailsUtils.CONFIG_GROOVY);
          if (configGroovy == null) return Collections.emptyMap();

          PsiFile configGroovyPsi = getElement().getManager().findFile(configGroovy);
          if (!(configGroovyPsi instanceof GroovyFile)) return Collections.emptyMap();

          Map<String, PsiElement> res = new HashMap<>();

          for (PsiElement e = configGroovyPsi.getFirstChild(); e != null; e = e.getNextSibling()) {
            if (e instanceof GrAssignmentExpression) {
              GrExpression lValue = ((GrAssignmentExpression)e).getLValue();
              if (lValue.getText().equals(GrailsConstraintsUtil.GRAILS_GORM_DEFAULT_CONSTRAINTS)) {
                PsiElement eClosure = ((GrAssignmentExpression)e).getRValue();
                if (eClosure instanceof GrClosableBlock) {
                  for (PsiElement m = eClosure.getFirstChild(); m != null; m = m.getNextSibling()) {
                    if (m instanceof GrMethodCall && !PsiImplUtil.hasExpressionArguments(((GrMethodCall)m).getArgumentList())) {
                      String methodName = PsiUtil.getMethodName((GrMethodCall)m);
                      if (methodName != null && methodName.indexOf('*') == -1 && (hint == null || hint.equals(methodName))) {
                        res.put(methodName, m);
                        if (hint != null) return res;
                      }
                    }
                  }
                }
              }
            }
          }

          return res;
        }

        @Override
        public PsiElement resolve() {
          String value = getValue();
          Map<String, PsiElement> map = getConstraintGroups(value);
          return map.get(value);
        }

        @Override
        public Object @NotNull [] getVariants() {
          Set<String> strings = getConstraintGroups(null).keySet();
          return ArrayUtil.toObjectArray(strings);
        }
      }
    };
  }
}
