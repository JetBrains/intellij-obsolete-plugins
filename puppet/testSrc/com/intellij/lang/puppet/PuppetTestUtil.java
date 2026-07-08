package com.intellij.lang.puppet;

import com.intellij.lang.puppet.project.PuppetTestCase;
import com.intellij.openapi.application.PathManager;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.junit.internal.MethodSorter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public final class PuppetTestUtil {
  public static String getTestDataPath() {
    return PathManager.getHomePath() + "/plugins/puppet/testdata/";
  }

  public static PuppetLanguage.Version @NotNull [] getVersions(@NotNull Class<? extends TestCase> clazz, @NotNull String testName) {
    try {
      final Method method = clazz.getMethod(testName);
      final OnVersion versions = method.getAnnotation(OnVersion.class);
      if (versions != null) {
        return versions.value();
      }
    }
    catch (NoSuchMethodException e) {
      // ignore
    }

    final OnVersion classDefaults = clazz.getAnnotation(OnVersion.class);
    if (classDefaults != null) {
      return classDefaults.value();
    }

    return new PuppetLanguage.Version[0];
  }

  public static TestSuite createTestSuiteForVersions(@NotNull Class<? extends PuppetTestCase> clazz) {

    final HashMap<String, TestSuite> versionToSuite = new HashMap<>();

    try {
      for (Method method : MethodSorter.getDeclaredMethods(clazz)) {
        if (!isPublicTestMethod(method)) {
          continue;
        }

        final PuppetLanguage.Version[] versions = getVersions(clazz, method.getName());

        for (PuppetLanguage.Version version : versions) {
          final String suiteName = version + "#" + clazz.getName();
          final TestSuite suiteToAdd;

          if (versionToSuite.containsKey(suiteName)) {
            suiteToAdd = versionToSuite.get(suiteName);
          }
          else {
            suiteToAdd = new TestSuite(suiteName);
            versionToSuite.put(suiteName, suiteToAdd);
          }

          final PuppetTestCase result = clazz.newInstance();
          result.setName(method.getName() + "#" + version);
          result.setLanguageVersion(version);
          suiteToAdd.addTest(result);
        }
      }
    }
    catch (InstantiationException | IllegalAccessException e) {
      e.printStackTrace();
    }

    final TestSuite suite = new TestSuite(clazz.getSimpleName());
    for (TestSuite testSuite : versionToSuite.values()) {
      suite.addTest(testSuite);
    }

    return suite;
  }

  private static boolean isPublicTestMethod(Method m) {
    return isTestMethod(m) && Modifier.isPublic(m.getModifiers());
  }

  private static boolean isTestMethod(Method m) {
    return m.getParameterTypes().length == 0 &&
           m.getName().startsWith("test") &&
           m.getReturnType().equals(Void.TYPE);
  }
}
