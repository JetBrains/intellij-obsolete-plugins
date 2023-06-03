package org.intellij.j2ee.web.resin.resin.version;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A simple test class loader capable of loading from
 * multiple sources, such as local files or a URL.
 * <p/>
 * This class is derived from an article by Chuck McManis
 * http://www.javaworld.com/javaworld/jw-10-1996/indepth.src.html
 * with large modifications.
 * <p/>
 * Note that this has been updated to use the non-deprecated version of
 * defineClass() -- JDM.
 *
 * @author Jack Harich - 8/18/97
 * @author John D. Mitchell - 99.03.04
 */
abstract class MultiClassLoader extends ClassLoader {

  //---------- Fields --------------------------------------
  private final Map<String, Class> classes = new ConcurrentHashMap<>();
  private char classNameReplacementChar;

  protected boolean monitorOn = false;
  protected boolean sourceMonitorOn = true;

  //---------- Initialization ------------------------------
  MultiClassLoader() {
  }

  //---------- Superclass Overrides ------------------------

  /**
   * This is a simple version for external clients since they
   * will always want the class resolved before it is returned
   * to them.
   */
  @Override
  public Class loadClass(String className) throws ClassNotFoundException {
    return (loadClass(className, true));
  }

  //---------- Abstract Implementation ---------------------
  @Override
  public synchronized Class loadClass(String className,
                                      boolean resolveIt) throws ClassNotFoundException {

    Class result;
    byte[] classBytes;
    monitor(">> MultiClassLoader.loadClass(" + className + ", " + resolveIt + ")");

    //----- Check our local cache of classes
    result = classes.get(className);
    if (result != null) {
      monitor(">> returning cached result.");
      return result;
    }

    //----- Check with the primordial class loader
    try {
      result = super.findSystemClass(className);
      monitor(">> returning system class (in CLASSPATH).");
      return result;
    }
    catch (ClassNotFoundException e) {
      monitor(">> Not a system class.");
    }

    //----- Try to load it from preferred source
    // Note loadClassBytes() is an abstract method
    classBytes = loadClassBytes(className);
    if (classBytes == null) {
      throw new ClassNotFoundException();
    }

    //----- Define it (parse the class file)
    result = defineClass(className, classBytes, 0, classBytes.length);
    if (result == null) {
      throw new ClassFormatError();
    }

    //----- Resolve if necessary
    if (resolveIt) resolveClass(result);

    // Done
    classes.put(className, result);
    monitor(">> Returning newly loaded class.");
    return result;
  }

  //---------- Public Methods ------------------------------

  /**
   * This optional call allows a class name such as
   * "COM.test.Hello" to be changed to "COM_test_Hello",
   * which is useful for storing classes from different
   * packages in the same retrival directory.
   * In the above example the char would be '_'.
   *
   * @param replacement the replacement char
   */
  public void setClassNameReplacementChar(char replacement) {
    classNameReplacementChar = replacement;
  }

  //---------- Protected Methods ---------------------------
  protected abstract byte[] loadClassBytes(String className);

  protected String formatClassName(String className) {
    if (classNameReplacementChar == '\u0000') {
      // '/' is used to map the package to the path
      return className.replace('.', '/') + ".class";
    }
    else {
      // Replace '.' with custom char, such as '_'
      return className.replace('.',
                               classNameReplacementChar) + ".class";
    }
  }

  protected void monitor(String text) {
    if (monitorOn) print(text);
  }

  //--- Std
  protected static void print(String text) {
    System.out.println(text);
  }
} // End class
