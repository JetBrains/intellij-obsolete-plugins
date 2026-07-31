/*
 * Copyright (c) 2000-2007 JetBrains s.r.o. All Rights Reserved.
 */
package com.intellij.gwt.maven

import com.intellij.compiler.artifacts.ArtifactsTestUtil
import com.intellij.gwt.facet.GwtFacet
import com.intellij.gwt.facet.GwtFacetType
import com.intellij.gwt.module.GwtModulesManager
import com.intellij.gwt.module.model.GwtInheritsEntry
import com.intellij.gwt.module.model.GwtModule
import com.intellij.gwt.module.model.GwtRelativePath
import com.intellij.javaee.web.facet.WebFacetType
import com.intellij.maven.testFramework.fixtures.MavenVersionArguments
import com.intellij.maven.testFramework.fixtures.assertModules
import com.intellij.maven.testFramework.fixtures.assumeMaven3
import com.intellij.maven.testFramework.fixtures.awaitConfiguration
import com.intellij.maven.testFramework.fixtures.createProjectSubFile
import com.intellij.maven.testFramework.fixtures.getFacet
import com.intellij.maven.testFramework.fixtures.importProjectAsync
import com.intellij.maven.testFramework.fixtures.mavenImportingFixture
import com.intellij.maven.testFramework.fixtures.projectPath
import com.intellij.maven.testFramework.fixtures.waitForImportWithinTimeout
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.util.xml.GenericAttributeValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.idea.maven.model.MavenId
import org.jetbrains.jps.gwt.model.GwtJavaScriptOutputStyle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.util.stream.Collectors

@TestApplication
@ParameterizedClass
@ArgumentsSource(MavenVersionArguments::class)
class GwtLtgtFacetImporterTest(mavenVersion: String, modelVersion: String) {

  private val maven by mavenImportingFixture(mavenVersion = mavenVersion, modelVersion = modelVersion)

  private val pluginId: MavenId
    get() = MavenId("net.ltgt.gwt.maven", "gwt-maven-plugin", "1.1.0")

  private fun getFacet(module: String): GwtFacet? = maven.getFacet(module, GwtFacetType.getInstance())

  private fun pluginCoordinates(): String {
    val id = pluginId
    return "<groupId>" + id.groupId + "</groupId>" +
           "<artifactId>" + id.artifactId + "</artifactId>" +
           "<version>" + id.version + "</version>"
  }

  @Test
  fun testGwtFacet() = runBlocking {
    maven.assumeMaven3()
    maven.importProjectAsync("<groupId>test</groupId>" +
                       "<artifactId>project</artifactId>" +
                       "<version>1</version>" +

                       "<properties>" +
                       "  <google.webtoolkit.home>c:/tools/gwt</google.webtoolkit.home>" +
                       "</properties>" +

                       "<build>" +
                       "  <plugins>" +
                       "    <plugin>" +
                       pluginCoordinates() +
                       "    </plugin>" +
                       "  </plugins>" +
                       "</build>")
    maven.awaitConfiguration()

    maven.assertModules("project")

    val f = getFacet("project")
    assertEquals("file://c:/tools/gwt", f!!.configuration.gwtSdkUrl)
    assertNull(f.getWebFacet())
  }

  @Test
  fun testGwtFacetWithPathConfiguredInsidePlugin() = runBlocking {
    maven.assumeMaven3()
    maven.importProjectAsync("<groupId>test</groupId>" +
                       "<artifactId>project</artifactId>" +
                       "<version>1</version>" +

                       "<build>" +
                       "  <plugins>" +
                       "    <plugin>" +
                       pluginCoordinates() +
                       "      <configuration>" +
                       "        <gwtHome>c:/tools/gwt</gwtHome>" +
                       "      </configuration>" +
                       "    </plugin>" +
                       "  </plugins>" +
                       "</build>")
    maven.awaitConfiguration()

    maven.assertModules("project")

    val f = getFacet("project")
    assertEquals("file://c:/tools/gwt", f!!.configuration.gwtSdkUrl)
  }

  @Test
  fun testGwtFacetWithoutPath() = runBlocking {
    maven.assumeMaven3()
    maven.importProjectAsync("<groupId>test</groupId>" +
                       "<artifactId>project</artifactId>" +
                       "<version>1</version>" +

                       "<build>" +
                       "  <plugins>" +
                       "    <plugin>" +
                       pluginCoordinates() +
                       "    </plugin>" +
                       "  </plugins>" +
                       "</build>")
    maven.awaitConfiguration()

    maven.assertModules("project")

    val f = getFacet("project")
    assertEquals("", f!!.configuration.gwtSdkUrl)
  }

  @Test
  fun testDoesNotResetExistingGwtPathIfNoPathSpecifiedInPomXml() = runBlocking {
    maven.assumeMaven3()
    maven.importProjectAsync("<groupId>test</groupId>" +
                       "<artifactId>project</artifactId>" +
                       "<version>1</version>" +

                       "<build>" +
                       "  <plugins>" +
                       "    <plugin>" +
                       pluginCoordinates() +
                       "    </plugin>" +
                       "  </plugins>" +
                       "</build>")
    maven.awaitConfiguration()

    maven.assertModules("project")

    val f = getFacet("project")
    f!!.configuration.gwtSdkUrl = "file://c:/tools/gwt"
    assertEquals("file://c:/tools/gwt", f.configuration.gwtSdkUrl)

    maven.importProjectAsync()

    assertEquals("file://c:/tools/gwt", f.configuration.gwtSdkUrl)
  }

