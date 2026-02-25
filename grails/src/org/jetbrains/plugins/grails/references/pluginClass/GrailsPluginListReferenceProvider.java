// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.pluginClass;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GroovyMvcIcons;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.resolve.ElementResolveResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author user
 */
public class GrailsPluginListReferenceProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    PsiField aField = (PsiField)element.getParent().getParent();
    PsiClass aClass = aField.getContainingClass();

    if (!GrailsUtils.isGrailsPluginClass(aClass)) return PsiReference.EMPTY_ARRAY;

    return new PsiReference[]{
      new PsiPolyVariantReferenceBase<>(element, false) {

        private static String extractPluginName(String fileName) {
          String str = fileName.substring(0, fileName.length() - "GrailsPlugin.groovy".length());
          return StringUtil.decapitalize(str);
        }

        @Override
        public Object @NotNull [] getVariants() {
          Set<Object> existingPlugins = new HashSet<>();

          PsiElement element = getElement();

          existingPlugins.add(extractPluginName(element.getContainingFile().getOriginalFile().getName()));

          GrListOrMap list = (GrListOrMap)element.getParent();
          for (GrExpression expression : list.getInitializers()) {
            if (expression != element && expression instanceof GrLiteralImpl) {
              existingPlugins.add(((GrLiteralImpl)expression).getValue());
            }
          }

          List<LookupElement> res = new ArrayList<>();

          Project project = element.getProject();

          for (String fileName : FilenameIndex.getAllFilenames(project)) {
            if (fileName.endsWith("GrailsPlugin.groovy") && fileName.length() > "GrailsPlugin.groovy".length()) {
              PsiFile[] files = FilenameIndex.getFilesByName(project, fileName, GlobalSearchScope.allScope(project));

              for (PsiFile file : files) {
                if (file instanceof GroovyFile) {
                  for (PsiClass psiClass : ((GroovyFile)file).getClasses()) {
                    if (!psiClass.hasModifierProperty(PsiModifier.ABSTRACT) && psiClass.getName().endsWith("GrailsPlugin")) {
                      String str = extractPluginName(fileName);
                      if (existingPlugins.add(str)) {
                        res.add(LookupElementBuilder.create(str).withIcon(GroovyMvcIcons.Groovy_mvc_plugin));
                      }

                      break;
                    }
                  }
                }
              }
            }
          }

          return res.toArray();
        }

        @Override
        public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
          String value = getValue();

          if (value.isEmpty()) return ResolveResult.EMPTY_ARRAY;

          String fileName = StringUtil.capitalize(value) + "GrailsPlugin.groovy";

          Project project = getElement().getProject();

          GlobalSearchScope searchScope = GlobalSearchScope.allScope(project);

          PsiFile[] files = FilenameIndex.getFilesByName(project, fileName, searchScope);

          if (files.length == 0) return ResolveResult.EMPTY_ARRAY;

          List<ResolveResult> res = new ArrayList<>(files.length);

          for (PsiFile file : files) {
            if (file instanceof GroovyFile) {
              for (PsiClass psiClass : ((GroovyFile)file).getClasses()) {
                if (!psiClass.hasModifierProperty(PsiModifier.ABSTRACT) && psiClass.getName().endsWith("GrailsPlugin")) {
                  res.add(new ElementResolveResult<>(psiClass));
                  break;
                }
              }
            }
          }

          return res.toArray(ResolveResult.EMPTY_ARRAY);
        }
      }
    };
  }
}
