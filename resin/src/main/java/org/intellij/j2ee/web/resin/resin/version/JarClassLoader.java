package org.intellij.j2ee.web.resin.resin.version;

/**
 * Jar classloader class
 * From <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip70.html">http://www.javaworld.com/javaworld/javatips/jw-javatip70.html</a>
 *
 * @see ResinVersionDetector
 */
class JarClassLoader extends MultiClassLoader {
  private final JarResources jarResources;

  JarClassLoader(String jarName) {
    // Create the JarResource and suck in the jar file.
    jarResources = new JarResources(jarName, false);
  }

  @Override
  protected byte[] loadClassBytes(String className) {
    // Support the MultiClassLoader's class name munging facility.
    className = formatClassName(className);
    // Attempt to get the class data from the JarResource.
    return (jarResources.getResource(className));
  }
}
