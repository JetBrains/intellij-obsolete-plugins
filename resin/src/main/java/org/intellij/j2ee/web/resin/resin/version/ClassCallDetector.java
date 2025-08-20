package org.intellij.j2ee.web.resin.resin.version;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import org.intellij.j2ee.web.resin.ResinBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * This class is able to detect the version of the selected resin home
 */
final class ClassCallDetector {

  private static final Logger LOG = Logger.getInstance(ClassCallDetector.class);

  @NonNls
  private static final String RESIN_VERSION_CLASS = "com.caucho.Version";
  @NonNls
  private static final String RESIN_XDEBUG_CLASS = "com.caucho.log.LogManagerImpl";
  @NonNls
  private static final String RESIN_JMX_CLASS = "com.caucho.jmx.MBeanServerBuilderImpl";
  @NonNls
  private static final String RESIN_VERSION_CLASS_ATT_NAME = "VERSION";

  private ClassCallDetector() {

  }

  /**
   * This method will try to retrieve the version of the selected Resin by calling the class com.caucho.Version
   *
   * @param resinHome the resin home
   * @return ResinVersion representing the detected version. null if it was unable to detect it
   */
  @Nullable
  public static ResinVersion getResinVersion(File resinHome) {
    //java -cp "<resinHome>/lib/resin.jar" com.caucho.Version
    try {

      File resinJar = new File(resinHome, FileUtil.toSystemDependentName("lib/resin.jar"));
      if (!resinJar.exists()) {
        return null;
      }

      //Extract version from class
      JarClassLoader loader = new JarClassLoader(resinJar.getAbsolutePath());

      String version = getResinVersionFromClass(loader);
      if (version == null) {
        version = getResinVersionFromManifest(resinJar);
      }
      String startupClass = StartupClassFinder.getStartupClassForVersion(version);

      boolean allowDebug = hasClass(loader, RESIN_XDEBUG_CLASS);
      boolean allowJmx = hasClass(loader, RESIN_JMX_CLASS);

      return startupClass == null ? null :
             new GenericResinVersion(ResinBundle.message("resin.version.prefix", version), version, startupClass, allowDebug, allowJmx);
    }
    catch (IOException e) {
      LOG.error(e);
      return null;
    }
  }

  private static String getResinVersionFromClass(JarClassLoader loader) {
    try {
      Class versionClass = loader.loadClass(RESIN_VERSION_CLASS);
      Field field = versionClass.getDeclaredField(RESIN_VERSION_CLASS_ATT_NAME);
      return field.get(null).toString();
    }
    catch (ClassNotFoundException e) {
      // LOG.error(e); This is valid for version 5+
      return null;
    }
    catch (IllegalAccessException | NoSuchFieldException e) {
      LOG.error(e);
      return null;
    }
  }

  private static String getResinVersionFromManifest(File jarFile) throws IOException {
    try (JarFile jar = new JarFile(jarFile)) {
      Attributes attributes = jar.getManifest().getMainAttributes();
      if (attributes == null) {
        return null;
      }
      return attributes.getValue("Implementation-Version");
    }
  }


  private static boolean hasClass(JarClassLoader loader, @NonNls String className) {
    try {
      return loader.loadClass(className) != null;
    }
    catch (ClassNotFoundException ignore) {
      return false;
    }
  }
}