  //@Test
  @Suppress("unused")
  fun testSettingCorrespondingWebFacetForGwtFacet() = runBlocking {
    maven.assumeMaven3()
    maven.importProjectAsync("<groupId>test</groupId>" +
                       "<artifactId>project</artifactId>" +
                       "<version>1</version>" +
                       "<packaging>war</packaging>" +

                       "<build>" +
                       "  <plugins>" +
                       "    <plugin>" +
                       pluginCoordinates() +
                       "    </plugin>" +
                       "  </plugins>" +
                       "</build>")
    maven.awaitConfiguration()

    maven.assertModules("project")

    val gwt = getFacet("project")
    val web = maven.getFacet("project", WebFacetType.getInstance(), "Web")
    assertEquals(web, gwt!!.webFacet)

    val metaInfPath = FileUtil.toSystemIndependentName("${maven.projectPath}/target/project-1/META-INF")
    assertExplodedLayout("project:war", """<root>
 WEB-INF/
  classes/
   module:project
 META-INF/
  file:$metaInfPath/MANIFEST.MF
 javaee-resources:Web(project)
 gwt-compiler-output:GWT(project)""")
  }

  @Test
  fun testAddingExistingGwtModules() = runBlocking {
    maven.assumeMaven3()
    maven.createProjectSubFile("src/main/java/Test.gwt.xml", "<module></module>")

    maven.importProjectAsync("<groupId>test</groupId>" +
                       "<artifactId>project</artifactId>" +
                       "<version>1</version>" +

                       "<properties>" +
                       "  <google.webtoolkit.home>c:/tools/gwt</google.webtoolkit.home>" +
                       "</properties>" +

                       "<build>" +
                       "  <plugins>" +
                       "    <plugin>" +
                       pluginCoordinates() +
                       "    </plugin>" +
                       "  </plugins>" +
                       "</build>")
    maven.awaitConfiguration()

    maven.assertModules("project")

    smartReadAction(maven.project) {
      val mm = GwtModulesManager.getInstance(maven.project).getAllGwtModules()
      assertEquals(1, mm.size)
      assertEquals("Test", mm[0].getShortName())
    }
  }

