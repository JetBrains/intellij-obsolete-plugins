package org.jetbrains.jps.gwt.build.common;

import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

public class DelegatingOutputLineReader extends OutputLineReader {

  private final Consumer<? super String> myLineConsumer;

  public DelegatingOutputLineReader(Consumer<? super String> lineConsumer) {
    myLineConsumer = lineConsumer;
  }

  @Override
  protected void parseLine(@NotNull String line) {
    myLineConsumer.consume(line);
  }
}
