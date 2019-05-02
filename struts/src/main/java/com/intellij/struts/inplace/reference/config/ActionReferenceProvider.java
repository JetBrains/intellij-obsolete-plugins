/*
 * Copyright 2000-2006 JetBrains s.r.o.
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

package com.intellij.struts.inplace.reference.config;

import com.intellij.javaee.web.ServletPathReferenceProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.inplace.reference.path.ActionWebPathsProvider;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA.
 * User: DAvdeev
 *
 * see http://struts.apache.org/1.2.9/userGuide/struts-html.html#form
 */
public class ActionReferenceProvider extends PsiReferenceProvider {

  private final static ActionWebPathsProvider PROVIDER = new ActionWebPathsProvider(false);
  @Override
  @NotNull
  public PsiReference[] getReferencesByElement(@NotNull final PsiElement element, @NotNull final ProcessingContext context) {
    final StrutsModel model = StrutsManager.getInstance().getStrutsModel(element);

    if (model != null) {
      final PsiReference[] references = PROVIDER.createReferences(element, model.getServletMappingInfo(), false);
      final PsiReference mappingReference = ServletPathReferenceProvider.createMappingReference(element);
      return mappingReference == null ? references : ArrayUtil.append(references, mappingReference, PsiReference.class);
    } else {
      return PsiReference.EMPTY_ARRAY;
    }
  }
}
