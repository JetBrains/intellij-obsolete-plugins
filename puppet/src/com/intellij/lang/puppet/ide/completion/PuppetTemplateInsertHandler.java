package com.intellij.lang.puppet.ide.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.impl.LiveTemplateLookupElementImpl;
import org.jetbrains.annotations.NotNull;

public abstract class PuppetTemplateInsertHandler implements InsertHandler<LookupElement> {

  protected abstract Template getTemplate(InsertionContext context, LookupElement item);

  @Override
  public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
    LiveTemplateLookupElementImpl.startTemplate(context, getTemplate(context, item));
  }
}
