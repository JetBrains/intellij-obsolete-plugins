package org.jetbrains.plugins.ruby.chef.lint.foodcritic;

import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class FoodcriticCookbookCache {
  private MultiMap<String, FoodcriticProblem> warnings;

  public @Nullable FoodcriticResult getCachedProblem(final @NotNull FoodcriticState state) {
    final String relativePath = VfsUtilCore.getRelativePath(state.file.getVirtualFile(), state.cookbook.getVirtualFile());
    Collection<FoodcriticProblem> problems = null;
    if (relativePath != null) {

      if (warnings != null) {
        problems = warnings.get(relativePath);
      }
    }

    return problems != null ? new FoodcriticResult(problems) : null;
  }

  public boolean isInvalidated() {
    return warnings == null;
  }

  public void cacheValue(final @NotNull FoodcriticProblem problem) {
    if (warnings != null) {
      warnings.putValue(problem.path, problem);
    }
  }

  public void init() {
    warnings = new MultiMap<>();
  }
}
