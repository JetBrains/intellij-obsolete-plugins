// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gradle;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

// requires locally installed Java 8 & 11
public class GradleGrailsImportingTest extends GradleImportingTestCase {
  @Parameterized.Parameter(1) public String grailsVersion;

  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  @Parameterized.Parameters(name = "with Gradle-{0}, Grails-{1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
      new String[]{"4.6", "3.3.1"},
      new String[]{"5.6.2", "4.1.0"},
      new String[]{"6.8.3", "5.3.0"}
      // should be 7.6 -> 5.3.0, but Grails Gradle plugin fails
      // strict compatibility check that is enabled in the test
    );
  }

  @Test
  public void importBasicGrailsProject() throws IOException {
    createGrailsStdFolders();
    createProjectSubFile("gradle.properties", "grailsVersion=" + grailsVersion);

    importProject(createBuildScriptBuilder().withBuildScriptMavenCentral().withMavenCentral()
                    .addBuildScriptPostfix("""
                                             repositories {
                                                 maven { url "https://repo.grails.org/grails/core" }
                                             }
                                             dependencies {
                                                classpath "org.grails:grails-gradle-plugin:${grailsVersion}"
                                             }
                                             """.replace("${grailsVersion}", grailsVersion))
                    .addPostfix("""
                                  version "0.1"
                                  group "myapp"
                                  apply plugin:"war"
                                  apply plugin:"org.grails.grails-web"
                                  apply plugin:"org.grails.grails-gsp"
                                  
                                  repositories {
                                      maven { url "https://repo.grails.org/grails/core" }
                                  }
                                  """).generate());

    Module module = getModule("project");
    VirtualFile appRoot = GrailsFramework.getInstance().findAppRoot(module);
    TestCase.assertNotNull(appRoot);
  }

  private void createGrailsStdFolders() throws IOException {
    createProjectSubDirs("grails-app/conf/hibernate", "grails-app/controllers", "grails-app/domain", "grails-app/i18n",
                         "grails-app/services", "grails-app/taglib", "grails-app/utils", "grails-app/views/layouts", "lib", "src/groovy",
                         "src/java", "test/unit", "test/integration", "web-app/WEB-INF");
  }
}
