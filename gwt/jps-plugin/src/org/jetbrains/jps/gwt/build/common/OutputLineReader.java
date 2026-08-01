package org.jetbrains.jps.gwt.build.common;

import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;

public abstract class OutputLineReader {
  private final StringBuilder myBuffer = new StringBuilder();

  public void parseOutput(final String text) {
    myBuffer.append(text);
    int start = 0;
    while (true) {
      int lineEnd1 = myBuffer.indexOf("\n", start);
      int lineEnd2 = myBuffer.indexOf("\r", start);
      if (lineEnd1 == -1 && lineEnd2 == -1) break;

      int lineEnd = lineEnd1 == -1 ? lineEnd2 : lineEnd2 == -1 ? lineEnd1 : Math.min(lineEnd1, lineEnd2);
      parseLine(myBuffer.substring(start, lineEnd).trim());
      start = lineEnd + 1;
    }

    myBuffer.delete(0, start);
  }

  protected abstract void parseLine(@NlsSafe @NotNull String line);
}
