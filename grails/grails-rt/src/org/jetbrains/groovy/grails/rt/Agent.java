package org.jetbrains.groovy.grails.rt;

import java.lang.instrument.Instrumentation;

public final class Agent {

  private static boolean initialized;

  public static final String DEBUG_KIND_FILE = "idea.grails.kind.file";

  public static void premain(String options, Instrumentation inst) {
    // Handle duplicate agents
    if (initialized) {
      return;
    }
    initialized = true;

    inst.addTransformer(new AddAgentJarToClassPathTransformer());

    String fileName = System.getProperty(DEBUG_KIND_FILE);
    if (fileName != null) {
      inst.addTransformer(new ForkListenerTransformer(fileName));
    }
  }

}
