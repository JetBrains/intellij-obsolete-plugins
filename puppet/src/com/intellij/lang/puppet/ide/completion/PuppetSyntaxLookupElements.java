package com.intellij.lang.puppet.ide.completion;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Template;
import com.intellij.lang.puppet.PuppetBundle;
import org.jetbrains.annotations.NotNull;

public final class PuppetSyntaxLookupElements {

  public static @NotNull LookupElement getClassDefnitionElement() {
    return LookupElementBuilder
      .create("class...")
      .withTypeText(PuppetBundle.message("puppet.completion.define.class"))
      .withInsertHandler(new PuppetTemplateInsertHandler() {
        @Override
        protected Template getTemplate(InsertionContext context, LookupElement item) {
          return PuppetLiveTemplateBuilder.create(context.getProject())
            .addText("class ")
            .addVariable("name")
            .addText(" (")
            .addVariable("parameters", "")
            .addText(") {\n")
            .addEndVariable()
            .addText("\n}")
            .reformat(true)
            .getTemplate();
        }
      });
  }

  public static @NotNull LookupElement getResourceTypeDefinitionElement() {
    return LookupElementBuilder
      .create("define...")
      .withTypeText(PuppetBundle.message("puppet.completion.define.resource.type"))
      .withInsertHandler(new PuppetTemplateInsertHandler() {
        @Override
        protected Template getTemplate(InsertionContext context, LookupElement item) {
          return PuppetLiveTemplateBuilder.create(context.getProject())
            .addText("define ")
            .addVariable("name")
            .addText(" (")
            .addVariable("parameters", "")
            .addText(") {\n")
            .addEndVariable()
            .addText("\n}")
            .reformat(true)
            .getTemplate();
        }
      });
  }

  public static @NotNull LookupElement getFunctionDefinitionElement() {
    return LookupElementBuilder
      .create("function...")
      .withTypeText(PuppetBundle.message("puppet.completion.define.function"))
      .withInsertHandler(new PuppetTemplateInsertHandler() {
        @Override
        protected Template getTemplate(InsertionContext context, LookupElement item) {
          return PuppetLiveTemplateBuilder.create(context.getProject())
            .addText("function ")
            .addVariable("name")
            .addText(" (")
            .addVariable("parameters", "")
            .addText(") {\n")
            .addEndVariable()
            .addText("\n}")
            .reformat(true)
            .getTemplate();
        }
      });
  }

  public static @NotNull LookupElement getNodeDefinitionElement() {
    return LookupElementBuilder
      .create("node...")
      .withTypeText(PuppetBundle.message("puppet.completion.define.node"))
      .withInsertHandler(new PuppetTemplateInsertHandler() {
        @Override
        protected Template getTemplate(InsertionContext context, LookupElement item) {
          return PuppetLiveTemplateBuilder.create(context.getProject())
            .addText("node ")
            .addVariable("name")
            .addText("{\n")
            .addEndVariable()
            .addText("\n}")
            .reformat(true)
            .getTemplate();
        }
      });
  }

  public static @NotNull LookupElement getIfCompoundElement() {
    return LookupElementBuilder
      .create("if...")
      .withTypeText(PuppetBundle.message("puppet.completion.if"))
      .withInsertHandler(new PuppetTemplateInsertHandler() {
        @Override
        protected Template getTemplate(InsertionContext context, LookupElement item) {
          return PuppetLiveTemplateBuilder.create(context.getProject())
            .addText("if ")
            .addVariable("condition", "true")
            .addText(" {\n")
            .addEndVariable()
            .addText("\n}")
            .reformat(true)
            .getTemplate();
        }
      });
  }

  public static @NotNull LookupElement getUnlessCompoundElement() {
    return LookupElementBuilder
      .create("unless...")
      .withTypeText(PuppetBundle.message("puppet.completion.unless"))
      .withInsertHandler(new PuppetTemplateInsertHandler() {
        @Override
        protected Template getTemplate(InsertionContext context, LookupElement item) {
          return PuppetLiveTemplateBuilder.create(context.getProject())
            .addText("unless ")
            .addVariable("condition", "true")
            .addText(" {\n")
            .addEndVariable()
            .addText("\n}")
            .reformat(true)
            .getTemplate();
        }
      });
  }

  public static @NotNull LookupElement getElsifCompoundElement() {
    return LookupElementBuilder
      .create("elsif...")
      .withTypeText(PuppetBundle.message("puppet.completion.elsif"))
      .withInsertHandler(new PuppetTemplateInsertHandler() {
        @Override
        protected Template getTemplate(InsertionContext context, LookupElement item) {
          return PuppetLiveTemplateBuilder.create(context.getProject())
            .addText("elsif ")
            .addVariable("condition", "true")
            .addText(" {\n")
            .addEndVariable()
            .addText("\n}")
            .reformat(true)
            .getTemplate();
        }
      });
  }

  public static @NotNull LookupElement getElseCompoundElement() {
    return LookupElementBuilder
      .create("else...")
      .withTypeText(PuppetBundle.message("puppet.completion.else"))
      .withInsertHandler(new PuppetTemplateInsertHandler() {
        @Override
        protected Template getTemplate(InsertionContext context, LookupElement item) {
          return PuppetLiveTemplateBuilder.create(context.getProject())
            .addText("else {\n")
            .addEndVariable()
            .addText("\n}")
            .reformat(true)
            .getTemplate();
        }
      });
  }

  public static @NotNull LookupElement getCaseCompoundElement() {
    return LookupElementBuilder
      .create("case...")
      .withTypeText(PuppetBundle.message("puppet.completion.case"))
      .withInsertHandler(new PuppetTemplateInsertHandler() {
        @Override
        protected Template getTemplate(InsertionContext context, LookupElement item) {
          return PuppetLiveTemplateBuilder.create(context.getProject())
            .addText("case ")
            .addVariable("controlexpression", "true")
            .addText(" {\n")
            .addVariable("variant", "default")
            .addText(": {")
            .addVariable("code", "")
            .addText("}\n")
            .addEndVariable()
            .addText("\n}")
            .reformat(true)
            .getTemplate();
        }
      });
  }

}
