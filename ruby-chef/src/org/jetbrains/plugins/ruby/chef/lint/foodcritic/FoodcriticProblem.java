package org.jetbrains.plugins.ruby.chef.lint.foodcritic;

import org.jetbrains.annotations.NotNull;

class FoodcriticProblem {
  final @NotNull String path;
  final @NotNull String description;
  final int lineNumber;

  FoodcriticProblem(final @NotNull String path, final @NotNull String description, int lineNumber) {
    this.lineNumber = lineNumber;
    this.description = description;
    this.path = path;
  }
}
