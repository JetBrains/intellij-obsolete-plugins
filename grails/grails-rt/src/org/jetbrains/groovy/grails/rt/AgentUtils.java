package org.jetbrains.groovy.grails.rt;

public final class AgentUtils {

  public static boolean isGrailsClassLoader(ClassLoader loader) {
    String loaderName = loader.getClass().getName();
    return loaderName.equals("org.codehaus.groovy.grails.cli.support.GrailsRootLoader")
           || loaderName.equals("org.grails.launcher.RootLoader");
  }

}
