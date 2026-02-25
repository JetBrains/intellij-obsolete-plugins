// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GroovyMvcIcons;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.parser.GroovyElementTypes;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrStringContent;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrStringImpl;
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns;
import org.jetbrains.plugins.groovy.mvc.plugins.MvcPluginDescriptor;
import org.jetbrains.plugins.groovy.mvc.plugins.MvcPluginUtil;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GrailsPluginNameCompletionContributor extends CompletionContributor {

  private static final String[] SCOPES = {"build", "compile", "runtime", "test", "provided"};

  public static final Pattern DEPENDENCY_FORMAT = Pattern.compile("([^:]*):([^:]*)(?::([^:]*))?");

  private static final ElementPattern<? extends PsiElement> PATTERN = PsiJavaPatterns
    .psiElement().withParent(GroovyPatterns.stringLiteral().withParent(PsiJavaPatterns.psiElement(GrArgumentList.class).withParent(
    GroovyPatterns
      .methodCall().withMethodName(StandardPatterns.string().oneOf(SCOPES)).withParent(PsiJavaPatterns.psiElement(GrClosableBlock.class).withParent(
      GroovyPatterns.methodCall().withMethodName("plugins").withParent(PsiJavaPatterns.psiElement(GrClosableBlock.class).withParent(
        GroovyPatterns.groovyAssignmentExpression().operation(GroovyTokenTypes.mASSIGN).left(
          PsiJavaPatterns.psiElement().withText("grails.project.dependency.resolution"))
          .inFile(PlatformPatterns.psiFile().withName(GrailsUtils.BUILD_CONFIG))
      )))))));

  private static final ElementPattern<? extends PsiElement> PATTERN_GSTRING = PsiJavaPatterns.psiElement(GroovyTokenTypes.mGSTRING_CONTENT).withParent(
    PsiJavaPatterns.psiElement(GroovyElementTypes.GSTRING_CONTENT).withParent(
      PsiJavaPatterns.psiElement(GrStringImpl.class)
        .withParent(PsiJavaPatterns.psiElement(GrArgumentList.class).withParent(
          GroovyPatterns.methodCall().withMethodName(StandardPatterns.string().oneOf(SCOPES)).withParent(
            PsiJavaPatterns.psiElement(GrClosableBlock.class).withParent(
            GroovyPatterns.methodCall().withMethodName("plugins").withParent(PsiJavaPatterns.psiElement(GrClosableBlock.class).withParent(
              GroovyPatterns.groovyAssignmentExpression().operation(GroovyTokenTypes.mASSIGN).left(
                PsiJavaPatterns.psiElement().withText("grails.project.dependency.resolution"))
                .inFile(PlatformPatterns.psiFile().withName(GrailsUtils.BUILD_CONFIG))
            ))))))));

  public GrailsPluginNameCompletionContributor() {
    GrailsPluginNameCompletionProvider provider = new GrailsPluginNameCompletionProvider();

    extend(CompletionType.BASIC, PATTERN, provider);
    extend(CompletionType.BASIC, PATTERN_GSTRING, provider);
  }

  @Override
  public void duringCompletion(@NotNull CompletionInitializationContext context) {
    CharSequence text = context.getEditor().getDocument().getCharsSequence();

    for (int i = context.getEditor().getCaretModel().getOffset(); i < context.getReplacementOffset(); i++) {
      if (text.charAt(i) == ':') {
        context.setReplacementOffset(i);
        break;
      }
    }
  }

  private static class GrailsPluginNameCompletionProvider extends CompletionProvider<CompletionParameters> {
    private static void completePluginName(CompletionParameters parameters, CompletionResultSet result) {
      Module module = ModuleUtilCore.findModuleForPsiElement(parameters.getOriginalFile());
      if (module != null) {
        List<MvcPluginDescriptor> plugins = MvcPluginUtil.loadPluginList(module);

        for (MvcPluginDescriptor plugin : plugins) {
          result.addElement(LookupElementBuilder.create(plugin.getName()).withIcon(GroovyMvcIcons.Groovy_mvc_plugin));
        }
      }
    }

    private static void completePluginVersion(CompletionParameters parameters, @NotNull String pluginName, CompletionResultSet result) {
      Module module = ModuleUtilCore.findModuleForPsiElement(parameters.getOriginalFile());
      if (module != null) {
        List<MvcPluginDescriptor> plugins = MvcPluginUtil.loadPluginList(module);

        MvcPluginDescriptor plugin = null;

        for (MvcPluginDescriptor p : plugins) {
          if (pluginName.equals(p.getName())) {
            plugin = p;
            break;
          }
        }

        if (plugin != null) {
          for (MvcPluginDescriptor.Release release : plugin.getReleases()) {
            LookupElement lookupElement = LookupElementBuilder.create(release.getVersion()).withIcon(GroovyMvcIcons.Groovy_mvc_plugin);
            if (release == plugin.getLastRelease()) {
              lookupElement = PrioritizedLookupElement.withPriority(lookupElement, 1);
            }

            result.addElement(lookupElement);
          }
        }
      }
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
      PsiElement position = parameters.getPosition();

      PsiElement element = position.getParent();
      if (element instanceof GrStringContent) element = element.getParent();

      TextRange range;

      if (element instanceof GrLiteralImpl) {
        range = ElementManipulators.getValueTextRange(element);
        assert element.getTextRange().equals(position.getTextRange());
      }
      else if (element instanceof GrStringImpl) {
        range = new TextRange(0, position.getTextLength());
      }
      else {
        return;
      }

      String text = range.substring(position.getText());

      Matcher matcher = DEPENDENCY_FORMAT.matcher(text);
      if (!matcher.matches()) return;

      int offset = parameters.getOffset() - position.getTextOffset() - range.getStartOffset();

      if (offset >= matcher.start(2) && offset <= matcher.start(2) + matcher.group(2).length()) {
        completePluginName(parameters, result.withPrefixMatcher(text.substring(matcher.start(2), offset)));
      }
      else if (matcher.group(3) != null && offset >= matcher.start(3)) {
        completePluginVersion(parameters, matcher.group(2), result.withPrefixMatcher(text.substring(matcher.start(3), offset)));
      }
    }
  }
}
