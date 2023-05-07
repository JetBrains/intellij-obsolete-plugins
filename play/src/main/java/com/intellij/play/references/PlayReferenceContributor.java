package com.intellij.play.references;

import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.play.constants.PlayConstants;
import com.intellij.play.language.PlayElementTypes;
import com.intellij.play.language.psi.PlayNameValueCompositeElement;
import com.intellij.play.language.psi.PlayTag;
import com.intellij.psi.*;
import com.intellij.psi.filters.PsiMethodCallFilter;
import com.intellij.psi.filters.position.FilterPattern;
import com.intellij.psi.filters.position.ParentElementFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;

import static com.intellij.patterns.PlatformPatterns.*;
import static com.intellij.patterns.PsiJavaPatterns.psiElement;
import static com.intellij.patterns.PsiJavaPatterns.psiLiteral;

public class PlayReferenceContributor extends PsiReferenceContributor {

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registerExtendDirectiveReferenceProvider(registrar);
    registerIncludeDirectiveReferenceProvider(registrar);

    registerCustomTagsReferenceProvider(registrar);
    registerMessagesReferenceProvider(registrar);
    registerRoutesReferenceProvider(registrar);
    registerRenderTemplateReferenceProvider(registrar);
    registerReverseRouterReferenceProvider(registrar);
    registerCssSourceReferenceProvider(registrar);
    registerScriptSourceReferenceProvider(registrar);

    registerRenderArgsPutMethodReferenceProvider(registrar);
    registerFlashScopePutMethodReferenceProvider(registrar);
    registerListTagIteratorReferenceProvider(registrar);

    registerInterceptorAnnotationsReferenceProvider(registrar);
  }

  private static void registerInterceptorAnnotationsReferenceProvider(PsiReferenceRegistrar registrar) {
    String[] annotations = {"play.mvc.Before", "play.mvc.After", "play.mvc.Finally"};
    PlayControllerMethodsReferenceProvider provider = new PlayControllerMethodsReferenceProvider();

    registrar.registerReferenceProvider(psiLiteral().insideAnnotationParam(string().oneOf(annotations), "unless"), provider);
    registrar.registerReferenceProvider(psiLiteral().insideAnnotationParam(string().oneOf(annotations), "only"), provider);
  }

  private static void registerReverseRouterReferenceProvider(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(
      PsiJavaPatterns.literalExpression().and(new FilterPattern(new ParentElementFilter(new PsiMethodCallFilter(
        PlayConstants.ROUTER_CLASS, "reverse", "getFullUrl"), 2))), new PlayControllerActionPsiReferenceProvider());
  }

  private static void registerListTagIteratorReferenceProvider(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(psiElement(GrLiteral.class).withSuperParent(2, psiElement(PlayNameValueCompositeElement.class)
      .withName("as").withSuperParent(1, psiElement(PlayTag.class).withName("list"))), new PlayFakeRenameableReferenceProvider());
  }

  private static void registerRenderArgsPutMethodReferenceProvider(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(
      PsiJavaPatterns.literalExpression().and(new FilterPattern(new ParentElementFilter(new PsiMethodCallFilter(
        PlayConstants.RENDER_ARG_SCOPE_CLASS, "put"), 2))), new PlayFakeRenameableReferenceProvider());
  }

  private static void registerFlashScopePutMethodReferenceProvider(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(
      PsiJavaPatterns.literalExpression().and(new FilterPattern(new ParentElementFilter(new PsiMethodCallFilter(
        PlayConstants.FLASH_SCOPE_CLASS, "put"), 2))), new PlayFakeRenameableReferenceProvider());
  }

  private static void registerRenderTemplateReferenceProvider(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(
      PsiJavaPatterns.literalExpression().and(new FilterPattern(new ParentElementFilter(new PsiMethodCallFilter(
        PlayConstants.CONTROLLER_CLASS, "render"), 2))), new PlayRenderViewsPsiReferenceProvider());
  }

  private static void registerCustomTagsReferenceProvider(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(psiElement(PlayTag.class), new PlayCustomTagPsiReferenceProvider());
  }

  private static void registerExtendDirectiveReferenceProvider(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(psiElement(GrLiteral.class).withSuperParent(2, psiElement(PlayTag.class).withName("extends")),
                                        new PlayPathViewsPsiReferenceProvider());
  }

  private static void registerIncludeDirectiveReferenceProvider(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(psiElement(GrLiteral.class).withSuperParent(2, psiElement(PlayTag.class).withName("include")),
                                        new PlayPathViewsPsiReferenceProvider());
  }

  private static void registerMessagesReferenceProvider(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(psiElement(GrLiteral.class).withSuperParent(1, psiElement(PlayElementTypes.MESSAGE_TEXT))
                                          .afterLeaf(psiElement(PlayElementTypes.MESSAGE_START)),
                                        new PlayMessagePsiReferenceProvider());
  }

  private static void registerCssSourceReferenceProvider(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(psiElement(GrLiteral.class).withSuperParent(2, psiElement(PlayNameValueCompositeElement.class)
      .withName("src").withSuperParent(1, psiElement(PlayTag.class).withName("stylesheet"))), new PlayPathViewsPsiReferenceProvider());
  }

  private static void registerScriptSourceReferenceProvider(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(psiElement(GrLiteral.class).withSuperParent(2, psiElement(PlayNameValueCompositeElement.class)
      .withName("src").withSuperParent(1, psiElement(PlayTag.class).withName("script"))), new PlayPathViewsPsiReferenceProvider());
  }

  private static void registerRoutesReferenceProvider(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(psiFile().withName("routes"), new PlayRoutesPsiReferenceProvider());
  }
}