  @Test
  fun testGwtCompilerOptions() = runBlocking {
    maven.assumeMaven3()
    maven.importProjectAsync("""${gwtProject("war")}<build>
    <plugins>
        <plugin>
${pluginCoordinates()}            <configuration>
                <compilerArgs>
                    <compilerArg>-XdisableCastChecking</compilerArg>
                    <compilerArg>-XdisableClassMetadata</compilerArg>
                </compilerArgs>
                <draftCompile>true</draftCompile>
                <logLevel>INFO</logLevel>
                <localWorkers>1C</localWorkers>
                <optimize>9</optimize>
                <sourceLevel>1.6</sourceLevel>
                <style>OBF</style>
                <failOnError>true</failOnError>
                <jvmArgs>
                  <jvmArg>-Xmx1g</jvmArg>
                  <jvmArg>-Xms128m</jvmArg>
                </jvmArgs>
                <systemProperties>
                  <my.key>my.value</my.key>
                </systemProperties>
            </configuration>
        </plugin>
    </plugins>
</build>""")

    maven.awaitConfiguration()

    val facet = getFacet("project")!!

    assertEquals(LanguageLevel.JDK_1_6, facet.configuration.getClientLanguageLevel())
    assertEquals(GwtJavaScriptOutputStyle.OBFUSCATED, facet.configuration.outputStyle)
    assertEquals(1024, facet.configuration.compilerMaxHeapSize)

    UsefulTestCase.assertSameElements(
      facet.configuration.compilerParameters.split(" (?=-)".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(),
      "-draftCompile",
      "-logLevel INFO",
      "-localWorkers 1C",
      "-optimize 9",
      "-sourceLevel 1.6",
      "-failOnError",
      "-XdisableCastChecking",
      "-XdisableClassMetadata")

    UsefulTestCase.assertSameElements(
      facet.configuration.additionalCompilerVMParameters.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(),
      "-Xms128m", "-Dmy.key=my.value")
  }

  @Test
  fun testImportEmptyModule() = runBlocking {
    maven.assumeMaven3()
    maven.createProjectSubFile("src/main/module.gwt.xml", "<module/>")
    maven.importProjectAsync("""${gwtProject("gwt-app")}<build>
    <plugins>
        <plugin>
${pluginCoordinates()}            <extensions>true</extensions>
            <configuration>
              <moduleName>ppp.MyModule</moduleName>
            </configuration>
        </plugin>
    </plugins>
</build>""")
    maven.awaitConfiguration()

    val moduleFile = UsefulTestCase.refreshAndFindFile(File(
      "${maven.projectPath}/target/generated-sources/gwt-maven-plugin/ppp/MyModule.gwt.xml"))
    assertNotNull(moduleFile)
    val modulePsiFile = readAction { PsiManager.getInstance(maven.project).findFile(moduleFile) }!!

    val gwtModule = readAction { GwtModulesManager.getInstance(maven.project).getGwtModuleByXmlFile(modulePsiFile) }
    assertGwtModule(gwtModule, "ppp.MyModule", "ppp.MyModule",
                    mutableListOf<String?>("client", "shared"), listOf("super"),
                    listOf("com.google.gwt.core.Core"))
  }

  @Test
  fun testImportConfiguredModule() = runBlocking {
    maven.assumeMaven3()
    maven.createProjectSubFile("module/template.gwt.xml",
                         """
                           <module rename-to='ignored'>
                            <source path='myClient'/>
                            <super-source path='mySuper'/>
                            <inherits name='qqq.Module'/>
                           </module>
                           """.trimIndent())
    maven.importProjectAsync("""${gwtProject("gwt-app")}<build>
    <sourceDirectory>sources</sourceDirectory>
    <plugins>
        <plugin>
${pluginCoordinates()}            <extensions>true</extensions>
            <configuration>
              <moduleName>ppp.MyModule</moduleName>
              <moduleShortName>my-module</moduleShortName>
              <moduleTemplate>module/template.gwt.xml</moduleTemplate>
            </configuration>
        </plugin>
    </plugins>
</build>""")

    maven.awaitConfiguration()

    val moduleFile = UsefulTestCase.refreshAndFindFile(File(
      "${maven.projectPath}/target/generated-sources/gwt-maven-plugin/ppp/MyModule.gwt.xml"))
    assertNotNull(moduleFile)
    val psiManager = PsiManager.getInstance(maven.project)
    val modulePsiFile = readAction { psiManager.findFile(moduleFile) }!!

    val gwtModulesManager = GwtModulesManager.getInstance(maven.project)
    val gwtModule = readAction { gwtModulesManager.getGwtModuleByXmlFile(modulePsiFile) }
    assertGwtModule(gwtModule, "ppp.MyModule", "my-module",
                    listOf("myClient"), listOf("mySuper"),
                    listOf("qqq.Module"))

    val templateFile = UsefulTestCase.refreshAndFindFile(File("${maven.projectPath}/module/template.gwt.xml"))
    val templatePsiFile = readAction { psiManager.findFile(templateFile) }
    maven.waitForImportWithinTimeout {
      withContext(Dispatchers.EDT) {
        WriteCommandAction.runWriteCommandAction(maven.project) { (templatePsiFile as XmlFile?)!!.getRootTag()!!.delete() }
      }
    }

    val gwtModule1 = readAction { gwtModulesManager.getGwtModule(modulePsiFile) }
    assertGwtModule(gwtModule1, "ppp.MyModule", "my-module",
                    mutableListOf<String?>("client", "shared"), listOf("super"),
                    listOf("com.google.gwt.core.Core"))
  }

  private fun assertExplodedLayout(artifactName: String, expected: String?) {
    assertJarLayout("$artifactName exploded", expected)
  }

  private fun assertJarLayout(artifactName: String?, expected: String?) {
    ArtifactsTestUtil.assertLayout(maven.project, artifactName, expected)
  }

  companion object {
    private fun gwtProject(packaging: String): String {
      return """<groupId>test</groupId>
<artifactId>project</artifactId>
<packaging>$packaging</packaging>
<version>1</version>
<dependencies>
    <dependency>
        <groupId>com.google.gwt</groupId>
        <artifactId>gwt-user</artifactId>
        <version>2.7.0</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>com.google.gwt</groupId>
        <artifactId>gwt-dev</artifactId>
        <version>2.7.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
"""
    }

    private suspend fun assertGwtModule(gwtModule: GwtModule?, qualifiedName: String, outputName: String,
                                        sources: List<String?>, superSources: List<String?>, inherits: List<String?>) = readAction {
      assertNotNull(gwtModule)

      assertEquals(qualifiedName, gwtModule!!.getQualifiedName())
      assertEquals(outputName, gwtModule.getOutputName())

      UsefulTestCase.assertSameElements(gwtModule.getSources().stream()
                                          .map { obj: GwtRelativePath -> obj.getPath() }.map { obj: GenericAttributeValue<String> -> obj.getStringValue() }.collect(
          Collectors.toList()), sources)

      UsefulTestCase.assertSameElements(gwtModule.getSuperSources().stream()
                                          .map { obj: GwtRelativePath -> obj.getPath() }.map { obj: GenericAttributeValue<String> -> obj.getStringValue() }.collect(
          Collectors.toList()), superSources)

      UsefulTestCase.assertSameElements(gwtModule.getInheritses().stream()
                                          .map { obj: GwtInheritsEntry -> obj.getName() }.map { obj: GenericAttributeValue<String> -> obj.getStringValue() }.collect(
          Collectors.toList()), inherits)
    }
  }
}
