// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.groovy.grails.i18n;

import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.options.OptPane;
import com.intellij.lang.properties.PrefixBasedPropertyReference;
import com.intellij.lang.properties.references.PropertyReference;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.inspections.GspAndGroovyInspection;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspAttribute;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;
import org.jetbrains.plugins.grails.references.common.GroovyGspAttributeWrapper;
import org.jetbrains.plugins.grails.references.common.GspAttributeWrapper;
import org.jetbrains.plugins.grails.references.common.XmlGspAttributeWrapper;
import org.jetbrains.plugins.groovy.lang.psi.GroovyRecursiveElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.text.MessageFormat;

import static com.intellij.codeInspection.options.OptPane.checkbox;
import static com.intellij.codeInspection.options.OptPane.pane;

@ApiStatus.Internal
public final class GrailsI18nInspection extends GspAndGroovyInspection {

  private static final String SHORT_NAME = "InvalidI18nProperty";
  private static final Key<GrailsI18nInspection> SHORT_NAME_KEY = Key.create(SHORT_NAME);

  public boolean ignoreTagsWithDefault;

  @Override
  public @NotNull String getShortName() {
    return SHORT_NAME;
  }

  @Override
  public @NotNull OptPane getOptionsPane() {
    return pane(
      checkbox("ignoreTagsWithDefault", GrailsBundle.message("label.text.ignore.if.default.value.specified")));
  }

  @Override
  protected GroovyRecursiveElementVisitor createGroovyFileVisitor(final @NotNull ProblemsHolder holder, final boolean isOnTheFly) {
    return new GroovyRecursiveElementVisitor() {
      @Override
      public void visitNamedArgument(@NotNull GrNamedArgument argument) {
        GrExpression expression = argument.getExpression();
        if (expression instanceof GrLiteral) {
          if (((GrLiteral)expression).isString()) {
            GrCall call = PsiUtil.getCallByNamedParameter(argument);
            if (call instanceof GrMethodCall) {
              GrExpression invokedExpression = ((GrMethodCall)call).getInvokedExpression();
              if (invokedExpression instanceof GrReferenceExpression) {
                PsiElement resolve = ((GrReferenceExpression)invokedExpression).resolve();
                if (resolve instanceof TagLibNamespaceDescriptor.GspTagMethod) {
                  
                  GspAttributeWrapper attr = new GroovyGspAttributeWrapper(argument, (TagLibNamespaceDescriptor.GspTagMethod)resolve);
                  Boolean refType = GrailsI18nPropertyReferenceProvider.getTypeOfReference(attr);

                  if (refType != null && (!refType || !ignoreTagsWithDefault)) {
                    for (PsiReference reference : expression.getReferences()) {
                      if (reference instanceof PropertyReference) {
                        checkReference(holder, refType, isOnTheFly, (PropertyReference)reference);
                        break;
                      }
                    }
                  }
                }
              }
            }
          }

          return;
        }

        super.visitNamedArgument(argument);
      }
    };
  }

  private static void checkReference(ProblemsHolder holder, boolean isSoft, boolean isOnTheFly, @NotNull PropertyReference reference) {
    if (!(reference instanceof PrefixBasedPropertyReference) ||
        !((PrefixBasedPropertyReference)reference).isDynamicPrefix()) {
      
      if (reference.multiResolve(false).length == 0) {
        String pattern = reference.getUnresolvedMessagePattern();
        String message = pattern.contains("{0}") ? MessageFormat.format(pattern, reference.getCanonicalText()) : pattern;

        LocalQuickFix[] quickFixes;
        if (!isOnTheFly) {
          quickFixes = LocalQuickFix.EMPTY_ARRAY;
        }
        else {
          quickFixes = reference.getQuickFixes();

          if (isSoft) {
            quickFixes = ArrayUtil.append(quickFixes, new LocalQuickFix() {

              @Override
              public @NotNull String getFamilyName() {
                return GrailsBundle.message("quick.fix.don.t.check.message.code");
              }

              @Override
              public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
                final PsiElement element = descriptor.getPsiElement();

                InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getCurrentProfile();
                profile.modifyToolSettings(SHORT_NAME_KEY, element, tool -> tool.ignoreTagsWithDefault = true);
              }
            });
          }
        }

        holder.registerProblemForReference(reference, ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, message, quickFixes);
      }
    }
  }

  @Override
  protected GspElementVisitor createGspElementVisitor() {
    return new GspElementVisitor() {
      @Override
      public void visitXmlAttribute(final @NotNull XmlAttribute attribute) {
        if (!(attribute instanceof GspAttribute)) return;

        Boolean refType = GrailsI18nPropertyReferenceProvider.getTypeOfReference(new XmlGspAttributeWrapper((GspAttribute)attribute));

        if (refType != null && (!refType || !ignoreTagsWithDefault)) {
          final XmlAttributeValue attributeValue = attribute.getValueElement();
          if (attributeValue == null) return;

          PropertyReference reference = GrailsI18nPropertyReferenceProvider.getReferenceByElement(attributeValue);
          if (reference == null) return;

          checkReference(getProblemHolder(), refType, isOnTheFly(), reference);
        }
      }
    };
  }
}
