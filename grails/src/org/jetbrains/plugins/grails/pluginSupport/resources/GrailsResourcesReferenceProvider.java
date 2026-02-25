// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.pluginSupport.resources;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.references.GrailsMethodNamedArgumentReferenceProvider;
import org.jetbrains.plugins.grails.references.common.GrailsFileReferenceSetBase;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.grails.util.SafeReference;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.resolve.GroovyStringLiteralManipulator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrailsResourcesReferenceProvider implements GrailsMethodNamedArgumentReferenceProvider.Contributor {
  @Override
  public void register(GrailsMethodNamedArgumentReferenceProvider registrar) {
    ClassNameCondition classNameCondition = new ClassNameCondition(GrailsResourcesUtil.MODULE_BUILDER_CLASS);

    registrar.register(-1, ResourceModuleReferenceProvider.class, classNameCondition, "dependsOn");

    registrar.register(0, ResourceUrlReferenceProvider.class, classNameCondition, "resource");
    registrar.register("url", ResourceUrlReferenceProvider.class, classNameCondition, "resource");
  }

  public static class ResourceUrlReferenceProvider extends Provider {
    @Override
    protected PsiReference[] createRef(@NotNull PsiElement element, @NotNull GroovyResolveResult method) {
      String text = element.getText();
      assert element instanceof GrLiteralImpl;
      TextRange range = GroovyStringLiteralManipulator.getLiteralRange(text);
      
      String value = range.substring(text);
      if (value.contains(":/")) return PsiReference.EMPTY_ARRAY;

      VirtualFile root = GrailsFramework.getInstance().findAppRoot(element);
      if (root == null) return PsiReference.EMPTY_ARRAY;

      final VirtualFile webApp = root.findChild(GrailsUtils.webAppDir);
      if (webApp == null) return PsiReference.EMPTY_ARRAY;

      GrailsFileReferenceSetBase set = new GrailsFileReferenceSetBase(value, element, range.getStartOffset(), null, true, true) {
        @Override
        protected VirtualFile getDefaultContext(boolean isAbsolute) {
          return webApp;
        }
      };

      return set.getAllReferences();
    }
  }

  public static class ResourceModuleReferenceProvider extends Provider {

    private static final Pattern ourPattern = Pattern.compile("[^\\s,]+");

    public static PsiReference[] createManyModuleReferences(@NotNull PsiElement element, @NotNull String value, int offset) {
      return CachedValuesManager.getCachedValue(element, () -> Result.create(
        doCreateManyModuleReferences(element, value, offset), PsiModificationTracker.MODIFICATION_COUNT
      ));
    }

    private static PsiReference @NotNull [] doCreateManyModuleReferences(@NotNull PsiElement element, @NotNull String value, int offset) {
      List<PsiReference> res = new ArrayList<>();

      Matcher matcher = ourPattern.matcher(value);
      while (matcher.find()) {
        TextRange range = new TextRange(offset + matcher.start(), offset + matcher.end());
        res.add(new GrailsResourceModuleReference(element, range, false));
      }

      PsiReference[] resArray = res.toArray(PsiReference.EMPTY_ARRAY);

      SafeReference.makeReferencesSafe(resArray);

      return resArray;
    }

    @Override
    public PsiReference[] createRef(@NotNull PsiElement element,
                                    @NotNull GrMethodCall methodCall,
                                    int argumentIndex,
                                    @NotNull GroovyResolveResult method) {
      GrLiteralImpl literal = (GrLiteralImpl)element;
      
      String value = (String)literal.getValue();
      assert value != null;
      
      if (value.contains(",")) {
        int startInElement = GroovyStringLiteralManipulator.getLiteralRange(element.getText()).getStartOffset();
        return createManyModuleReferences(element, value, startInElement);
      }
      else {
        return new PsiReference[]{new GrailsResourceModuleReference(element, false)};
      }
    }
  }
}
