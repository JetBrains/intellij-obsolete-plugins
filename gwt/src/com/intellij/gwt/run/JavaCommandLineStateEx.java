package com.intellij.gwt.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaCommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import org.jetbrains.annotations.NotNull;

public abstract class JavaCommandLineStateEx extends JavaCommandLineState {
  protected JavaCommandLineStateEx(@NotNull ExecutionEnvironment environment) {
    super(environment);
  }

  @Override
  protected final JavaParameters createJavaParameters() throws ExecutionException {
    try {
      return createJavaParametersImpl();
    }
    catch (RuntimeException | Error e) {
      onException();
      throw e;
    }
  }

  @Override
  public final @NotNull ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
    try {
      return executeImpl(executor, runner);
    }
    catch (RuntimeException | Error e) {
      onException();
      throw e;
    }
  }

  protected abstract JavaParameters createJavaParametersImpl() throws ExecutionException;

  protected abstract ExecutionResult executeImpl(Executor executor, ProgramRunner<?> runner) throws ExecutionException;

  protected abstract void onException();
}
