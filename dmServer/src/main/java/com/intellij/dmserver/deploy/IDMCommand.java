package com.intellij.dmserver.deploy;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface IDMCommand<T> {
  @Nullable
  T execute() throws IOException, TimeoutException, ExecutionException;
}
