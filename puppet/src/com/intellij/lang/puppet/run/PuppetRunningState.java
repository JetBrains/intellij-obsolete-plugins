package com.intellij.lang.puppet.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import org.jetbrains.annotations.NotNull;

/**
 * @author Anna Bulenkova
 */
public class PuppetRunningState extends CommandLineState {
  protected PuppetRunningState(final ExecutionEnvironment environment, final PuppetRunnerParameters parameters) {
    super(environment);
  }

  @Override
  protected @NotNull ProcessHandler startProcess() throws ExecutionException {
    return null;
  }
}
