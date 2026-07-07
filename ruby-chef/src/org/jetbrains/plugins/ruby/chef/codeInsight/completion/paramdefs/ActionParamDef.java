package org.jetbrains.plugins.ruby.chef.codeInsight.completion.paramdefs;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import com.intellij.util.Processor;
import icons.RubyIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.chef.symbols.ResourceSymbolProvider;
import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.LookupItemType;
import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.ParamDefBase;
import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.ParamDefImplUtil;
import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.RubyCallArgumentContext;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.SymbolUtil;
import org.jetbrains.plugins.ruby.ruby.lang.psi.RPsiElement;
import org.jetbrains.plugins.ruby.ruby.lang.psi.basicTypes.RSymbol;
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.classes.RClass;
import org.jetbrains.plugins.ruby.ruby.lang.psi.holders.RContainer;
import org.jetbrains.plugins.ruby.ruby.lang.psi.methodCall.RCall;
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.fields.RField;
import org.jetbrains.plugins.ruby.ruby.refactoring.common.RubySuperClassesCollector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class ActionParamDef extends ParamDefBase {
  public static final String ALLOWED_ACTIONS = "allowed_actions";

  @Override
  public void processAllVariants(@NotNull RubyCallArgumentContext context,
                                 @NotNull Consumer<? super LookupElement> elementsConsumer,
                                 @NotNull Predicate<String> lookupStringFilter) {
    processAllowedActions(context, symbol -> {
      if (lookupStringFilter.test(symbol.getValue())) {
        elementsConsumer.consume(
          ParamDefImplUtil.createSimpleLookupItem(symbol.getValue(), LookupItemType.Symbol, context.getArgumentElement(), RubyIcons.Ruby.Nodes.Symbol)
        );
      }
      return true;
    });
  }

  @Override
  public @Nullable PsiElement resolveReference(final @NotNull RubyCallArgumentContext context) {
    Ref<PsiElement> resultRef = Ref.create();

    final RPsiElement valueElement = context.getArgumentElement();
    final String valueElementName = valueElement.getName();
    processAllowedActions(context, symbol -> {
      if (valueElement instanceof RSymbol && valueElementName != null && valueElementName.equals(symbol.getName())) {
        resultRef.set(symbol);
        return false;
      }
      return true;
    });

    return resultRef.get();
  }

  private static void processAllowedActions(@NotNull RubyCallArgumentContext context, @NotNull Processor<RSymbol> symbolProcessor) {
    final RCall attributeCall = context.getCall();
    final RClass resourceContainer = ResourceSymbolProvider.findParentResourceClass(attributeCall);
    if (resourceContainer == null) return;

    Set<RContainer> classesSet = new HashSet<>(RubySuperClassesCollector.getSuperClasses(
      SymbolUtil.getSymbolByContainer(resourceContainer), resourceContainer, false).keySet());
    classesSet.add(resourceContainer);

    for (RContainer rContainer : classesSet) {
      if (!(rContainer instanceof RClass)) continue;

      for (RField allowedActionsField : ((RClass)rContainer).getFieldsDeclarations()) {
        if (!ALLOWED_ACTIONS.equals(allowedActionsField.getName())) {
          continue;
        }
        final RCall allowedActionsCall = PsiTreeUtil.getParentOfType(allowedActionsField, RCall.class);
        if (allowedActionsCall == null) return;

        final List<RPsiElement> allowedActionsSymbols = allowedActionsCall.getCallArguments().getElements();
        for (RPsiElement actionSymbol : allowedActionsSymbols) {
          if (!(actionSymbol instanceof RSymbol)) return;

          if (!symbolProcessor.process((RSymbol)actionSymbol)) {
            return;
          }
        }
      }
    }
  }
}
