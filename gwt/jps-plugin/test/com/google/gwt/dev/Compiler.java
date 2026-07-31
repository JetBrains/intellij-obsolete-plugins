package com.google.gwt.dev;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Compiler {
  public static void main(String[] args) throws IOException {
    String outputPath = null;
    List<String> modules = new ArrayList<>();
    int i = 0;
    while (i < args.length) {
      String arg = args[i];
      if (arg.equals("-war")) {
        i++;
        outputPath = args[i];
      }
      else if (arg.startsWith("-")) {
        i++;
      }
      else {
        modules.add(arg);
      }
      i++;
    }
    if (outputPath == null) {
      System.err.println("Output dir not specified");
      System.exit(1);
    }
    File outputDir = new File(outputPath);
    for (String module : modules) {
      File moduleOutputDir = new File(outputDir, module);
      moduleOutputDir.mkdirs();
      System.out.println("Compiling GWT module " + module + " to " + moduleOutputDir.getAbsolutePath());
      new File(moduleOutputDir, module + ".html").createNewFile();
    }
  }
}
