package com.intellij.lang.puppet.project.meta;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PuppetMetadata {
  @Nullable
  String getName();

  /**
   * @return presentable name for puppet entity, e.g. some-module 1.5, someenv, Unknown module; should handle nullability in metadata
   * fixme add error mark using com.intellij.codeInsight.problems.WolfTheProblemSolverImpl
   */
  @NotNull
  String getPresentableName();
}
