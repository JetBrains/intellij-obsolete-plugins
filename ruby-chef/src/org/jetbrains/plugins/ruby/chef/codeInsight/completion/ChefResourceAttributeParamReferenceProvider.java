package org.jetbrains.plugins.ruby.chef.codeInsight.completion;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.ParamDefReference;
import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.matcher.MatchContext;
import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.matcher.MatchResult;
import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.matcher.ParamDefExpression;
import org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.matcher.ParamDefSeq;
import org.jetbrains.plugins.ruby.ruby.codeInsight.resolve.ResolveUtil;
import org.jetbrains.plugins.ruby.ruby.codeInsight.symbols.structure.Symbol;
import org.jetbrains.plugins.ruby.ruby.codeInsight.types.CoreTypes;
import org.jetbrains.plugins.ruby.ruby.lang.psi.RPsiElement;
import org.jetbrains.plugins.ruby.ruby.lang.psi.assoc.RAssoc;
import org.jetbrains.plugins.ruby.ruby.lang.psi.controlStructures.methods.RMethod;
import org.jetbrains.plugins.ruby.ruby.lang.psi.expressions.RArray;
import org.jetbrains.plugins.ruby.ruby.lang.psi.expressions.RArrayIndexing;
import org.jetbrains.plugins.ruby.ruby.lang.psi.expressions.RBinaryExpression;
import org.jetbrains.plugins.ruby.ruby.lang.psi.impl.variables.RConstantImpl;
import org.jetbrains.plugins.ruby.ruby.lang.psi.methodCall.RCall;
import org.jetbrains.plugins.ruby.ruby.lang.psi.variables.RConstant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.intellij.util.containers.ContainerUtil.ar;
import static org.jetbrains.plugins.ruby.ruby.codeInsight.paramDefs.ParamDefExpressionUtil.bool;

public final class ChefResourceAttributeParamReferenceProvider extends PsiReferenceProvider {
  public static final String SET_OR_RETURN = "set_or_return";
  public static final String KIND_OF = "kind_of";
  private static ChefResourceAttributeParamReferenceProvider ourInstance;

  private ChefResourceAttributeParamReferenceProvider() {
  }

  public static ChefResourceAttributeParamReferenceProvider getInstance() {
    if (ourInstance == null) {
      ourInstance = new ChefResourceAttributeParamReferenceProvider();
    }
    return ourInstance;
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    if (!(element instanceof RPsiElement)) {
      return PsiReference.EMPTY_ARRAY;
    }

    final RCall call = PsiTreeUtil.getParentOfType(element, RCall.class, false, RBinaryExpression.class);
    if (call == null) {
      return PsiReference.EMPTY_ARRAY;
    }

    return ar(new ParamDefReference(element, element, () -> match(element, call)));
  }

  public static @Nullable MatchResult match(final @NotNull PsiElement element, @Nullable RCall call) {
    if (call == null || call instanceof RArrayIndexing) {
      return null;
    }

    if (RConstantImpl.isDummyIdentifier(call.getPsiCommand())) {
      call = PsiTreeUtil.getParentOfType(PsiTreeUtil.prevLeaf(call.getPsiCommand()), RCall.class);
      if (call == null) {
        return null;
      }
    }

    final PsiElement psiCommand = call.getPsiCommand();
    Symbol symbol =
      ResolveUtil.resolveToSymbolWithCaching(psiCommand.getReference(), !ApplicationManager.getApplication().isUnitTestMode());
    if (symbol == null) return null;

    final PsiElement resourceMethod = symbol.getPsiElement();
    if (!(resourceMethod instanceof RMethod)) return null;

    final List<RPsiElement> setOrReturnCalls =
      ContainerUtil.filter(((RMethod)resourceMethod).getCompoundStatement().getStatements(),
                           element1 -> element1 instanceof RCall && SET_OR_RETURN.equals(((RCall)element1).getCommand()));

    if (setOrReturnCalls.isEmpty()) return null;
    final RPsiElement setOrReturnCall = setOrReturnCalls.get(0);
    if (!(setOrReturnCall instanceof RCall)) return null;

    ParamDefSeq paramDefSeq = null;
    final Collection<RAssoc> kindOfAssoc = PsiTreeUtil.findChildrenOfType(((RCall)setOrReturnCall).getCallArguments(), RAssoc.class);
    for (RAssoc assoc : kindOfAssoc) {
      if (!KIND_OF.equals(assoc.getKeyText())) continue;

      final RPsiElement argType = assoc.getValue();
      final ArrayList<String> typeNames = new ArrayList<>();
      if (argType instanceof RConstant) {
        typeNames.add(argType.getName());
      }
      else if (argType instanceof RArray) {
        final List<RPsiElement> argTypes = ((RArray)argType).getElements();
        for (RPsiElement type : argTypes) {
          if (type instanceof RConstant) typeNames.add(type.getName());
        }
      }

      final ArrayList<ParamDefExpression> convertable = new ArrayList<>();
      for (String typeName : typeNames) {
        if (CoreTypes.TrueClass.equals(typeName) || CoreTypes.FalseClass.equals(typeName)) {
          convertable.add(bool().toExpr());
        }
      }

      paramDefSeq = new ParamDefSeq(convertable.toArray(new ParamDefExpression[0]));
    }

    final ParamDefExpression expr = paramDefSeq;
    if (expr == null) {
      return null;
    }

    final MatchContext context = new MatchContext(element, call, call.getCallArguments().getFirstElement());
    return expr.match(context);
  }
}