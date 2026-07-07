package org.jetbrains.plugins.ruby.chef.codeInsight.completion;

import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.filters.ElementFilter;
import com.intellij.psi.filters.position.FilterPattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.chef.ChefUtil;
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.blocks.RBodyStatement;
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.blocks.RCompoundStatement;
import org.jetbrains.plugins.ruby.ruby.lang.psi.expressions.RListOfExpressions;
import org.jetbrains.plugins.ruby.ruby.lang.psi.iterators.RDoBlockCall;
import org.jetbrains.plugins.ruby.ruby.lang.psi.iterators.RDoCodeBlock;
import org.jetbrains.plugins.ruby.ruby.lang.psi.methodCall.RCall;
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RConstant;
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RIdentifier;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.PlatformPatterns.psiFile;

public final class ChefResourceReferenceContributor extends PsiReferenceContributor {
  public static final class Holder {
    private static final FilterPattern CHEF_FILTER = new FilterPattern(
      new ElementFilter() {
        @Override
        public boolean isAcceptable(Object element, @Nullable PsiElement context) {
          return ChefUtil.isInCookbook(context);
        }

        @Override
        public boolean isClassAcceptable(Class hintClass) {
          return true;
        }
      });

    private static final PsiElementPattern.Capture<RCompoundStatement>
      CHEF_RESOURCES_CONTAINER = psiElement(RCompoundStatement.class).withParent(psiFile().and(CHEF_FILTER));

    private static final PsiElementPattern.Capture<RDoBlockCall>
      CHEF_RESOURCE_DECLARATION = psiElement(RDoBlockCall.class).withParent(CHEF_RESOURCES_CONTAINER);

    private static final PsiElementPattern.Capture<PsiElement> CHEF_IDENTIFIER_OR_CONSTANT =
      psiElement().andOr(psiElement(RIdentifier.class),
                         psiElement(RConstant.class));

    public static final PsiElementPattern.Capture<PsiElement>
      CHEF_RESOURCE_NAME_COMPLETION =
      psiElement().andOr(CHEF_IDENTIFIER_OR_CONSTANT.withParent(psiElement(RCall.class).withParent(CHEF_RESOURCE_DECLARATION)),
                         CHEF_IDENTIFIER_OR_CONSTANT.withParent(CHEF_RESOURCES_CONTAINER));

    private static final PsiElementPattern.Capture<RCompoundStatement> CHEF_RESOURCE_CONTENT =
      psiElement(RCompoundStatement.class).withParent(
        psiElement(RBodyStatement.class).withParent(
          psiElement(RDoCodeBlock.class).withParent(
            CHEF_RESOURCE_DECLARATION)));

    public static final PsiElementPattern.Capture<PsiElement> CHEF_RESOURCE_CONTENT_PATTERN = psiElement().andOr(psiElement().withParent(
      CHEF_RESOURCE_CONTENT), psiElement().withParent(psiElement(RCall.class).withParent(CHEF_RESOURCE_CONTENT)));

    private static final PsiElementPattern.Capture<PsiElement> CHEF_ATTRIBUTE_ARGUMENT_PATTERN =
      psiElement().withParent(psiElement(RListOfExpressions.class).withParent(RCall.class).withParent(CHEF_RESOURCE_CONTENT_PATTERN));
  }
  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(Holder.CHEF_RESOURCE_CONTENT_PATTERN, ChefResourceAttributeReferenceProvider.getInstance());
    registrar.registerReferenceProvider(Holder.CHEF_ATTRIBUTE_ARGUMENT_PATTERN, ChefResourceAttributeParamReferenceProvider.getInstance());
  }
}
