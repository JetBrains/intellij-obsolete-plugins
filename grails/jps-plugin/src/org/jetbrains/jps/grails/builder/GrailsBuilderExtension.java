// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.jps.grails.builder;

import com.intellij.openapi.application.PathManager;
import com.intellij.util.text.VersionComparatorUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.groovy.grails.compiler.GrailsCompilerRtMarker;
import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.groovy.GroovyBuilderExtension;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.model.module.JpsDependencyElement;
import org.jetbrains.jps.model.module.JpsLibraryDependency;
import org.jetbrains.jps.model.module.JpsModule;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GrailsBuilderExtension implements GroovyBuilderExtension {
  public static final Pattern CORE_JAR_PATTERN = Pattern.compile("grails-core-(\\d[^-]*(?:-SNAPSHOT)?)\\.jar");

  @Override
  public @NotNull Collection<String> getCompilationClassPath(@NotNull CompileContext context, @NotNull ModuleChunk chunk) {
    for (JpsModule module : chunk.getModules()) {
      if (shouldInjectGrails(module)) {
        return Collections.singleton(PathManager.getJarPathForClass(GrailsCompilerRtMarker.class));
      }
    }

    return Collections.emptyList();
  }

  private static boolean shouldInjectGrails(@NotNull JpsModule module) {
    final String version = getGrailsVersion(module);
    return shouldInjectGrails(version);
  }

  @Contract("null -> false")
  private static boolean shouldInjectGrails(@Nullable String version) {
    return version != null && VersionComparatorUtil.compare(version, "3.0") < 0;
  }

  private static @Nullable String getGrailsVersion(@NotNull JpsModule module) {
    String foundVersion = null;

    for (JpsDependencyElement dependencyElement : module.getDependenciesList().getDependencies()) {
      if (!(dependencyElement instanceof JpsLibraryDependency)) continue;
      final JpsLibrary library = ((JpsLibraryDependency)dependencyElement).getLibrary();
      if (library == null) continue;

      for (File file : library.getFiles(JpsOrderRootType.COMPILED)) {
        final Matcher matcher = CORE_JAR_PATTERN.matcher(file.getName());
        if (!matcher.matches()) continue;
        if (foundVersion == null) {
          foundVersion = matcher.group(1);
        }
        else if (VersionComparatorUtil.compare(foundVersion, matcher.group(1)) != 0) {
          // if another version is present in classpath
          // return null to avoid compilation issues
          return null;
        }
      }
    }

    return foundVersion;
  }

  @Override
  public @NotNull Collection<String> getCompilationUnitPatchers(@NotNull CompileContext context, @NotNull ModuleChunk chunk) {
    Set<String> res = new HashSet<>();

    for (JpsModule module : chunk.getModules()) {
      String grailsVersion = getGrailsVersion(module);
      if (!shouldInjectGrails(grailsVersion)) continue;

      res.add("org.jetbrains.groovy.grails.compiler.GrailsJUnitPatcher");

      if (grailsVersion.compareTo("1.2") < 0) {
        res.add("org.jetbrains.groovy.grails.compiler.GrailsDomainClassPatcher");
      }
      else if (grailsVersion.compareTo("2.0") >= 0) {
        res.add("org.jetbrains.groovy.grails.compiler.EmptyGrailsAwarePatcher");
        res.add("org.jetbrains.groovy.grails.compiler.Grails2_0_JUnitPatcher");
      }
    }

    return res;
  }
}
