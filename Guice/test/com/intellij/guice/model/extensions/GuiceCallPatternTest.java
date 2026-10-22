package com.intellij.guice.model.extensions;

import com.intellij.guice.GuiceTestBase;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UastContextKt;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

/**
 * Checks the matching semantics of {@link GuiceCallPattern} through {@link GuiceCallMatcher}.
 */
public class GuiceCallPatternTest extends GuiceTestBase {

  private static final String FIXTURE = """
    class Builder {
      static Builder newBinder(Object o) { return null; }
      Builder to(Class<?> c) { return this; }
    }
    class Other {}
    class Wrap {
      static Builder newBinder(Object o) { return null; }
      static Other newOther(Object o) { return null; }
    }
    class Use {
      void m(Builder b) {
        Builder.newBinder(1);
        Wrap.newBinder(2);
        Wrap.newOther(3);
        b.to(String.class);
      }
    }
    """;

  private @NotNull List<UCallExpression> fixtureCalls() {
    PsiFile file = myFixture.configureByText("Use.java", FIXTURE);
    List<UCallExpression> calls = new ArrayList<>();
    for (PsiMethodCallExpression psiCall : PsiTreeUtil.findChildrenOfType(file, PsiMethodCallExpression.class)) {
      UCallExpression call = UastContextKt.toUElement(psiCall, UCallExpression.class);
      if (call != null) calls.add(call);
    }
    assertThat(calls).hasSize(4);
    return calls;
  }

  private static @NotNull Set<String> matching(@NotNull GuiceCallPattern pattern, @NotNull List<UCallExpression> calls) {
    GuiceCallMatcher<Runnable> matcher = GuiceCallMatcher.<Runnable>builder(false).add(pattern, () -> {}).build();
    Set<String> matched = new LinkedHashSet<>();
    for (UCallExpression call : calls) {
      String text = call.getSourcePsi().getText();
      matcher.process(call, true, matched, (handler, c, qualifiedName, containingClass, sink) -> {
        sink.add(text);
        return true;
      });
    }
    return matched;
  }

  public void testConstraintKindsAreConjunctive() {
    List<UCallExpression> calls = fixtureCalls();
    GuiceCallPattern ownerAndReturn = GuiceCallPattern.named("newBinder", "newOther")
      .withOwnerClass("Builder")
      .withReturnInheritor("Builder");
    assertThat(matching(ownerAndReturn, calls)).containsExactly("Builder.newBinder(1)");

    GuiceCallPattern contradiction = GuiceCallPattern.named("newBinder", "newOther")
      .withOwnerClass("Builder")
      .withReturnInheritor("Other");
    assertThat(matching(contradiction, calls)).isEmpty();
  }

  public void testAnyOfMatchesWhenOneAlternativeMatches() {
    List<UCallExpression> calls = fixtureCalls();
    GuiceCallPattern names = GuiceCallPattern.named("newBinder", "newOther");
    GuiceCallPattern either = GuiceCallPattern.anyOf(
      names.withOwnerClass("Builder"),
      names.withReturnInheritor("Other"));
    assertThat(matching(either, calls)).containsExactly("Builder.newBinder(1)", "Wrap.newOther(3)");

    // A constraint of another kind added after anyOf applies conjunctively to every alternative.
    GuiceCallPattern byOwner = GuiceCallPattern.anyOf(names.withOwnerClass("Builder"), names.withOwnerClass("Wrap"));
    assertThat(matching(byOwner, calls)).containsExactly("Builder.newBinder(1)", "Wrap.newBinder(2)", "Wrap.newOther(3)");
    assertThat(matching(byOwner.withReturnInheritor("Other"), calls)).containsExactly("Wrap.newOther(3)");
  }

  public void testForBinderMatchesFactoryOnBinderOrWrapperReturningBinder() {
    List<UCallExpression> calls = fixtureCalls();
    GuiceCallPattern binder = GuiceCallPattern.named("newBinder", "newOther").forBinder("Builder");
    assertThat(matching(binder, calls)).containsExactly("Builder.newBinder(1)", "Wrap.newBinder(2)");
  }

  public void testForBindingBuilderMatchesOwnerOrReceiver() {
    List<UCallExpression> calls = fixtureCalls();
    assertThat(matching(GuiceCallPattern.named("to").forBindingBuilder("Builder"), calls))
      .containsExactly("b.to(String.class)");
    assertThat(matching(GuiceCallPattern.named("to").forBindingBuilder("Other"), calls)).isEmpty();
  }

  public void testDuplicateMethodNamesAreTolerated() {
    assertThat(GuiceCallPattern.named("bind", "bind").getMethodNames()).containsExactly("bind");
  }
}
