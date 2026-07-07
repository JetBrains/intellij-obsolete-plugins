package org.jetbrains.plugins.ruby.chef.codeInsight.completion.paramdefs;

import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.util.Consumer;
import icons.RubyIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.ruby.codeInsight.completion.RubyLookupElement;
import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.ParamDefBase;
import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.RubyCallArgumentContext;

import java.util.function.Predicate;

public class NumberParamDef extends ParamDefBase {
  private final String myText;
  private final int myPriority;

  public NumberParamDef(String text, int priority) {
    myText = text;
    myPriority = priority;
  }

  @Override
  public void processAllVariants(@NotNull RubyCallArgumentContext context,
                                 @NotNull Consumer<? super LookupElement> elementsConsumer,
                                 @NotNull Predicate<String> lookupStringFilter) {
    elementsConsumer.consume(PrioritizedLookupElement
                               .withPriority(new RubyLookupElement("\"" + myText + "\"", null, null, true, RubyIcons.Ruby.Ruby, null),
                                             myPriority));
  }

  @Override
  protected boolean inspectionEnabledFor(RubyCallArgumentContext context) {
    return false;
  }
}
