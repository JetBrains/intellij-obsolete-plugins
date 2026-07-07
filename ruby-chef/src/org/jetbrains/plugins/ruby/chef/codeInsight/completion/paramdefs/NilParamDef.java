package org.jetbrains.plugins.ruby.chef.codeInsight.completion.paramdefs;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.util.Consumer;
import icons.RubyIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.ruby.codeInsight.completion.RubyCompletionGrouping;
import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.LookupItemType;
import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.ParamDefBase;
import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.ParamDefImplUtil;
import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.RubyCallArgumentContext;

import java.util.function.Predicate;

public class NilParamDef extends ParamDefBase {
  @Override
  public void processAllVariants(@NotNull RubyCallArgumentContext context,
                                 @NotNull Consumer<? super LookupElement> elementsConsumer,
                                 @NotNull Predicate<String> lookupStringFilter) {
    elementsConsumer.consume(ParamDefImplUtil.createLookupElement(
      "nil", LookupItemType.None, context.getArgumentElement(), RubyIcons.Ruby.Ruby, null, RubyCompletionGrouping.RUBY_KEYWORDS
    ));
  }

  @Override
  protected boolean inspectionEnabledFor(RubyCallArgumentContext context) {
    return false;
  }
}
