package com.intellij.play.utils.routes;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouterLineDescriptor {
  static private final Pattern routePattern =
    Pattern.compile("^(GET|POST|PUT|DELETE|OPTIONS|HEAD|WS|\\*)[((]?([^)]*)\\)?\\s+(.*/[^\\s]*)\\s+([^\\s(]+)(.+)?(\\s*)$");

  private Pair<String, Integer> myMethod;
  private Pair<String, Integer> myAction;
  private Pair<String, Integer> myPath;
  private final String myLine;
  private final int myStartOffset;


  public RouterLineDescriptor(@NotNull String line,
                              int startOffset) {
    myLine = line;
    myStartOffset = startOffset;
    
    Matcher matcher = routePattern.matcher(line);
    if (matcher.matches()) {
      String action = matcher.group(4); // ([^\\s(]+)
       
      if (!StringUtil.isEmptyOrSpaces(action)) {
        if (action.startsWith("module:")) {
          // todo later
        }
        else {
          myAction = Pair.create(action, startOffset + line.indexOf(action));

          String method = matcher.group(1);    // (GET|POST|PUT|DELETE|OPTIONS|HEAD|WS|\\*)
          if (!StringUtil.isEmptyOrSpaces(method)) {
            myMethod = Pair.create(action, startOffset + line.indexOf(method));
          }
          String path = matcher.group(3);  //  (.*/[^\\s]*)
          if (!StringUtil.isEmptyOrSpaces(path)) {
            myPath = Pair.create(action, startOffset + line.indexOf(path));
          }
          // String spaces = matcher.group(2);    // ([^)]*)
          // String params = matcher.group(5);     // (.+)
          // String headers = matcher.group(6);     // (\\s*)
        }
      } else {
        // Logger.error("Invalid route definition : %s", line);
      }
    }
  }

  public String getLine() {
    return myLine;
  }

  public int getStartOffset() {
    return myStartOffset;
  }

  @Nullable
  public Pair<String, Integer> getMethod() {
    return myMethod;
  }

  @Nullable
  public Pair<String, Integer> getAction() {
    return myAction;
  }

  @Nullable
  public Pair<String, Integer> getPath() {
    return myPath;
  }
}
