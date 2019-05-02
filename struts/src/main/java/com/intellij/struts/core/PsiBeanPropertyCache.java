/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.struts.core;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PropertyUtilBase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

public class PsiBeanPropertyCache {

  @NonNls
  private static final String ACTION_FORM = "org.apache.struts.action.ActionForm";
  @NonNls
  private static final String VALIDATOR_FORM = "org.apache.struts.validator.ValidatorForm";

  @NotNull
  public static PsiBeanPropertyCache getInstance(final Project project) {
    return ServiceManager.getService(project, PsiBeanPropertyCache.class);
  }

  private final HashMap<String, PsiClassInfo> cache = new HashMap<>();

  @NotNull
  public PsiBeanProperty[] getBeanProperties(@Nullable final PsiClass clazz) {
    if (clazz == null) {
      return PsiBeanProperty.EMPTY_ARRAY;
    }

    final String className = clazz.getQualifiedName();
    PsiClassInfo info = cache.get(className);
    if (info != null) {
      final PsiFile psiFile = clazz.getContainingFile();
      if (psiFile != null) {
        final long l = psiFile.getModificationStamp();
        if (info.clazz == clazz && l == info.modificationStamp) {
          return info.props;
        }
      }
    }

    final PsiMethod[] methods = clazz.getAllMethods();
    final Set<PsiBeanPropertyImpl> props = new TreeSet<>();
    for (PsiMethod method : methods) {
      if (!method.hasModifierProperty(PsiModifier.PUBLIC)) {
        continue;
      }
      final String containing = method.getContainingClass().getQualifiedName();
      if (CommonClassNames.JAVA_LANG_OBJECT.equals(containing) ||
        ACTION_FORM.equals(containing) ||
        VALIDATOR_FORM.equals(containing)) {
        continue;
      }

      final String name = PropertyUtilBase.getPropertyName(method);
      if (name != null) {
        PsiField field = PropertyUtilBase.findPropertyField(clazz, name, method.hasModifierProperty(PsiModifier.STATIC));
        if (field != null) {
          if (!field.hasModifierProperty(PsiModifier.PUBLIC)) {
            field = null;
          }
        }
        PsiBeanPropertyImpl prop = new PsiBeanPropertyImpl(clazz, name, field);
        props.add(prop);
      }
    }
    info = new PsiClassInfo();
    info.clazz = clazz;
    final PsiFile psiFile = clazz.getContainingFile();
    if (psiFile != null) {
      info.modificationStamp = psiFile.getModificationStamp();
    }
    info.props = props.toArray(PsiBeanProperty.EMPTY_ARRAY);
    cache.put(className, info);
    return info.props;
  }

  private static class PsiClassInfo {
    private PsiClass clazz;
    private PsiBeanProperty[] props;
    private long modificationStamp;
  }
}
