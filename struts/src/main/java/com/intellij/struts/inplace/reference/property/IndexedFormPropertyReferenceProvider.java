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

package com.intellij.struts.inplace.reference.property;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.struts.core.PsiBeanProperty;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * @author Dmitry Avdeev
 */
public class IndexedFormPropertyReferenceProvider extends FormPropertyReferenceProvider {

  @Override
  protected PropertyReference createReference(PropertyReferenceSet set, int index, TextRange range) {
    return new ValidatorFormPropertyReference(set, index, range, this) {

      @Override
      @NotNull
      protected PsiBeanProperty[] getPropertiesForTag(final boolean forVariants) {
        final PsiBeanProperty[] properties = super.getPropertiesForTag(forVariants);
        if (!forVariants) {
          return properties;
        }
        // variants will be filtered
        PsiClass collectionClass = null;
        final ArrayList<PsiBeanProperty> list = new ArrayList<>();
        for (PsiBeanProperty property: properties) {
          final PsiMethod getter = property.getGetter();
          if (getter != null) {
            final PsiType returnType = getter.getReturnType();
            if (returnType != null) {
              if (returnType instanceof PsiClassType) {
                final PsiClass psiClass = ((PsiClassType)returnType).resolve();
                if (psiClass != null) {
                  if (collectionClass == null) {
                    final Project project = psiClass.getProject();
                    collectionClass = JavaPsiFacade.getInstance(project).findClass(CommonClassNames.JAVA_UTIL_LIST, GlobalSearchScope.allScope(project));
                  }
                  if (collectionClass != null && InheritanceUtil.isInheritorOrSelf(psiClass, collectionClass, true)) {
                    list.add(property);
                  }
                }
              } else if (returnType instanceof PsiArrayType) {
                list.add(property);
              }
            }
          }
        }
        return list.toArray(PsiBeanProperty.EMPTY_ARRAY);
      }
    };
  }
}
