package com.intellij.lang.puppet.ide.completion;

import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.ConstantNode;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class PuppetLiveTemplateBuilder {

  private final @NotNull Template myTemplate;

  private PuppetLiveTemplateBuilder(@NotNull Project project, @NotNull String key, @NotNull String group) {
    myTemplate = TemplateManager.getInstance(project).createTemplate(key, group);
  }

  public static @NotNull PuppetLiveTemplateBuilder create(@NotNull Project project) {
    return new PuppetLiveTemplateBuilder(project, "", "");
  }

  public @NotNull PuppetLiveTemplateBuilder addText(@NotNull String text) {
    myTemplate.addTextSegment(text);
    return this;
  }

  public @NotNull PuppetLiveTemplateBuilder addSelectionStartVariable() {
    myTemplate.addSelectionStartVariable();
    return this;
  }

  public @NotNull PuppetLiveTemplateBuilder reformat(boolean reformat) {
    myTemplate.setToReformat(reformat);
    return this;
  }

  public @NotNull PuppetLiveTemplateBuilder indent(boolean indent) {
    myTemplate.setToIndent(indent);
    return this;
  }

  public @NotNull PuppetLiveTemplateBuilder shortenLongNames(boolean shortenLongNames) {
    myTemplate.setToShortenLongNames(shortenLongNames);
    return this;
  }

  public @NotNull PuppetLiveTemplateBuilder inline(boolean inline) {
    myTemplate.setInline(inline);
    return this;
  }

  public @NotNull PuppetLiveTemplateBuilder addSelectionEndVariable() {
    myTemplate.addSelectionEndVariable();
    return this;
  }

  public @NotNull PuppetLiveTemplateBuilder addEndVariable() {
    myTemplate.addEndVariable();
    return this;
  }

  public @NotNull PuppetLiveTemplateBuilder addVariable(@NotNull String name) {
    return addVariable(name, name);
  }

  public @NotNull PuppetLiveTemplateBuilder addVariable(@NotNull String name, @NotNull String expression) {
    return addVariable(name, expression, "");
  }

  public @NotNull PuppetLiveTemplateBuilder addVariable(@NotNull String name, @NotNull String expression, @NotNull String defaultValue) {
    return addVariable(name, expression, defaultValue, true);
  }

  public @NotNull PuppetLiveTemplateBuilder addVariable(@NotNull String name,
                                                        @NotNull String expression,
                                                        @NotNull String defaultValue,
                                                        boolean isAlwaysStopAt) {
    myTemplate.addVariable(name, new ConstantNode(expression), new ConstantNode(defaultValue), isAlwaysStopAt);
    return this;
  }

  public @NotNull Template getTemplate() {
    return myTemplate;
  }
}
