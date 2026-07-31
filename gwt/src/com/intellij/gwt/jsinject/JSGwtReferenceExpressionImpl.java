/*
 * Copyright 2000-2005 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.gwt.jsinject;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.javascript.JSTokenTypes;
import com.intellij.lang.javascript.psi.impl.JSReferenceExpressionImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.JavaClassReferenceProvider;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JSGwtReferenceExpressionImpl extends JSReferenceExpressionImpl {
  public JSGwtReferenceExpressionImpl(final IElementType elementType) {
    super(elementType);
  }

  @Override
  public PsiReference @NotNull [] getReferences() {
    PsiElement at = findPsiChildByType(JSTokenTypes.AT);
    if (at == null) return PsiReference.EMPTY_ARRAY;
    PsiElement classNameStart = at.getNextSibling();
    if (classNameStart == null) return PsiReference.EMPTY_ARRAY;

    PsiElement colon2 = findPsiChildByType(JSTokenTypes.COLON_COLON);
    PsiElement classNameFinish;
    if (colon2 == null) {
      classNameFinish = getLastChild();
    }
    else {
      classNameFinish = colon2.getPrevSibling();
    }
    if (classNameFinish == null) return PsiReference.EMPTY_ARRAY;

    TextRange classNameRange = new TextRange(classNameStart.getStartOffsetInParent(),
                                             classNameFinish.getStartOffsetInParent() + classNameFinish.getTextLength());

    GwtFacet facet = GwtFacet.findFacetByPsiElement(this);
    final boolean shortReferencesSupported = facet == null || facet.getSdkVersion().isShortClassReferencesInJavaScriptSupported();
    JavaClassReferenceProvider referenceProvider = new GwtJsniJavaClassReferenceProvider(shortReferencesSupported);
    PsiReference[] classReferences = referenceProvider.getReferencesByString(classNameRange.substring(getText()), this, classNameRange.getStartOffset());

    PsiElement member = findPsiChildByType(JSTokenTypes.GWT_FIELD_OR_METHOD);
    if (member == null) {
      return classReferences;
    }
    TextRange range = TextRange.from(member.getStartOffsetInParent(), member.getTextLength());
    PsiReference classReference = classReferences.length > 0 ? classReferences[classReferences.length - 1] : null;
    GwtClassMemberReference classMemberReference = new GwtClassMemberReference(this, classReference, range);
    return ArrayUtil.append(classReferences, classMemberReference, PsiReference.class);
  }

  @Override
  public boolean shouldCheckReferences() {
    return false;
  }

  private class GwtJsniJavaClassReferenceProvider extends JavaClassReferenceProvider {
    private final boolean myShortReferencesSupported;

    GwtJsniJavaClassReferenceProvider(boolean shortReferencesSupported) {
      myShortReferencesSupported = shortReferencesSupported;
      if (shortReferencesSupported) {
        setOption(JavaClassReferenceProvider.ADVANCED_RESOLVE, true);
      }
    }

    @Override
    public GlobalSearchScope getScope(@NotNull Project project) {
      return getResolveScope();
    }

    @Override
    public @NotNull PsiFile getContextFile(@NotNull PsiElement element) {
      if (myShortReferencesSupported) {
        PsiLanguageInjectionHost host = InjectedLanguageManager.getInstance(element.getProject()).getInjectionHost(element);
        if (host != null) {
          return host.getContainingFile();
        }
      }
      return super.getContextFile(element);
    }

    @Override
    public @Nullable PsiClass getContextClass(@NotNull PsiElement element) {
      if (myShortReferencesSupported) {
        PsiLanguageInjectionHost host = InjectedLanguageManager.getInstance(element.getProject()).getInjectionHost(element);
        if (host != null) {
          return PsiTreeUtil.getParentOfType(host, PsiClass.class);
        }
      }
      return null;
    }
  }
}