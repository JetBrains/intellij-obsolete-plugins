package org.jetbrains.plugins.ruby.chef.lint.foodcritic;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

class FoodcriticResult {
  private final @NotNull Collection<FoodcriticProblem> myProblems;

  FoodcriticResult(final @NotNull Collection<FoodcriticProblem> problems) {
    myProblems = problems;
  }

  public @NotNull Collection<FoodcriticProblem> getProblems() {
    return myProblems;
  }
}
