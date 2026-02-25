// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.buildConfig;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.common.GrailsRootBasedFileReferenceSet;
import org.jetbrains.plugins.grails.util.GrailsPatterns;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns;

import java.util.Set;

public class BuildConfigFileReferenceProvider extends PsiReferenceProvider {

  private static final Set<String> SUPPORT_PROPERTIES = ContainerUtil.newHashSet(
    "grails.work.dir",
    "grails.project.work.dir",
    "grails.project.war.exploded.dir",
    "grails.project.plugins.dir",
    "grails.global.plugins.dir",
    "grails.project.resource.dir",
    "grails.project.source.dir",
    "grails.project.web.xml",
    "grails.project.class.dir",
    "grails.project.plugin.class.dir",
    "grails.project.test.class.dir",
    "grails.project.test.reports.dir",
    "grails.project.docs.output.dir",
    "grails.project.test.source.dir",
    "grails.project.target.dir",
    "grails.project.war.file", "grails.war.destFile"
  );


  private static boolean isInlinePluginReference(GrReferenceExpression ref) {
    GrExpression expression = ref.getQualifierExpression();
    if (expression != null) {
      return "grails.plugin.location".equals(expression.getText());
    }

    return false;
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    GrAssignmentExpression assignmentExpression = (GrAssignmentExpression)element.getParent();

    GrExpression left = assignmentExpression.getLValue();
    if (!(left instanceof GrReferenceExpression)) return PsiReference.EMPTY_ARRAY;

    String text = left.getText();

    if (!SUPPORT_PROPERTIES.contains(text) && !isInlinePluginReference((GrReferenceExpression)left)) return PsiReference.EMPTY_ARRAY;

    return GrailsRootBasedFileReferenceSet.createReferences(element);
  }

  public static void register(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(
      GroovyPatterns.rightOfAssignment(GroovyPatterns.stringLiteral(), GroovyPatterns.groovyAssignmentExpression().withParent(
        GrailsPatterns.buildConfig())),

      new BuildConfigFileReferenceProvider());
  }
}
