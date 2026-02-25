// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.config.AbstractConfigUtils;
import org.jetbrains.plugins.groovy.util.LibrariesUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GrailsConfigUtils extends AbstractConfigUtils {

  private static final Logger LOG = Logger.getInstance(GrailsConfigUtils.class);

  public static final Pattern CORE_JAR_PATTERN = Pattern.compile("grails-core-(?<version>\\d[^-]*(?:-SNAPSHOT)?)\\.jar");
  public static final Condition<Module> IS_GRAILS3_MODULE = module -> GrailsStructure.isVersionAtLeast("3.0", module);

  private static final @NonNls String GRAILS_MANIFEST_MF = "META-INF/GRAILS-MANIFEST.MF";
  static final @NonNls String DIST = "/dist";
  static final @NonNls String LIB_DIR = "/lib";

  private static final GrailsConfigUtils ourGrailsConfigUtils = new GrailsConfigUtils();

  private GrailsConfigUtils() {}

  public static GrailsConfigUtils getInstance() {
    return ourGrailsConfigUtils;
  }

  @Override
  public boolean isSDKLibrary(Library library) {
    if (library == null) return false;

    return findGrailsJar(library.getFiles(OrderRootType.CLASSES)) != null;
  }

  private static @Nullable VirtualFile findGrailsJar(VirtualFile[] files) {
    for (VirtualFile file : files) {
      if (isGrailsCoreJar(file.getNameSequence())) {
        return VfsUtil.getLocalFile(file);
      }
    }
    return null;
  }

  @Override
  public boolean isSDKHome(VirtualFile file) {
    if (file != null && file.isDirectory()) {
      final String path = file.getPath();
      if (LibrariesUtil.getFilesInDirectoryByPattern(path + "/lib", CORE_JAR_PATTERN).length > 0) {
        return true;
      }
      if (LibrariesUtil.getFilesInDirectoryByPattern(path + "/embeddable", CORE_JAR_PATTERN).length > 0) {
        return true;
      }
      if (file.findFileByRelativePath("bin/grails") != null) {
        return true;
      }
    }
    return false;
  }

  @Override
  public @Nullable String getSDKVersionOrNull(@NotNull String path) {
    String grailsJarVersion = getSDKJarVersion(path + DIST, CORE_JAR_PATTERN, AbstractConfigUtils.MANIFEST_PATH);
    if (grailsJarVersion == null) {
      // check for versions <= 0.6
      return getSDKJarVersion(path + DIST, CORE_JAR_PATTERN, GRAILS_MANIFEST_MF);
    }
    return grailsJarVersion;
  }

  public static @Nullable String getGrailsVersion(@Nullable Module module) {
    if (module == null) {
      return null;
    }
    return getGrailsVersion(OrderEnumerator.orderEntries(module).getAllLibrariesAndSdkClassesRoots());
  }

  public static @Nullable @NlsSafe String getGrailsVersion(VirtualFile @NotNull [] files) {
    for (VirtualFile file : files) {
      Matcher m = CORE_JAR_PATTERN.matcher(file.getName());
      if (m.matches()) {
        return m.group(1);
      }
    }

    return null;
  }

  public static @Nullable VirtualFile getSDKInstallPath(@Nullable Module module) {
    if (module == null) return null;
    return getGrailsLibraryHome(OrderEnumerator.orderEntries(module).getAllLibrariesAndSdkClassesRoots());
  }

  public static @Nullable VirtualFile getGrailsLibraryHome(VirtualFile[] files) {
    for (VirtualFile file : files) {
      if (isGrailsCoreJar(file.getNameSequence())) {
        VirtualFile grailsJar = VfsUtil.getLocalFile(file);

        final VirtualFile parent = grailsJar.getParent();
        if (parent != null) {
          if (ApplicationManager.getApplication().isUnitTestMode()) {
            return parent;
          }
          else {
            if (Comparing.equal("dist", parent.getNameSequence())) {
              return parent.getParent();
            }
          }
        }
      }
    }

    return null;
  }

  public static boolean isAtLeastGrails(Module module, String version) {
    final String currentVersion = getGrailsVersion(module);
    return currentVersion != null && currentVersion.compareTo(version) >= 0;
  }

  public static boolean isGrailsLessThan(Module module, String version) {
    final String currentVersion = getGrailsVersion(module);
    return currentVersion != null && currentVersion.compareTo(version) < 0;
  }

  public static boolean isAtLeastGrails1_1(Module module) {
    return isAtLeastGrails(module, "1.1");
  }

  public static boolean isAtLeastGrails1_2(Module module) {
    return isAtLeastGrails(module, "1.2");
  }

  public static boolean isAtLeastGrails1_3(Module module) {
    return isAtLeastGrails(module, "1.3");
  }

  public static boolean isAtLeastGrails1_4(Module module) {
    return isAtLeastGrails(module, "1.4");
  }

  public static boolean isAtLeastGrails2_0(Module module) {
    return isAtLeastGrails(module, "2.0");
  }

  public static boolean isAtLeastGrails2_1(Module module) {
    return isAtLeastGrails(module, "2.1");
  }

  public static boolean isGrailsCoreJar(CharSequence fileName) {
    return CORE_JAR_PATTERN.matcher(fileName).matches();
  }
}
