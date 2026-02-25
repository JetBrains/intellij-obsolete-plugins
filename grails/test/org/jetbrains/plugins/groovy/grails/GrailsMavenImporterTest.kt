/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.groovy.grails

import com.intellij.facet.FacetManager
import com.intellij.javaee.web.facet.WebFacet
import com.intellij.maven.testFramework.MavenMultiVersionImportingTestCase
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.config.GrailsFramework
import org.junit.Test
import java.util.Arrays

class GrailsMavenImporterTest : MavenMultiVersionImportingTestCase() {
  private fun createGrailsStdFolders() {
    createProjectSubDirs("grails-app/conf/hibernate",
                         "grails-app/controllers",
                         "grails-app/domain",
                         "grails-app/i18n",
                         "grails-app/services",
                         "grails-app/taglib",
                         "grails-app/utils",
                         "grails-app/views/layouts",
                         "lib",
                         "src/groovy",
                         "src/java",
                         "test/unit",
                         "test/integration",
                         "web-app/WEB-INF")
  }

  @Test
  fun testImportGrailsProject() {
    createStdProjectFolders()
    createGrailsStdFolders()

    importProject("  <groupId>test</groupId>" +
                  "  <artifactId>project</artifactId>" +
                  "  <packaging>war</packaging>" +
                  "  <version>1</version>" +
                  "  <dependencies>" +
                  "    <dependency>" +
                  "      <groupId>org.grails</groupId>" +
                  "      <artifactId>grails-crud</artifactId>" +
                  "      <version>1.1</version>" +
                  "    </dependency>" +
                  "    <dependency>" +
                  "      <groupId>org.grails</groupId>" +
                  "      <artifactId>grails-gorm</artifactId>" +
                  "      <version>1.1</version>" +
                  "    </dependency>" +
                  "  </dependencies>" +
                  "  <build>" +
                  "    <pluginManagement />" +
                  "    <plugins>" +
                  "      <plugin>" +
                  "        <groupId>org.grails</groupId>" +
                  "        <artifactId>grails-maven-plugin</artifactId>" +
                  "        <version>1.0</version>" +
                  "      </plugin>" +
                  "    </plugins>" +
                  "  </build>")

    assertModules("project")

    assertSources("project",
                  "grails-app/controllers",
                  "grails-app/domain",
                  "grails-app/i18n",
                  "grails-app/services",
                  "grails-app/taglib",
                  "grails-app/utils",
                  "grails-app/jobs",
                  "grails-app/realms",
                  "src/groovy",
                  "src/java",
                  "src/main/java",
                  "src/gwt",
                  "src/scala")
    assertDefaultResources("project", "grails-app/resources")

    assertTestSources("project",
                      "src/test/java",
                      "test/integration",
                      "test/functional",
                      "test/unit")
    assertDefaultTestResources("project")

    val module = getModule("project")

    val webFacet = FacetManager.getInstance(module).findFacet(WebFacet.ID, "GrailsWeb")
    assertNotNull(webFacet)

    val appRoot = GrailsFramework.getInstance().findAppRoot(module)
    assertNotNull(appRoot)

    val shouldBeRoot: MutableList<VirtualFile?> = ArrayList(Arrays.asList(
      VfsUtil.findRelativeFile(appRoot, "web-app"),
      VfsUtil.findRelativeFile(appRoot, "grails-app", "views")
    ))

    for (webRoot in webFacet!!.getWebRoots()) {
      shouldBeRoot.remove(webRoot.file)
    }

    assertTrue("Following web folder was not added to web roots: " + shouldBeRoot.toString() + "; " + webFacet.getWebRoots(),
               shouldBeRoot.isEmpty())
  }
}
