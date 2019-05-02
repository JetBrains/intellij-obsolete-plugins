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

package com.intellij.struts.inplace.reference;

import com.intellij.jsp.impl.TldTagDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlElementDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utilities for finding enclosing JSP custom tags.
 *
 * @author davdeev
 */
public class XmlReferenceUtil {

  /**
   * Name of virtual root tag in JSPs.
   */
  @NonNls
  private static final String ROOT = "root";

  private XmlReferenceUtil() {
  }

  /**
   * Find the parent tag within the same namespace by walking up recursively.
   *
   * @param tag              Tag to start from.
   * @param enclosingTagName Name of enclosing tag.
   * @return Enclosing tag with the given name or {@code null} if none found.
   */
  @Nullable
  public static XmlTag findEnclosingTag(@NotNull final XmlTag tag, @NotNull @NonNls final String enclosingTagName) {
    return findEnclosingTag(tag, enclosingTagName, tag.getNamespace(), null);
  }

  /**
   * Find the parent tag within the same namespace and the given TagClass by walking up recursively.
   *
   * @param tag              Tag to start from.
   * @param enclosingTagName Name of enclosing tag.
   * @param tagClassName     TagClass of enclosing tag.
   * @return Enclosing tag with the given attributes or {@code null} if none found.
   */
  @Nullable
  public static XmlTag findEnclosingTagByClass(@NotNull final XmlTag tag,
                                               @NotNull @NonNls final String enclosingTagName,
                                               @NotNull @NonNls final String tagClassName) {
    final Project project = tag.getProject();
    final PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(tagClassName, GlobalSearchScope.allScope(project));
    return findEnclosingTag(tag, enclosingTagName, tag.getNamespace(), psiClass);
  }

  /**
   * Find the parent tag with the given attributes by walking up recursively.
   *
   * @param tag              Tag to start from.
   * @param enclosingTagName Name of enclosing tag.
   * @param ns               Namespace of enclosing tag (opt). Only one of ns or clazz can be specified.
   * @param clazz            TagClass of enclosing tag (opt). Only one of ns or clazz can be specified.
   * @return Enclosing tag with the given attributes or {@code null} if none found.
   */
  @Nullable
  protected static XmlTag findEnclosingTag(@NotNull final XmlTag tag,
                                           @NotNull final String enclosingTagName,
                                           @Nullable final String ns,
                                           @Nullable final PsiClass clazz) {

    final String name = tag.getLocalName();
    if (name.equals(ROOT)) {
      return null;
    }

    if (name.equals(enclosingTagName)) {
      if (ns != null) {
        final String namespace = tag.getNamespace();
        if (namespace.equals(ns)) {
          return tag;
        }
      }

      if (clazz != null) {
        final XmlElementDescriptor descriptor = tag.getDescriptor();
        if (descriptor instanceof TldTagDescriptor) {
          final String tagClassName = ((TldTagDescriptor) descriptor).getTagClass();
          if (tagClassName != null) {
            final Project project = tag.getProject();
            final PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(tagClassName, GlobalSearchScope.allScope(project));
            if (InheritanceUtil.isInheritorOrSelf(psiClass, clazz, true)) {
              return tag;
            }
          }
        }
      }
    }

    final PsiElement parent = tag.getContext();
    if (!(parent instanceof XmlTag)) {
      return null;
    }

    return findEnclosingTag((XmlTag) parent, enclosingTagName, ns, clazz); // walk up
  }

}